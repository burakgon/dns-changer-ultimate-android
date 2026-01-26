package com.dns.changer.ultimate.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.ads.AnalyticsEvents
import com.dns.changer.ultimate.ads.AnalyticsManager
import com.dns.changer.ultimate.ads.AnalyticsParams
import com.dns.changer.ultimate.ads.AnalyticsUserProps
import com.dns.changer.ultimate.data.repository.DnsRepository
import com.dns.changer.ultimate.service.DnsConnectionManager
import com.dns.changer.ultimate.service.DnsSpeedTestService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PendingAction {
    NONE, CONNECT, DISCONNECT
}

data class MainUiState(
    val servers: Map<DnsCategory, List<DnsServer>> = emptyMap(),
    val selectedServer: DnsServer? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val showVpnPermissionDialog: Boolean = false,
    val showCustomDnsDialog: Boolean = false,
    val vpnPermissionIntent: Intent? = null,
    val showConnectionSuccessOverlay: Boolean = false,
    val showDisconnectionOverlay: Boolean = false,
    val lastConnectedServer: DnsServer? = null,
    // Track server switch state atomically with UI state
    val pendingSwitchToServer: DnsServer? = null,
    // Track pending action while loading interstitial ad
    val pendingAction: PendingAction = PendingAction.NONE,
    val isPreparingAction: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dnsRepository: DnsRepository,
    private val connectionManager: DnsConnectionManager,
    private val speedTestService: DnsSpeedTestService,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Expose connection state directly without stateIn to avoid duplicate collections
    val connectionState: StateFlow<ConnectionState> get() = connectionManager.connectionState

    // Track previous state for transition detection
    private var previousConnectionState: ConnectionState = ConnectionState.Disconnected


    init {
        loadServers()
        setDefaultServerIfNeeded()
        viewModelScope.launch {
            syncVpnState()
        }
        observeConnectionState()
        observeSelectedServer()
    }

    /**
     * Set Cloudflare as default DNS server if no server is selected (first launch)
     * Uses first() instead of collect to avoid endless collection
     */
    private fun setDefaultServerIfNeeded() {
        viewModelScope.launch {
            val id = dnsRepository.selectedDnsId.first()
            if (id == null && _uiState.value.selectedServer == null) {
                // First launch - set Cloudflare as default
                val defaultServer = PresetDnsServers.all.first() // Cloudflare
                _uiState.update { it.copy(selectedServer = defaultServer) }
                dnsRepository.selectServer(defaultServer)
                android.util.Log.d("MainViewModel", "Set default server: ${defaultServer.name}")
            }
        }
    }

    /**
     * Observe selected server changes from repository (syncs with SpeedTest screen)
     */
    private fun observeSelectedServer() {
        viewModelScope.launch {
            dnsRepository.selectedServer.collect { server ->
                if (server != null && server.id != _uiState.value.selectedServer?.id) {
                    android.util.Log.d("MainViewModel", "Server selection synced from repository: ${server.name}")
                    _uiState.update { it.copy(selectedServer = server) }
                }
            }
        }
    }

    /**
     * Sync our state with the actual VPN service state
     */
    private suspend fun syncVpnState() {
        android.util.Log.d("MainViewModel", "Syncing with VPN state on startup")
        connectionManager.syncWithVpnState()
    }

    private fun loadServers() {
        // Initial load with preset servers
        val serversByCategory = PresetDnsServers.all.groupBy { it.category }
        _uiState.value = _uiState.value.copy(servers = serversByCategory)

        // Observe custom servers and update the list when they change
        viewModelScope.launch {
            dnsRepository.allServers.collect { allServers ->
                val grouped = allServers.groupBy { it.category }
                _uiState.update { it.copy(servers = grouped) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            // Collect from the shared stateIn flow to avoid duplicate upstream collectors
            connectionState.collect { state ->
                val wasConnecting = previousConnectionState is ConnectionState.Connecting
                val wasConnected = previousConnectionState is ConnectionState.Connected
                val wasDisconnecting = previousConnectionState is ConnectionState.Disconnecting
                val wasSwitching = previousConnectionState is ConnectionState.Switching
                val isNowConnected = state is ConnectionState.Connected
                val isNowDisconnected = state is ConnectionState.Disconnected

                // Fire analytics and side effects OUTSIDE _uiState.update {} to avoid
                // duplicate calls when the update lambda is retried due to concurrent modifications.
                // MutableStateFlow.update {} uses compare-and-set and may re-execute the lambda.
                if (state is ConnectionState.Error) {
                    android.util.Log.e("MainViewModel", "Error state: ${state.message}")
                    analyticsManager.logEvent(
                        AnalyticsEvents.DNS_CONNECTION_ERROR,
                        mapOf(AnalyticsParams.ERROR_MESSAGE to (state.message ?: "unknown"))
                    )
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2000)
                        connectionManager.resetState()
                    }
                }

                // Only fire analytics on genuine state transitions, not on initial
                // StateFlow value delivery or re-emissions
                if (isNowConnected && (wasConnecting || wasSwitching)) {
                    val connectedServer = (state as ConnectionState.Connected).server
                    analyticsManager.logEvent(
                        AnalyticsEvents.DNS_CONNECTED,
                        mapOf(
                            AnalyticsParams.SERVER_NAME to connectedServer.name,
                            AnalyticsParams.DOH_ENABLED to connectedServer.isDoH
                        )
                    )
                    analyticsManager.setUserProperty(AnalyticsUserProps.DOH_ENABLED, connectedServer.isDoH.toString())
                }

                if (isNowDisconnected && (wasDisconnecting || wasConnected)) {
                    analyticsManager.logEvent(AnalyticsEvents.DNS_DISCONNECTED)
                }

                // Use atomic update for UI state only - no side effects inside
                _uiState.update { currentUiState ->
                    val switchTarget = currentUiState.pendingSwitchToServer
                    val isSwitchingServers = switchTarget != null

                    android.util.Log.d("MainViewModel", "State: $previousConnectionState -> $state, pendingSwitch=${switchTarget?.name}")

                    when {
                        isNowConnected -> {
                            val connectedServer = (state as ConnectionState.Connected).server
                            val showSuccessOverlay = wasConnecting || wasSwitching
                            val clearSwitch = switchTarget?.id == connectedServer.id || wasSwitching
                            android.util.Log.d("MainViewModel", "Connected! showSuccessOverlay=$showSuccessOverlay (wasConnecting=$wasConnecting, wasSwitching=$wasSwitching)")
                            if (clearSwitch) {
                                android.util.Log.d("MainViewModel", "Switch complete to ${connectedServer.name}")
                            }
                            currentUiState.copy(
                                connectionState = state,
                                lastConnectedServer = connectedServer,
                                showConnectionSuccessOverlay = showSuccessOverlay,
                                pendingSwitchToServer = null
                            )
                        }
                        isNowDisconnected && (wasDisconnecting || wasConnected) -> {
                            if (!isSwitchingServers) {
                                android.util.Log.d("MainViewModel", "Disconnected! showDisconnectionOverlay=true")
                                currentUiState.copy(
                                    connectionState = state,
                                    showDisconnectionOverlay = true
                                )
                            } else {
                                android.util.Log.d("MainViewModel", "Suppressing disconnect overlay (switching to ${switchTarget?.name})")
                                currentUiState.copy(connectionState = state)
                            }
                        }
                        else -> {
                            currentUiState.copy(connectionState = state)
                        }
                    }
                }

                previousConnectionState = state
            }
        }
    }

    private fun restoreState() {
        // Collect selected DNS ID in its own coroutine
        viewModelScope.launch {
            dnsRepository.selectedDnsId.collect { id ->
                val server = id?.let { PresetDnsServers.getById(it) }
                _uiState.value = _uiState.value.copy(selectedServer = server)
            }
        }

        // Collect connection state separately to restore connection if needed
        viewModelScope.launch {
            // Combine both flows to check restoration condition
            kotlinx.coroutines.flow.combine(
                dnsRepository.selectedDnsId,
                dnsRepository.isConnected
            ) { id, isConnected ->
                Pair(id, isConnected)
            }.collect { (id, isConnected) ->
                if (isConnected && id != null && connectionState.value is ConnectionState.Disconnected) {
                    val server = PresetDnsServers.getById(id)
                    if (server != null) {
                        connectionManager.restoreConnection(server)
                    }
                }
            }
        }
    }

    /**
     * Select a server. Does NOT auto-reconnect — the caller must trigger
     * the connect flow (with ads) when the user is already connected.
     * Returns true if the user was connected (caller should reconnect).
     */
    fun selectServer(server: DnsServer): Boolean {
        val wasConnected = connectionState.value is ConnectionState.Connected
        android.util.Log.d("MainViewModel", "selectServer: ${server.name}, wasConnected=$wasConnected")

        analyticsManager.logEvent(
            AnalyticsEvents.DNS_SERVER_SWITCH,
            mapOf(AnalyticsParams.SERVER_NAME to server.name)
        )
        analyticsManager.setUserProperty(AnalyticsUserProps.SELECTED_SERVER, server.name)

        // Set pending switch target AND selected server atomically in UI state
        if (wasConnected) {
            android.util.Log.d("MainViewModel", "Server selected while connected: ${server.name}")
            _uiState.update { it.copy(
                selectedServer = server,
                pendingSwitchToServer = server
            ) }
        } else {
            _uiState.update { it.copy(selectedServer = server) }
        }

        // Save selection in background
        viewModelScope.launch {
            dnsRepository.selectServer(server)
        }

        return wasConnected
    }

    fun prepareConnect() {
        // Use selected server or fall back to default (Cloudflare)
        val server = _uiState.value.selectedServer ?: PresetDnsServers.all.first().also {
            _uiState.update { state -> state.copy(selectedServer = it) }
        }

        // Check VPN permission first
        val vpnIntent = connectionManager.checkVpnPermission()
        if (vpnIntent != null) {
            _uiState.value = _uiState.value.copy(
                showVpnPermissionDialog = true,
                vpnPermissionIntent = vpnIntent
            )
            return
        }

        // Set preparing state - this will trigger ad loading in UI
        _uiState.update { it.copy(
            pendingAction = PendingAction.CONNECT,
            isPreparingAction = true
        ) }
    }

    fun prepareDisconnect() {
        // Set preparing state - this will trigger ad loading in UI
        _uiState.update { it.copy(
            pendingAction = PendingAction.DISCONNECT,
            isPreparingAction = true
        ) }
    }

    fun executeConnect() {
        val server = _uiState.value.selectedServer ?: PresetDnsServers.all.first()
        _uiState.update { it.copy(
            pendingAction = PendingAction.NONE,
            isPreparingAction = false
        ) }
        connectionManager.connect(server)
    }

    fun executeDisconnect() {
        _uiState.update { it.copy(
            pendingAction = PendingAction.NONE,
            isPreparingAction = false
        ) }
        connectionManager.disconnect()
    }

    fun cancelPendingAction() {
        _uiState.update { it.copy(
            pendingAction = PendingAction.NONE,
            isPreparingAction = false
        ) }
    }

    fun connect() {
        // Direct connect without ad (used for permission callback)
        val server = _uiState.value.selectedServer ?: PresetDnsServers.all.first().also {
            _uiState.update { state -> state.copy(selectedServer = it) }
        }

        // Check VPN permission
        val vpnIntent = connectionManager.checkVpnPermission()
        if (vpnIntent != null) {
            _uiState.value = _uiState.value.copy(
                showVpnPermissionDialog = true,
                vpnPermissionIntent = vpnIntent
            )
            return
        }

        connectionManager.connect(server)
    }

    fun disconnect() {
        connectionManager.disconnect()
    }

    fun checkVpnPermission(): android.content.Intent? {
        return connectionManager.checkVpnPermission()
    }

    /**
     * Check if internet connection is available
     */
    fun isInternetAvailable(): Boolean {
        return connectionManager.isInternetAvailable()
    }

    /**
     * Set visual Connecting state without actually connecting
     * Used to show "Connecting..." while ad is loading
     */
    fun setConnectingState() {
        connectionManager.setConnectingState()
    }

    /**
     * Set visual Disconnecting state without actually disconnecting
     * Used to show "Disconnecting..." while ad is loading
     */
    fun setDisconnectingState() {
        connectionManager.setDisconnectingState()
    }

    fun onVpnPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            showVpnPermissionDialog = false,
            vpnPermissionIntent = null
        )

        if (granted) {
            connect()
        }
    }

    fun showAddCustomDns() {
        _uiState.value = _uiState.value.copy(showCustomDnsDialog = true)
    }

    fun hideAddCustomDns() {
        _uiState.value = _uiState.value.copy(showCustomDnsDialog = false)
    }

    fun addCustomDns(
        name: String,
        primaryDns: String,
        secondaryDns: String,
        isDoH: Boolean = false,
        dohUrl: String? = null
    ) {
        val customServer = DnsServer(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            primaryDns = primaryDns,
            secondaryDns = secondaryDns,
            category = DnsCategory.CUSTOM,
            description = if (isDoH) "Custom DoH server" else "Custom DNS server",
            isCustom = true,
            isDoH = isDoH,
            dohUrl = dohUrl
        )

        viewModelScope.launch {
            dnsRepository.addCustomServer(customServer)
            // No need to call loadServers() - Flow will auto-update
        }

        hideAddCustomDns()
    }

    fun removeCustomDns(serverId: String) {
        viewModelScope.launch {
            dnsRepository.removeCustomServer(serverId)
            // Also remove from speed test results if present
            speedTestService.removeServerFromResults(serverId)
            // Flow will auto-update the servers list
        }
    }

    fun dismissVpnDialog() {
        _uiState.value = _uiState.value.copy(
            showVpnPermissionDialog = false,
            vpnPermissionIntent = null
        )
    }

    fun dismissConnectionSuccessOverlay() {
        _uiState.value = _uiState.value.copy(showConnectionSuccessOverlay = false)
    }

    fun dismissDisconnectionOverlay() {
        _uiState.value = _uiState.value.copy(showDisconnectionOverlay = false)
    }
}
