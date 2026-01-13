package com.dns.changer.ultimate.service

import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.LatencyRating
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.model.SpeedTestResult
import com.dns.changer.ultimate.data.model.SpeedTestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsSpeedTestService @Inject constructor() {

    private val _state = MutableStateFlow(SpeedTestState())
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    // Flag to cancel running test
    @Volatile
    private var isCancelled = false

    suspend fun runSpeedTest(servers: List<DnsServer> = PresetDnsServers.all) {
        if (_state.value.isRunning) return

        isCancelled = false
        _state.value = SpeedTestState(isRunning = true, progress = 0f, results = emptyList(), totalServers = servers.size)
        val results = mutableListOf<SpeedTestResult>()

        withContext(Dispatchers.IO) {
            servers.forEachIndexed { index, server ->
                // Check if cancelled
                if (isCancelled) return@withContext

                // Update current server being tested
                _state.value = _state.value.copy(
                    currentServer = server
                )

                val latency = measureDnsLatency(server.primaryDns)

                // Check again after network operation
                if (isCancelled) return@withContext

                if (latency >= 0) {
                    val result = SpeedTestResult(
                        server = server,
                        latencyMs = latency,
                        rating = LatencyRating.fromLatency(latency)
                    )
                    results.add(result)

                    // Sort and update results immediately so UI shows them dynamically
                    val sortedResults = results.sortedBy { it.latencyMs }
                    _state.value = _state.value.copy(
                        results = sortedResults,
                        fastestResult = sortedResults.firstOrNull(),
                        progress = (index + 1).toFloat() / servers.size
                    )
                } else {
                    // Still update progress even if test failed
                    _state.value = _state.value.copy(
                        progress = (index + 1).toFloat() / servers.size
                    )
                }

                // Small delay between tests for visual effect
                delay(50)
            }
        }

        // Only mark as complete if not cancelled
        if (!isCancelled) {
            val sortedResults = results.sortedBy { it.latencyMs }
            _state.value = SpeedTestState(
                isRunning = false,
                progress = 1f,
                results = sortedResults,
                fastestResult = sortedResults.firstOrNull()
            )
        }
    }

    fun stopAndClear() {
        isCancelled = true
        _state.value = SpeedTestState()
    }

    fun reset() {
        _state.value = SpeedTestState()
    }

    fun removeServerFromResults(serverId: String) {
        val currentState = _state.value
        if (currentState.results.isEmpty()) return

        val filteredResults = currentState.results.filter { it.server.id != serverId }
        _state.value = currentState.copy(
            results = filteredResults,
            fastestResult = filteredResults.firstOrNull()
        )
    }

    private fun measureDnsLatency(dnsServer: String): Long {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = 3000 // 3 second timeout for faster testing

            // Simple DNS query for google.com
            val query = buildDnsQuery("google.com")
            val address = InetAddress.getByName(dnsServer)

            val startTime = System.currentTimeMillis()

            val packet = DatagramPacket(query, query.size, address, 53)
            socket.send(packet)

            val responseBuffer = ByteArray(512)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)

            val endTime = System.currentTimeMillis()
            socket.close()

            endTime - startTime
        } catch (e: Exception) {
            -1L // Return -1 for failed tests
        }
    }

    private fun buildDnsQuery(domain: String): ByteArray {
        val buffer = mutableListOf<Byte>()

        // Transaction ID
        buffer.add(0x12.toByte())
        buffer.add(0x34.toByte())

        // Flags (standard query)
        buffer.add(0x01.toByte())
        buffer.add(0x00.toByte())

        // Questions: 1
        buffer.add(0x00.toByte())
        buffer.add(0x01.toByte())

        // Answer RRs: 0
        buffer.add(0x00.toByte())
        buffer.add(0x00.toByte())

        // Authority RRs: 0
        buffer.add(0x00.toByte())
        buffer.add(0x00.toByte())

        // Additional RRs: 0
        buffer.add(0x00.toByte())
        buffer.add(0x00.toByte())

        // Query Name
        domain.split(".").forEach { part ->
            buffer.add(part.length.toByte())
            part.forEach { buffer.add(it.code.toByte()) }
        }
        buffer.add(0x00.toByte()) // End of name

        // Type A (1)
        buffer.add(0x00.toByte())
        buffer.add(0x01.toByte())

        // Class IN (1)
        buffer.add(0x00.toByte())
        buffer.add(0x01.toByte())

        return buffer.toByteArray()
    }
}
