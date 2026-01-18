package com.dns.changer.ultimate.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.repository.DnsRepository
import com.dns.changer.ultimate.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dnsRepository: DnsRepository
) {
    // Structured concurrency: Use SupervisorJob so child failures don't cancel siblings
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentServer: DnsServer? = null

    fun checkVpnPermission(): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Check if any VPN is currently active on the system
     */
    private fun isVpnActive(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Check if internet connection is available (WiFi, Cellular, or Ethernet)
     * Returns true if device has an active internet connection
     */
    fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
               (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    /**
     * Check actual VPN state and sync our state with it
     * This checks the real system VPN state, not internal variables
     * Returns true if VPN is active, false otherwise
     */
    suspend fun syncWithVpnState(): Boolean {
        val vpnActive = isVpnActive()
        val currentState = _connectionState.value
        android.util.Log.d("DnsConnectionManager", "Syncing state: systemVpnActive=$vpnActive, currentState=$currentState")

        when {
            !vpnActive -> {
                // VPN is definitely not running - set Disconnected and update DataStore
                _connectionState.value = ConnectionState.Disconnected
                currentServer = null
                dnsRepository.setConnected(false)  // Update DataStore to prevent restoreState from restoring
                android.util.Log.d("DnsConnectionManager", "Synced to Disconnected (no VPN active)")
            }
            vpnActive && currentState is ConnectionState.Switching -> {
                // VPN is active but we're stuck in Switching - complete the switch
                val server = currentState.toServer
                _connectionState.value = ConnectionState.Connected(server)
                currentServer = server
                dnsRepository.setConnected(true)
                android.util.Log.d("DnsConnectionManager", "Synced Switching -> Connected: ${server.name}")
            }
            vpnActive && currentState is ConnectionState.Connecting -> {
                // VPN is active but we're stuck in Connecting - shouldn't happen often
                // Use currentServer if available
                currentServer?.let { server ->
                    _connectionState.value = ConnectionState.Connected(server)
                    dnsRepository.setConnected(true)
                    android.util.Log.d("DnsConnectionManager", "Synced Connecting -> Connected: ${server.name}")
                }
            }
            vpnActive && currentState is ConnectionState.Disconnecting -> {
                // VPN is still active but we thought we were disconnecting
                // This means disconnect didn't complete - set to Disconnected and try to stop
                stopVpnService()
                _connectionState.value = ConnectionState.Disconnected
                currentServer = null
                dnsRepository.setConnected(false)
                android.util.Log.d("DnsConnectionManager", "Synced Disconnecting -> Disconnected (forcing VPN stop)")
            }
            vpnActive && currentState is ConnectionState.Error -> {
                // VPN is active but we have an error state - clear to Disconnected
                _connectionState.value = ConnectionState.Disconnected
                currentServer = null
                dnsRepository.setConnected(false)
                android.util.Log.d("DnsConnectionManager", "Synced Error -> Disconnected")
            }
        }
        return vpnActive
    }

    /**
     * Reset state to Disconnected - used to recover from stuck states
     */
    fun resetState() {
        android.util.Log.d("DnsConnectionManager", "Resetting state to Disconnected")
        _connectionState.value = ConnectionState.Disconnected
        currentServer = null
    }

    /**
     * Set visual Connecting state without actually starting VPN
     * Used to show "Connecting..." while ad is loading
     */
    fun setConnectingState() {
        android.util.Log.d("DnsConnectionManager", "Setting visual Connecting state")
        _connectionState.value = ConnectionState.Connecting
        WidgetUpdater.update(context)
    }

    /**
     * Set visual Disconnecting state without actually stopping VPN
     * Used to show "Disconnecting..." while ad is loading
     */
    fun setDisconnectingState() {
        android.util.Log.d("DnsConnectionManager", "Setting visual Disconnecting state")
        _connectionState.value = ConnectionState.Disconnecting
        WidgetUpdater.update(context)
    }

    /**
     * Force stop VPN and reset state - emergency recovery
     */
    fun forceReset() {
        android.util.Log.d("DnsConnectionManager", "Force reset - stopping VPN and resetting state")
        try {
            stopVpnService()
        } catch (e: Exception) {
            android.util.Log.e("DnsConnectionManager", "Error stopping VPN during force reset", e)
        }
        _connectionState.value = ConnectionState.Disconnected
        currentServer = null
    }

    fun connect(server: DnsServer) {
        _connectionState.value = ConnectionState.Connecting
        currentServer = server
        WidgetUpdater.update(context)

        try {
            connectViaVpn(server)
            // Update state immediately - VPN service will handle actual connection
            _connectionState.value = ConnectionState.Connected(server)
            // Fire-and-forget DataStore updates using structured concurrency
            scope.launch {
                dnsRepository.selectServer(server)
                dnsRepository.setConnected(true)
            }
            WidgetUpdater.update(context)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
            WidgetUpdater.update(context)
        }
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.Disconnecting
        WidgetUpdater.update(context)

        try {
            stopVpnService()
            // Update state immediately - don't wait for DataStore
            _connectionState.value = ConnectionState.Disconnected
            currentServer = null
            // Fire-and-forget DataStore update using structured concurrency
            scope.launch {
                dnsRepository.setConnected(false)
            }
            WidgetUpdater.update(context)
        } catch (e: Exception) {
            // Even on error, try to stop and reset
            stopVpnService()
            _connectionState.value = ConnectionState.Disconnected
            currentServer = null
            WidgetUpdater.update(context)
        }
    }

    fun switchServer(newServer: DnsServer) {
        _connectionState.value = ConnectionState.Switching(newServer)
        WidgetUpdater.update(context)

        try {
            // Just send new config to running service - no stop/start needed
            currentServer = newServer
            connectViaVpn(newServer)
            // Update state immediately
            _connectionState.value = ConnectionState.Connected(newServer)
            // Fire-and-forget DataStore updates using structured concurrency
            scope.launch {
                dnsRepository.selectServer(newServer)
                dnsRepository.setConnected(true)
            }
            WidgetUpdater.update(context)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e.message ?: "Switch failed")
            WidgetUpdater.update(context)
        }
    }

    private fun connectViaVpn(server: DnsServer) {
        val intent = Intent(context, DnsVpnService::class.java).apply {
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
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopVpnService() {
        val intent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun openPrivateDnsSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun restoreConnection(server: DnsServer) {
        currentServer = server
        _connectionState.value = ConnectionState.Connected(server)
    }
}
