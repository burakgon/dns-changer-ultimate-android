package com.dns.changer.ultimate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dns.changer.ultimate.ads.AnalyticsEvents
import com.dns.changer.ultimate.ads.AnalyticsManager
import com.dns.changer.ultimate.ads.AnalyticsParams
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.service.DnsConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject

data class DnsLeakResult(
    val ip: String,
    val countryCode: String?,      // 2-letter code (e.g., "us")
    val countryName: String?,      // Full name (e.g., "United States of America")
    val provider: String?,         // Cleaned provider name (e.g., "Google LLC")
    val asn: String?               // Full ASN (e.g., "AS15169 Google LLC")
) {
    // Get flag emoji from country code
    val flagEmoji: String?
        get() = countryCode?.uppercase()?.takeIf { it.length == 2 }?.let { code ->
            val firstChar = Character.toChars(0x1F1E6 - 'A'.code + code[0].code)
            val secondChar = Character.toChars(0x1F1E6 - 'A'.code + code[1].code)
            String(firstChar) + String(secondChar)
        }
}

enum class LeakTestStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    COMPLETED_SECURE,
    COMPLETED_NOT_PROTECTED,
    COMPLETED_LEAK_DETECTED
}

data class LeakTestState(
    val status: LeakTestStatus = LeakTestStatus.IDLE,
    val results: List<DnsLeakResult> = emptyList(),
    val userPublicIp: String? = null,
    val progress: Float = 0f
)

@HiltViewModel
class LeakTestViewModel @Inject constructor(
    private val connectionManager: DnsConnectionManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _state = MutableStateFlow(LeakTestState())
    val state: StateFlow<LeakTestState> = _state.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    // Reuse OkHttpClient to leverage connection pooling and reduce resource overhead
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun startTest() {
        viewModelScope.launch {
            _state.value = LeakTestState(status = LeakTestStatus.RUNNING)

            val testResult = performDnsLeakTest { progress ->
                _state.value = _state.value.copy(progress = progress)
            }

            // Determine status based on connection state
            val isConnectedToVpn = connectionManager.connectionState.value is ConnectionState.Connected
            val finalStatus = if (isConnectedToVpn) {
                LeakTestStatus.COMPLETED_SECURE
            } else {
                LeakTestStatus.COMPLETED_NOT_PROTECTED
            }

            _state.value = _state.value.copy(
                status = finalStatus,
                results = testResult.first,
                userPublicIp = testResult.second,
                progress = 1f
            )

            // Log leak test completion
            val isLeaked = finalStatus == LeakTestStatus.COMPLETED_LEAK_DETECTED
            analyticsManager.logEvent(AnalyticsEvents.LEAK_TEST_COMPLETED, mapOf(
                AnalyticsParams.IS_LEAKED to isLeaked,
                AnalyticsParams.RESOLVER_COUNT to testResult.first.size
            ))
        }
    }

    fun clearState() {
        _state.value = LeakTestState()
    }

    private suspend fun performDnsLeakTest(
        onProgress: (Float) -> Unit
    ): Pair<List<DnsLeakResult>, String?> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DnsLeakResult>()
        var userPublicIp: String? = null

        // Use class-level httpClient for connection pooling
        val client = httpClient

        try {
            // Step 1: Get unique test ID from bash.ws
            onProgress(0.1f)
            android.util.Log.d("DnsLeakTest", "Step 1: Getting test ID from bash.ws")

            val idRequest = okhttp3.Request.Builder()
                .url("https://bash.ws/id")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val testId = client.newCall(idRequest).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.trim() ?: ""
                } else {
                    ""
                }
            }

            if (testId.isBlank()) {
                android.util.Log.e("DnsLeakTest", "Failed to get test ID")
                return@withContext Pair(emptyList(), null)
            }

            android.util.Log.d("DnsLeakTest", "Got test ID: $testId")
            onProgress(0.2f)

            // Step 2: Trigger 10 DNS queries in parallel
            android.util.Log.d("DnsLeakTest", "Step 2: Triggering 10 DNS queries")

            val dnsJobs = (1..10).map { i ->
                async {
                    try {
                        val request = okhttp3.Request.Builder()
                            .url("https://$i.$testId.bash.ws")
                            .header("User-Agent", "Mozilla/5.0")
                            .build()
                        // We don't care about the response, just trigger the DNS lookup
                        client.newCall(request).execute().close()
                        android.util.Log.d("DnsLeakTest", "DNS query $i completed")
                    } catch (e: Exception) {
                        // Expected - the request might fail, but DNS lookup was still made
                        android.util.Log.d("DnsLeakTest", "DNS query $i triggered (${e.message})")
                    }
                }
            }

            // Wait for all DNS queries to complete
            dnsJobs.awaitAll()
            onProgress(0.5f)

            // Step 3: Wait 3 seconds for server to collect data
            android.util.Log.d("DnsLeakTest", "Step 3: Waiting for server to collect data")
            delay(3000)
            onProgress(0.7f)

            // Step 4: Fetch results
            android.util.Log.d("DnsLeakTest", "Step 4: Fetching results")

            val resultRequest = okhttp3.Request.Builder()
                .url("https://bash.ws/dnsleak/test/$testId?json")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(resultRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    android.util.Log.d("DnsLeakTest", "Results: $body")

                    try {
                        val jsonArray = JSONArray(body)
                        for (i in 0 until jsonArray.length()) {
                            val entry = jsonArray.getJSONObject(i)
                            val type = entry.optString("type", "")
                            val ip = entry.optString("ip", "")
                            val country = entry.optString("country_name", "")
                            val asn = entry.optString("asn", "")

                            when (type) {
                                "ip" -> {
                                    // User's public IP
                                    userPublicIp = ip
                                    android.util.Log.d("DnsLeakTest", "User public IP: $ip")
                                }
                                "dns" -> {
                                    // DNS server that handled queries
                                    if (ip.isNotBlank() && results.none { it.ip == ip }) {
                                        // Parse provider name from ASN (remove "AS12345 " prefix)
                                        val providerName = asn.replace(Regex("^AS\\d+\\s*"), "").trim()

                                        results.add(DnsLeakResult(
                                            ip = ip,
                                            countryCode = entry.optString("country", "").ifBlank { null },
                                            countryName = country.ifBlank { null },
                                            provider = providerName.ifBlank { null },
                                            asn = asn.ifBlank { null }
                                        ))
                                        android.util.Log.d("DnsLeakTest", "DNS server: $ip ($providerName, $country)")
                                    }
                                }
                                "conclusion" -> {
                                    // Ignore - we determine leak status ourselves
                                    android.util.Log.d("DnsLeakTest", "Conclusion: $ip")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DnsLeakTest", "Failed to parse results: ${e.message}")
                    }
                } else {
                    android.util.Log.e("DnsLeakTest", "Failed to fetch results: ${response.code}")
                }
            }

            onProgress(1.0f)

        } catch (e: Exception) {
            android.util.Log.e("DnsLeakTest", "DNS leak test failed: ${e.message}")
        }

        android.util.Log.d("DnsLeakTest", "Test complete. Found ${results.size} DNS servers, user IP: $userPublicIp")
        Pair(results, userPublicIp)
    }
}
