package com.dns.changer.ultimate.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.model.SpeedTestState
import com.dns.changer.ultimate.service.DnsConnectionManager
import com.dns.changer.ultimate.service.DnsSpeedTestService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val speedTestService: DnsSpeedTestService,
    private val connectionManager: DnsConnectionManager,
    private val dnsRepository: com.dns.changer.ultimate.data.repository.DnsRepository
) : ViewModel() {

    // Directly expose service state - service is Singleton so state survives config changes
    val speedTestState: StateFlow<SpeedTestState> = speedTestService.state

    // Total server count (preset + custom)
    val totalServerCount: StateFlow<Int> = dnsRepository.allServers
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 26
        )

    private val _showPremiumGate = MutableStateFlow(false)
    val showPremiumGate: StateFlow<Boolean> = _showPremiumGate.asStateFlow()

    // Session-based unlock for current speed test results (resets on new test)
    private val _resultsUnlockedForSession = MutableStateFlow(false)
    val resultsUnlockedForSession: StateFlow<Boolean> = _resultsUnlockedForSession.asStateFlow()

    // VPN permission state
    private val _vpnPermissionIntent = MutableStateFlow<Intent?>(null)
    val vpnPermissionIntent: StateFlow<Intent?> = _vpnPermissionIntent.asStateFlow()

    private var pendingConnectionServer: DnsServer? = null

    fun startSpeedTest(isPremium: Boolean) {
        // Reset session unlock when starting a new test (non-premium users need to watch ad again)
        if (!isPremium) {
            _resultsUnlockedForSession.value = false
        }

        viewModelScope.launch {
            // Get all servers including custom ones
            val allServers = dnsRepository.allServers.first()
            android.util.Log.d("SpeedTestViewModel", "Starting speed test with ${allServers.size} servers (${allServers.count { it.isCustom }} custom)")
            speedTestService.runSpeedTest(allServers)
        }
    }

    fun resetTest() {
        speedTestService.reset()
    }

    fun stopAndClearTest() {
        speedTestService.stopAndClear()
        _resultsUnlockedForSession.value = false
    }

    fun connectToServer(server: DnsServer) {
        // Check if already connected - use switch instead
        val currentState = connectionManager.connectionState.value
        if (currentState is ConnectionState.Connected) {
            // Already connected, just switch server (no permission needed)
            viewModelScope.launch {
                dnsRepository.selectServer(server)
                connectionManager.switchServer(server)
            }
            return
        }

        // Check VPN permission first
        val vpnIntent = connectionManager.checkVpnPermission()
        if (vpnIntent != null) {
            // Need permission - store pending server and emit intent
            pendingConnectionServer = server
            _vpnPermissionIntent.value = vpnIntent
            return
        }

        // Permission granted, connect directly
        viewModelScope.launch {
            dnsRepository.selectServer(server)
            connectionManager.connect(server)
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        _vpnPermissionIntent.value = null

        if (granted && pendingConnectionServer != null) {
            val server = pendingConnectionServer!!
            pendingConnectionServer = null
            viewModelScope.launch {
                dnsRepository.selectServer(server)
                connectionManager.connect(server)
            }
        } else {
            pendingConnectionServer = null
        }
    }

    fun dismissVpnPermission() {
        _vpnPermissionIntent.value = null
        pendingConnectionServer = null
    }

    fun dismissPremiumGate() {
        _showPremiumGate.value = false
    }

    fun onAdWatched() {
        _showPremiumGate.value = false
        // Unlock results for current session only
        _resultsUnlockedForSession.value = true
    }

    fun onPremiumPurchased() {
        _showPremiumGate.value = false
        viewModelScope.launch {
            val allServers = dnsRepository.allServers.first()
            speedTestService.runSpeedTest(allServers)
        }
    }
}
