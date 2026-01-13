package com.dns.changer.ultimate.data.model

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val server: DnsServer) : ConnectionState()
    data object Disconnecting : ConnectionState()
    data class Switching(val toServer: DnsServer) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class SpeedTestResult(
    val server: DnsServer,
    val latencyMs: Long,
    val rating: LatencyRating
)

enum class LatencyRating(val displayName: String, val maxLatency: Long) {
    EXCELLENT("Excellent", 30),
    GOOD("Good", 60),
    FAIR("Fair", 100),
    POOR("Poor", Long.MAX_VALUE);

    companion object {
        fun fromLatency(latencyMs: Long): LatencyRating = when {
            latencyMs <= EXCELLENT.maxLatency -> EXCELLENT
            latencyMs <= GOOD.maxLatency -> GOOD
            latencyMs <= FAIR.maxLatency -> FAIR
            else -> POOR
        }
    }
}

data class SpeedTestState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val currentServer: DnsServer? = null,
    val results: List<SpeedTestResult> = emptyList(),
    val fastestResult: SpeedTestResult? = null
)
