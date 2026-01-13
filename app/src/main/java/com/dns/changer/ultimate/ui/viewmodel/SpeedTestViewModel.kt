package com.dns.changer.ultimate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val speedTestService: DnsSpeedTestService,
    private val connectionManager: DnsConnectionManager,
    private val dnsRepository: com.dns.changer.ultimate.data.repository.DnsRepository
) : ViewModel() {

    val speedTestState: StateFlow<SpeedTestState> = speedTestService.state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SpeedTestState()
        )

    private val _showPremiumGate = MutableStateFlow(false)
    val showPremiumGate: StateFlow<Boolean> = _showPremiumGate.asStateFlow()

    fun startSpeedTest(isPremium: Boolean, hasWatchedAd: Boolean) {
        if (!isPremium && !hasWatchedAd) {
            _showPremiumGate.value = true
            return
        }

        viewModelScope.launch {
            speedTestService.runSpeedTest(PresetDnsServers.all)
        }
    }

    fun resetTest() {
        speedTestService.reset()
    }

    fun connectToServer(server: DnsServer) {
        viewModelScope.launch {
            // Save the selected server so MainViewModel can track it
            dnsRepository.selectServer(server)
            connectionManager.connect(server)
        }
    }

    fun dismissPremiumGate() {
        _showPremiumGate.value = false
    }

    fun onAdWatched() {
        _showPremiumGate.value = false
        viewModelScope.launch {
            speedTestService.runSpeedTest(PresetDnsServers.all)
        }
    }

    fun onPremiumPurchased() {
        _showPremiumGate.value = false
        viewModelScope.launch {
            speedTestService.runSpeedTest(PresetDnsServers.all)
        }
    }
}
