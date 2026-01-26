package com.dns.changer.ultimate.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dns.changer.ultimate.ads.AnalyticsEvents
import com.dns.changer.ultimate.ads.AnalyticsManager
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferences: DnsPreferences

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Boot completed received")

        // Use goAsync() to allow coroutine to complete
        val pendingResult = goAsync()

        scope.launch {
            try {
                // Check if start on boot is enabled and user is premium
                val startOnBoot = preferences.startOnBoot.first()
                val isPremium = preferences.isPremium.first()

                Log.d("BootReceiver", "startOnBoot=$startOnBoot, isPremium=$isPremium")

                if (!startOnBoot || !isPremium) {
                    Log.d("BootReceiver", "Start on boot disabled or not premium, skipping")
                    pendingResult.finish()
                    return@launch
                }

                // Get the selected server
                val selectedId = preferences.selectedDnsId.first()
                if (selectedId == null) {
                    Log.d("BootReceiver", "No server selected, skipping")
                    pendingResult.finish()
                    return@launch
                }

                // Find the server (check presets first, then custom)
                val server = PresetDnsServers.getById(selectedId)
                    ?: getCustomServerById(selectedId)

                if (server == null) {
                    Log.d("BootReceiver", "Server not found: $selectedId")
                    pendingResult.finish()
                    return@launch
                }

                Log.d("BootReceiver", "Starting VPN with server: ${server.name}")
                analyticsManager.logEvent(AnalyticsEvents.BOOT_AUTO_CONNECT)

                // Start the VPN service
                val serviceIntent = Intent(context, DnsVpnService::class.java).apply {
                    action = DnsVpnService.ACTION_START
                    putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, server.primaryDns)
                    putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, server.secondaryDns)
                    putExtra(DnsVpnService.EXTRA_SERVER_NAME, server.name)
                    putExtra(DnsVpnService.EXTRA_IS_DOH, server.isDoH)
                    if (server.isDoH && !server.dohUrl.isNullOrBlank()) {
                        putExtra(DnsVpnService.EXTRA_DOH_URL, server.dohUrl)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Update connected state
                preferences.setConnected(true)

                Log.d("BootReceiver", "VPN service started successfully")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting VPN on boot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun getCustomServerById(id: String): com.dns.changer.ultimate.data.model.DnsServer? {
        val json = preferences.customDnsList.first() ?: return null
        return try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getString("id") == id) {
                    return com.dns.changer.ultimate.data.model.DnsServer(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        primaryDns = obj.getString("primaryDns"),
                        secondaryDns = obj.getString("secondaryDns"),
                        category = com.dns.changer.ultimate.data.model.DnsCategory.CUSTOM,
                        description = obj.optString("description", "Custom DNS server"),
                        isCustom = true,
                        isDoH = obj.optBoolean("isDoH", false),
                        dohUrl = if (obj.has("dohUrl")) obj.getString("dohUrl").takeIf { it.isNotBlank() } else null
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error parsing custom servers: ${e.message}")
            null
        }
    }
}
