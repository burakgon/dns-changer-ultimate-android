package com.dns.changer.ultimate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.SpeedTestState
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

    // Auto-start flag - set when navigating from "Find Fastest" button
    private val _shouldAutoStart = MutableStateFlow(false)
    val shouldAutoStart: StateFlow<Boolean> = _shouldAutoStart.asStateFlow()

    fun requestAutoStart() {
        _shouldAutoStart.value = true
    }

    fun clearAutoStart() {
        _shouldAutoStart.value = false
    }

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

    /**
     * Selects a server from speed test results.
     * Always navigates to main screen to go through the full connection flow.
     */
    fun selectServer(server: DnsServer) {
        viewModelScope.launch {
            dnsRepository.selectServer(server)
        }
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
