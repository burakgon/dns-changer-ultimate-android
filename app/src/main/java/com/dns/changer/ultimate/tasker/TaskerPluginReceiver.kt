package com.dns.changer.ultimate.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.service.DnsConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Tasker Plugin Fire Receiver
 *
 * This receiver implements the Locale Plugin API to handle actions
 * triggered by Tasker when a configured plugin action is executed.
 *
 * Intent Filter: com.twofortyfouram.locale.intent.action.FIRE_SETTING
 */
@AndroidEntryPoint
class TaskerPluginReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dnsConnectionManager: DnsConnectionManager

    @Inject
    lateinit var preferences: DnsPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "TaskerPluginReceiver"
        const val ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
        const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_SETTING) {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }

        val bundle = intent.getBundleExtra(EXTRA_BUNDLE)
        if (bundle == null) {
            Log.e(TAG, "No bundle provided")
            return
        }

        val action = bundle.getString(TaskerPluginActivity.BUNDLE_ACTION)
        Log.d(TAG, "Received Tasker action: $action")

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

                when (action) {
                    TaskerPluginActivity.ACTION_CONNECT -> handleConnect(bundle)
                    TaskerPluginActivity.ACTION_DISCONNECT -> handleDisconnect()
                    TaskerPluginActivity.ACTION_SWITCH -> handleSwitch(bundle)
                    else -> Log.w(TAG, "Unknown action type: $action")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleConnect(bundle: Bundle) {
        try {
            // Check VPN permission
            val vpnPermissionIntent = dnsConnectionManager.checkVpnPermission()
            if (vpnPermissionIntent != null) {
                Log.e(TAG, "VPN permission not granted. Cannot connect via Tasker without permission.")
                return
            }

            val server = parseServerFromBundle(bundle)
            if (server != null) {
                Log.d(TAG, "Connecting to DNS: ${server.name}")
                dnsConnectionManager.connect(server)
            } else {
                Log.e(TAG, "Failed to parse DNS server from bundle")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to DNS", e)
        }
    }

    private suspend fun handleDisconnect() {
        try {
            Log.d(TAG, "Disconnecting from DNS")
            dnsConnectionManager.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from DNS", e)
        }
    }

    private suspend fun handleSwitch(bundle: Bundle) {
        try {
            val currentState = dnsConnectionManager.connectionState.first()
            if (currentState !is ConnectionState.Connected) {
                Log.w(TAG, "Cannot switch - not currently connected, connecting instead")
                handleConnect(bundle)
                return
            }

            val server = parseServerFromBundle(bundle)
            if (server != null) {
                Log.d(TAG, "Switching to DNS: ${server.name}")
                dnsConnectionManager.switchServer(server)
            } else {
                Log.e(TAG, "Failed to parse DNS server from bundle")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error switching DNS server", e)
        }
    }

    private fun parseServerFromBundle(bundle: Bundle): DnsServer? {
        // Option 1: Use preset server by ID
        val serverId = bundle.getString(TaskerPluginActivity.BUNDLE_SERVER_ID)
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
        val primaryDns = bundle.getString(TaskerPluginActivity.BUNDLE_PRIMARY_DNS)
        if (!primaryDns.isNullOrBlank()) {
            val secondaryDns = bundle.getString(TaskerPluginActivity.BUNDLE_SECONDARY_DNS) ?: primaryDns
            val serverName = bundle.getString(TaskerPluginActivity.BUNDLE_SERVER_NAME) ?: "Tasker DNS"
            val isDoH = bundle.getBoolean(TaskerPluginActivity.BUNDLE_IS_DOH, false)
            val dohUrl = bundle.getString(TaskerPluginActivity.BUNDLE_DOH_URL)

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

        Log.e(TAG, "No valid server information provided in bundle")
        return null
    }
}
