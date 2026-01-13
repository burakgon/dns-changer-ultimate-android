package com.dns.changer.ultimate

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.LatencyRating
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.model.SpeedTestResult
import com.dns.changer.ultimate.service.DnsSpeedTestService
import com.dns.changer.ultimate.service.DnsVpnService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.HttpURLConnection
import java.net.URL

/**
 * CRAZY COMPREHENSIVE TEST SUITE
 * Tests EVERYTHING in the DNS Changer app
 */
@RunWith(AndroidJUnit4::class)
class CrazyComprehensiveTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Ensure VPN is stopped before each test
        stopVpnService()
    }

    @After
    fun teardown() {
        stopVpnService()
    }

    private fun stopVpnService() {
        try {
            val intent = Intent(context, DnsVpnService::class.java).apply {
                action = DnsVpnService.ACTION_STOP
            }
            context.startService(intent)
            Thread.sleep(500)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // ========== DNS SERVER MODEL TESTS ==========

    @Test
    fun test_01_PresetDnsServers_NotEmpty() {
        println("🧪 Testing: PresetDnsServers should not be empty")
        assertTrue("PresetDnsServers.all should not be empty", PresetDnsServers.all.isNotEmpty())
        println("✅ Found ${PresetDnsServers.all.size} preset DNS servers")
    }

    @Test
    fun test_02_PresetDnsServers_HaveValidIPs() {
        println("🧪 Testing: All preset DNS servers have valid IPs")
        PresetDnsServers.all.forEach { server ->
            assertTrue("Server ${server.name} should have valid primary DNS",
                server.primaryDns.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")))
            assertTrue("Server ${server.name} should have valid secondary DNS",
                server.secondaryDns.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")))
            println("✅ ${server.name}: ${server.primaryDns} / ${server.secondaryDns}")
        }
    }

    @Test
    fun test_03_PresetDnsServers_HaveAllCategories() {
        println("🧪 Testing: Preset DNS servers cover all categories")
        val categories = PresetDnsServers.all.map { it.category }.toSet()
        DnsCategory.entries.forEach { category ->
            assertTrue("Category $category should have at least one server",
                categories.contains(category))
            val count = PresetDnsServers.all.count { it.category == category }
            println("✅ Category $category: $count servers")
        }
    }

    @Test
    fun test_04_PresetDnsServers_HaveUniqueIds() {
        println("🧪 Testing: All DNS servers have unique IDs")
        val ids = PresetDnsServers.all.map { it.id }
        assertEquals("All server IDs should be unique", ids.size, ids.toSet().size)
        println("✅ All ${ids.size} server IDs are unique")
    }

    @Test
    fun test_05_DnsServer_Equality() {
        println("🧪 Testing: DnsServer equality")
        val server1 = DnsServer("test", "Test", "1.1.1.1", "1.0.0.1", DnsCategory.SPEED, "Test")
        val server2 = DnsServer("test", "Test", "1.1.1.1", "1.0.0.1", DnsCategory.SPEED, "Test")
        assertEquals("Same servers should be equal", server1, server2)
        println("✅ DnsServer equality works correctly")
    }

    // ========== LATENCY RATING TESTS ==========

    @Test
    fun test_06_LatencyRating_Excellent() {
        println("🧪 Testing: LatencyRating.EXCELLENT for low latency")
        assertEquals(LatencyRating.EXCELLENT, LatencyRating.fromLatency(10))
        assertEquals(LatencyRating.EXCELLENT, LatencyRating.fromLatency(30))
        println("✅ EXCELLENT rating for latency <= 30ms")
    }

    @Test
    fun test_07_LatencyRating_Good() {
        println("🧪 Testing: LatencyRating.GOOD for medium latency")
        assertEquals(LatencyRating.GOOD, LatencyRating.fromLatency(31))
        assertEquals(LatencyRating.GOOD, LatencyRating.fromLatency(60))
        println("✅ GOOD rating for latency 31-60ms")
    }

    @Test
    fun test_08_LatencyRating_Fair() {
        println("🧪 Testing: LatencyRating.FAIR for higher latency")
        assertEquals(LatencyRating.FAIR, LatencyRating.fromLatency(61))
        assertEquals(LatencyRating.FAIR, LatencyRating.fromLatency(100))
        println("✅ FAIR rating for latency 61-100ms")
    }

    @Test
    fun test_09_LatencyRating_Poor() {
        println("🧪 Testing: LatencyRating.POOR for high latency")
        assertEquals(LatencyRating.POOR, LatencyRating.fromLatency(101))
        assertEquals(LatencyRating.POOR, LatencyRating.fromLatency(1000))
        println("✅ POOR rating for latency > 100ms")
    }

    // ========== DNS QUERY TESTS ==========

    @Test
    fun test_10_DnsQuery_Cloudflare() {
        println("🧪 Testing: DNS query to Cloudflare (1.1.1.1)")
        val latency = measureDnsLatency("1.1.1.1")
        assertTrue("Cloudflare DNS should respond (latency: $latency ms)", latency > 0)
        println("✅ Cloudflare responded in ${latency}ms")
    }

    @Test
    fun test_11_DnsQuery_Google() {
        println("🧪 Testing: DNS query to Google (8.8.8.8)")
        val latency = measureDnsLatency("8.8.8.8")
        assertTrue("Google DNS should respond (latency: $latency ms)", latency > 0)
        println("✅ Google responded in ${latency}ms")
    }

    @Test
    fun test_12_DnsQuery_Quad9() {
        println("🧪 Testing: DNS query to Quad9 (9.9.9.9)")
        val latency = measureDnsLatency("9.9.9.9")
        assertTrue("Quad9 DNS should respond (latency: $latency ms)", latency > 0)
        println("✅ Quad9 responded in ${latency}ms")
    }

    @Test
    fun test_13_DnsQuery_OpenDNS() {
        println("🧪 Testing: DNS query to OpenDNS (208.67.222.222)")
        val latency = measureDnsLatency("208.67.222.222")
        assertTrue("OpenDNS should respond (latency: $latency ms)", latency > 0)
        println("✅ OpenDNS responded in ${latency}ms")
    }

    @Test
    fun test_14_DnsQuery_AllPresetServers() {
        println("🧪 Testing: DNS query to ALL preset servers")
        var successCount = 0
        var failCount = 0

        PresetDnsServers.all.forEach { server ->
            val latency = measureDnsLatency(server.primaryDns)
            if (latency > 0) {
                successCount++
                println("  ✅ ${server.name}: ${latency}ms")
            } else {
                failCount++
                println("  ❌ ${server.name}: FAILED")
            }
        }

        println("📊 Results: $successCount succeeded, $failCount failed")
        assertTrue("At least 80% of servers should respond",
            successCount.toFloat() / PresetDnsServers.all.size >= 0.8f)
    }

    // ========== SPEED TEST SERVICE TESTS ==========

    @Test
    fun test_15_SpeedTestService_InitialState() {
        println("🧪 Testing: SpeedTestService initial state")
        val service = DnsSpeedTestService()
        runBlocking {
            val state = service.state.first()
            assertFalse("Should not be running initially", state.isRunning)
            assertEquals("Progress should be 0", 0f, state.progress)
            assertTrue("Results should be empty", state.results.isEmpty())
            assertNull("Fastest result should be null", state.fastestResult)
            println("✅ Initial state is correct")
        }
    }

    @Test
    fun test_16_SpeedTestService_RunWithLimitedServers() {
        println("🧪 Testing: SpeedTestService with 3 servers")
        val service = DnsSpeedTestService()
        val testServers = PresetDnsServers.all.take(3)

        runBlocking {
            withTimeout(30000) {
                service.runSpeedTest(testServers)
            }

            val state = service.state.first()
            assertFalse("Should not be running after completion", state.isRunning)
            assertEquals("Progress should be 1.0", 1f, state.progress)
            assertTrue("Should have some results", state.results.isNotEmpty())

            println("✅ Speed test completed with ${state.results.size} results")
            state.results.forEachIndexed { index, result ->
                println("  ${index + 1}. ${result.server.name}: ${result.latencyMs}ms (${result.rating})")
            }
        }
    }

    @Test
    fun test_17_SpeedTestService_ResultsSortedByLatency() {
        println("🧪 Testing: SpeedTest results are sorted by latency")
        val service = DnsSpeedTestService()
        val testServers = PresetDnsServers.all.take(5)

        runBlocking {
            withTimeout(45000) {
                service.runSpeedTest(testServers)
            }

            val state = service.state.first()
            val latencies = state.results.map { it.latencyMs }
            assertEquals("Results should be sorted by latency",
                latencies.sorted(), latencies)
            println("✅ Results correctly sorted: $latencies")
        }
    }

    @Test
    fun test_18_SpeedTestService_FastestResultCorrect() {
        println("🧪 Testing: SpeedTest fastestResult is correct")
        val service = DnsSpeedTestService()
        val testServers = PresetDnsServers.all.take(5)

        runBlocking {
            withTimeout(45000) {
                service.runSpeedTest(testServers)
            }

            val state = service.state.first()
            assertNotNull("Fastest result should not be null", state.fastestResult)
            assertEquals("Fastest result should match first in sorted list",
                state.results.firstOrNull(), state.fastestResult)
            println("✅ Fastest: ${state.fastestResult?.server?.name} at ${state.fastestResult?.latencyMs}ms")
        }
    }

    @Test
    fun test_19_SpeedTestService_Reset() {
        println("🧪 Testing: SpeedTestService reset")
        val service = DnsSpeedTestService()

        runBlocking {
            // Run a quick test first
            withTimeout(20000) {
                service.runSpeedTest(PresetDnsServers.all.take(2))
            }

            // Verify we have results
            var state = service.state.first()
            assertTrue("Should have results after test", state.results.isNotEmpty())

            // Reset
            service.reset()

            // Verify reset
            state = service.state.first()
            assertFalse("Should not be running after reset", state.isRunning)
            assertEquals("Progress should be 0 after reset", 0f, state.progress)
            assertTrue("Results should be empty after reset", state.results.isEmpty())
            assertNull("Fastest result should be null after reset", state.fastestResult)
            println("✅ Reset works correctly")
        }
    }

    // ========== VPN SERVICE TESTS ==========

    @Test
    fun test_20_VpnService_InitialState() {
        println("🧪 Testing: VPN service initial state")
        assertFalse("VPN should not be running initially", DnsVpnService.isVpnRunning)
        assertNull("Current server name should be null", DnsVpnService.currentServerName)
        println("✅ VPN initial state correct")
    }

    @Test
    fun test_21_VpnService_IntentActions() {
        println("🧪 Testing: VPN service intent actions")
        assertEquals("START action should match",
            "com.dns.changer.ultimate.START_VPN", DnsVpnService.ACTION_START)
        assertEquals("STOP action should match",
            "com.dns.changer.ultimate.STOP_VPN", DnsVpnService.ACTION_STOP)
        println("✅ VPN intent actions correct")
    }

    // ========== NETWORK CONNECTIVITY TESTS ==========

    @Test
    fun test_22_Network_HttpConnectivity() {
        println("🧪 Testing: HTTP connectivity")
        try {
            val url = URL("http://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            connection.disconnect()
            assertTrue("Should get HTTP 200 or 301/302", responseCode in 200..399)
            println("✅ HTTP connectivity works (response: $responseCode)")
        } catch (e: Exception) {
            println("⚠️ HTTP test failed: ${e.message}")
            // Don't fail - network might not be available in test environment
        }
    }

    @Test
    fun test_23_Network_HttpsConnectivity() {
        println("🧪 Testing: HTTPS connectivity")
        try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            connection.disconnect()
            assertTrue("Should get HTTPS 200 or 301/302", responseCode in 200..399)
            println("✅ HTTPS connectivity works (response: $responseCode)")
        } catch (e: Exception) {
            println("⚠️ HTTPS test failed: ${e.message}")
        }
    }

    // ========== DNS RESOLUTION TESTS ==========

    @Test
    fun test_24_DnsResolution_Google() {
        println("🧪 Testing: DNS resolution for google.com")
        try {
            val addresses = InetAddress.getAllByName("google.com")
            assertTrue("Should resolve google.com", addresses.isNotEmpty())
            println("✅ google.com resolved to: ${addresses.map { it.hostAddress }}")
        } catch (e: Exception) {
            println("⚠️ DNS resolution failed: ${e.message}")
        }
    }

    @Test
    fun test_25_DnsResolution_Multiple() {
        println("🧪 Testing: DNS resolution for multiple domains")
        val domains = listOf("amazon.com", "github.com", "cloudflare.com", "microsoft.com")
        var successCount = 0

        domains.forEach { domain ->
            try {
                val addresses = InetAddress.getAllByName(domain)
                if (addresses.isNotEmpty()) {
                    successCount++
                    println("  ✅ $domain: ${addresses.first().hostAddress}")
                }
            } catch (e: Exception) {
                println("  ❌ $domain: ${e.message}")
            }
        }

        assertTrue("Should resolve at least 50% of domains",
            successCount >= domains.size / 2)
        println("📊 Resolved $successCount/${domains.size} domains")
    }

    // ========== CONCURRENT TEST ==========

    @Test
    fun test_26_Concurrent_DnsQueries() {
        println("🧪 Testing: Concurrent DNS queries")
        val servers = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")
        val results = mutableListOf<Pair<String, Long>>()

        val threads = servers.map { server ->
            Thread {
                val latency = measureDnsLatency(server)
                synchronized(results) {
                    results.add(server to latency)
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join(10000) }

        assertEquals("Should have results from all servers", servers.size, results.size)
        results.forEach { (server, latency) ->
            println("  $server: ${latency}ms")
        }
        println("✅ Concurrent queries completed")
    }

    // ========== STRESS TEST ==========

    @Test
    fun test_27_Stress_MultipleDnsQueries() {
        println("🧪 Testing: Stress test - 10 consecutive DNS queries")
        val server = "1.1.1.1"
        val latencies = mutableListOf<Long>()

        repeat(10) { i ->
            val latency = measureDnsLatency(server)
            latencies.add(latency)
            println("  Query ${i + 1}: ${latency}ms")
        }

        val successCount = latencies.count { it > 0 }
        assertTrue("At least 80% of queries should succeed", successCount >= 8)

        val avgLatency = latencies.filter { it > 0 }.average()
        println("📊 Success: $successCount/10, Avg latency: ${avgLatency.toInt()}ms")
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun test_28_EdgeCase_InvalidDnsServer() {
        println("🧪 Testing: Invalid DNS server handling")
        val latency = measureDnsLatency("256.256.256.256")
        assertEquals("Invalid IP should return -1", -1L, latency)
        println("✅ Invalid DNS server handled correctly")
    }

    @Test
    fun test_29_EdgeCase_NonExistentDnsServer() {
        println("🧪 Testing: Non-existent DNS server handling")
        val latency = measureDnsLatency("192.0.2.1") // TEST-NET, should not respond
        assertTrue("Non-existent server should timeout (return -1)", latency == -1L)
        println("✅ Non-existent DNS server handled correctly")
    }

    @Test
    fun test_30_EdgeCase_EmptyServerList() {
        println("🧪 Testing: Speed test with empty server list")
        val service = DnsSpeedTestService()

        runBlocking {
            service.runSpeedTest(emptyList())
            val state = service.state.first()
            assertFalse("Should not be running", state.isRunning)
            assertTrue("Results should be empty", state.results.isEmpty())
            println("✅ Empty server list handled correctly")
        }
    }

    // ========== HELPER FUNCTIONS ==========

    private fun measureDnsLatency(dnsServer: String): Long {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = 3000

            // Build DNS query for google.com
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
            -1L
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
        buffer.add(0x00.toByte())

        // Type A (1)
        buffer.add(0x00.toByte())
        buffer.add(0x01.toByte())

        // Class IN (1)
        buffer.add(0x00.toByte())
        buffer.add(0x01.toByte())

        return buffer.toByteArray()
    }
}
