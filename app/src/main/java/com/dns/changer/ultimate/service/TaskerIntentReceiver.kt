package com.dns.changer.ultimate.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * TaskerIntentReceiver - Enables Tasker and other automation apps to control DNS connections
 *
 * Supported Actions:
 *
 * 1. Connect to DNS:
 *    - Action: com.dns.changer.ultimate.TASKER_CONNECT
 *    - Extras:
 *      - "server_id" (String, optional): ID of preset server (e.g., "cloudflare", "google")
 *      - "primary_dns" (String, optional): Primary DNS IP address (e.g., "1.1.1.1")
 *      - "secondary_dns" (String, optional): Secondary DNS IP address (e.g., "1.0.0.1")
 *      - "server_name" (String, optional): Display name for custom DNS (default: "Tasker DNS")
 *      - "is_doh" (Boolean, optional): Enable DNS-over-HTTPS (default: false, requires premium)
 *      - "doh_url" (String, optional): DoH URL (required if is_doh is true)
 *
 * 2. Disconnect from DNS:
 *    - Action: com.dns.changer.ultimate.TASKER_DISCONNECT
 *    - No extras required
 *
 * 3. Switch DNS server:
 *    - Action: com.dns.changer.ultimate.TASKER_SWITCH
 *    - Extras: Same as TASKER_CONNECT
 *
 * 4. Query connection status:
 *    - Action: com.dns.changer.ultimate.TASKER_QUERY_STATUS
 *    - Returns broadcast with action: com.dns.changer.ultimate.TASKER_STATUS_RESULT
 *    - Result extras:
 *      - "is_connected" (Boolean): Whether DNS VPN is currently connected
 *      - "server_name" (String): Name of connected DNS server (empty if disconnected)
 *      - "state" (String): Current state (connected, disconnected, connecting, disconnecting, switching, error)
 *
 * Examples (using adb for testing):
 *
 * Connect to Cloudflare:
 *   adb shell am broadcast -a com.dns.changer.ultimate.TASKER_CONNECT --es server_id cloudflare
 *
 * Connect to custom DNS:
 *   adb shell am broadcast -a com.dns.changer.ultimate.TASKER_CONNECT --es primary_dns 8.8.8.8 --es secondary_dns 8.8.4.4 --es server_name "My DNS"
 *
 * Disconnect:
 *   adb shell am broadcast -a com.dns.changer.ultimate.TASKER_DISCONNECT
 *
 * Query status:
 *   adb shell am broadcast -a com.dns.changer.ultimate.TASKER_QUERY_STATUS
 */
@AndroidEntryPoint
class TaskerIntentReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dnsConnectionManager: DnsConnectionManager

    @Inject
    lateinit var preferences: DnsPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "TaskerIntentReceiver"

        // Intent actions
        const val ACTION_CONNECT = "com.dns.changer.ultimate.TASKER_CONNECT"
        const val ACTION_DISCONNECT = "com.dns.changer.ultimate.TASKER_DISCONNECT"
        const val ACTION_SWITCH = "com.dns.changer.ultimate.TASKER_SWITCH"
        const val ACTION_QUERY_STATUS = "com.dns.changer.ultimate.TASKER_QUERY_STATUS"
        const val ACTION_STATUS_RESULT = "com.dns.changer.ultimate.TASKER_STATUS_RESULT"

        // Intent extras
        const val EXTRA_SERVER_ID = "server_id"
        const val EXTRA_PRIMARY_DNS = "primary_dns"
        const val EXTRA_SECONDARY_DNS = "secondary_dns"
        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_IS_DOH = "is_doh"
        const val EXTRA_DOH_URL = "doh_url"

        // Result extras
        const val EXTRA_IS_CONNECTED = "is_connected"
        const val EXTRA_STATE = "state"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")

        // Use goAsync() to allow coroutine to complete
        val pendingResult = goAsync()

        scope.launch {
            try {
                // Check premium status - Tasker integration is a premium feature
                val isPremium = preferences.isPremium.first()
                if (!isPremium) {
                    Log.w(TAG, "Tasker integration requires premium subscription")
                    pendingResult.finish()
                    return@launch
                }

                when (intent.action) {
                    ACTION_CONNECT -> handleConnectInternal(context, intent)
                    ACTION_DISCONNECT -> handleDisconnectInternal(context)
                    ACTION_SWITCH -> handleSwitchInternal(context, intent)
                    ACTION_QUERY_STATUS -> handleQueryStatusInternal(context)
                    else -> Log.w(TAG, "Unknown action: ${intent.action}")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleConnectInternal(context: Context, intent: Intent) {
        try {
            // Check VPN permission first
            val vpnPermissionIntent = dnsConnectionManager.checkVpnPermission()
            if (vpnPermissionIntent != null) {
                Log.e(TAG, "VPN permission not granted. Cannot connect via Tasker without permission.")
                return
            }

            val server = parseServerFromIntent(intent)
            if (server != null) {
                Log.d(TAG, "Connecting to DNS: ${server.name}")
                dnsConnectionManager.connect(server)
            } else {
                Log.e(TAG, "Failed to parse DNS server from intent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to DNS", e)
        }
    }

    private suspend fun handleDisconnectInternal(context: Context) {
        try {
            Log.d(TAG, "Disconnecting from DNS")
            dnsConnectionManager.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from DNS", e)
        }
    }

    private suspend fun handleSwitchInternal(context: Context, intent: Intent) {
        try {
            val currentState = dnsConnectionManager.connectionState.first()
            if (currentState !is ConnectionState.Connected) {
                Log.w(TAG, "Cannot switch - not currently connected")
                // Instead of switching, just connect
                handleConnectInternal(context, intent)
                return
            }

            val server = parseServerFromIntent(intent)
            if (server != null) {
                Log.d(TAG, "Switching to DNS: ${server.name}")
                dnsConnectionManager.switchServer(server)
            } else {
                Log.e(TAG, "Failed to parse DNS server from intent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error switching DNS server", e)
        }
    }

    private suspend fun handleQueryStatusInternal(context: Context) {
        try {
            val state = dnsConnectionManager.connectionState.first()
            val isConnected = state is ConnectionState.Connected
            val serverName = when (state) {
                is ConnectionState.Connected -> state.server.name
                is ConnectionState.Switching -> state.toServer.name
                else -> ""
            }
            val stateName = when (state) {
                is ConnectionState.Connected -> "connected"
                is ConnectionState.Disconnected -> "disconnected"
                is ConnectionState.Connecting -> "connecting"
                is ConnectionState.Disconnecting -> "disconnecting"
                is ConnectionState.Switching -> "switching"
                is ConnectionState.Error -> "error"
            }

            Log.d(TAG, "Query status: isConnected=$isConnected, serverName=$serverName, state=$stateName")

            // Send broadcast with result
            val resultIntent = Intent(ACTION_STATUS_RESULT).apply {
                putExtra(EXTRA_IS_CONNECTED, isConnected)
                putExtra(EXTRA_SERVER_NAME, serverName)
                putExtra(EXTRA_STATE, stateName)
            }
            context.sendBroadcast(resultIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying status", e)
        }
    }

    /**
     * Parse DNS server from intent extras
     * Priority:
     * 1. If server_id is provided, use preset server
     * 2. If primary_dns is provided, create custom server
     * 3. Otherwise, return null
     */
    private fun parseServerFromIntent(intent: Intent): DnsServer? {
        // Option 1: Use preset server by ID
        val serverId = intent.getStringExtra(EXTRA_SERVER_ID)
        if (!serverId.isNullOrBlank()) {
            val server = PresetDnsServers.getById(serverId)
            if (server != null) {
                Log.d(TAG, "Using preset server: $serverId")
                return server
            } else {
                Log.w(TAG, "Preset server not found: $serverId")
            }
        }

        // Option 2: Create custom server from DNS addresses
        val primaryDns = intent.getStringExtra(EXTRA_PRIMARY_DNS)
        if (!primaryDns.isNullOrBlank()) {
            val secondaryDns = intent.getStringExtra(EXTRA_SECONDARY_DNS) ?: primaryDns
            val serverName = intent.getStringExtra(EXTRA_SERVER_NAME) ?: "Tasker DNS"
            val isDoH = intent.getBooleanExtra(EXTRA_IS_DOH, false)
            val dohUrl = intent.getStringExtra(EXTRA_DOH_URL)

            // Validate DoH configuration
            if (isDoH && dohUrl.isNullOrBlank()) {
                Log.e(TAG, "DoH enabled but no DoH URL provided")
                return null
            }

            Log.d(TAG, "Creating custom server: $serverName (DoH: $isDoH)")
            return DnsServer(
                id = "tasker_${UUID.randomUUID()}",
                name = serverName,
                primaryDns = primaryDns,
                secondaryDns = secondaryDns,
                category = DnsCategory.CUSTOM,
                isCustom = true,
                isDoH = isDoH,
                dohUrl = dohUrl
            )
        }

        Log.e(TAG, "No valid server information provided in intent")
        return null
    }
}
