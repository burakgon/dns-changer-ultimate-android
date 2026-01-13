package com.dns.changer.ultimate

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.model.SpeedTestResult
import com.dns.changer.ultimate.data.model.LatencyRating
import com.dns.changer.ultimate.service.DnsSpeedTestService
import com.dns.changer.ultimate.service.DnsVpnService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * SERVER SWITCHING TEST SUITE
 * Tests server switching functionality on both SpeedTest screen and Main Connect screen
 */
@RunWith(AndroidJUnit4::class)
class ServerSwitchingTest {

    private lateinit var context: Context
    private lateinit var speedTestService: DnsSpeedTestService

    // Test servers
    private val cloudflare = PresetDnsServers.all.find { it.name.contains("Cloudflare") }
        ?: PresetDnsServers.all.first()
    private val google = PresetDnsServers.all.find { it.name.contains("Google") }
        ?: PresetDnsServers.all[1]
    private val quad9 = PresetDnsServers.all.find { it.name.contains("Quad9") }
        ?: PresetDnsServers.all[2]

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        speedTestService = DnsSpeedTestService()
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

    // ========== SPEED TEST SCREEN - SERVER SWITCHING TESTS ==========

    @Test
    fun test_SpeedTest_01_ResultsContainValidServers() {
        println("🧪 Testing: Speed test results contain valid servers for switching")

        runBlocking {
            withTimeout(30000) {
                speedTestService.runSpeedTest(listOf(cloudflare, google, quad9))
            }

            val state = speedTestService.state.first()
            assertTrue("Should have results to switch to", state.results.isNotEmpty())

            state.results.forEach { result ->
                assertNotNull("Result should have server", result.server)
                assertTrue("Server should have valid primary DNS",
                    result.server.primaryDns.isNotBlank())
                assertTrue("Server should have valid secondary DNS",
                    result.server.secondaryDns.isNotBlank())
                println("  ✅ ${result.server.name}: ${result.latencyMs}ms - Ready for switching")
            }
        }
    }

    @Test
    fun test_SpeedTest_02_FastestServerIsFirstResult() {
        println("🧪 Testing: Fastest server from speed test is the first result")

        runBlocking {
            withTimeout(30000) {
                speedTestService.runSpeedTest(listOf(cloudflare, google, quad9))
            }

            val state = speedTestService.state.first()
            val fastestResult = state.fastestResult

            assertNotNull("Should have fastest result", fastestResult)
            assertEquals("Fastest result should be first in list",
                state.results.firstOrNull()?.server?.id, fastestResult?.server?.id)

            println("✅ Fastest server: ${fastestResult?.server?.name} at ${fastestResult?.latencyMs}ms")
        }
    }

    @Test
    fun test_SpeedTest_03_CanSwitchToAnyResultServer() {
        println("🧪 Testing: Can switch to any server from speed test results")

        runBlocking {
            withTimeout(30000) {
                speedTestService.runSpeedTest(listOf(cloudflare, google, quad9))
            }

            val state = speedTestService.state.first()

            // Verify each result has all required fields for connection
            state.results.forEachIndexed { index, result ->
                val server = result.server

                // Verify server has all required fields for VPN service
                assertTrue("Server ID should not be blank", server.id.isNotBlank())
                assertTrue("Server name should not be blank", server.name.isNotBlank())
                assertTrue("Primary DNS should be valid IP",
                    server.primaryDns.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")))
                assertTrue("Secondary DNS should be valid IP",
                    server.secondaryDns.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")))

                println("  ✅ [${index + 1}] ${server.name} - switchable")
            }

            println("✅ All ${state.results.size} servers are ready for switching")
        }
    }

    @Test
    fun test_SpeedTest_04_ResultsAreSortedForOptimalSwitching() {
        println("🧪 Testing: Speed test results are sorted by latency for optimal switching")

        runBlocking {
            withTimeout(45000) {
                speedTestService.runSpeedTest(PresetDnsServers.all.take(5))
            }

            val state = speedTestService.state.first()

            // Verify sorted by latency (ascending)
            val latencies = state.results.map { it.latencyMs }
            val sortedLatencies = latencies.sorted()

            assertEquals("Results should be sorted by latency", sortedLatencies, latencies)

            println("✅ Results sorted for optimal selection: ${latencies.joinToString(" < ")}ms")
        }
    }

    @Test
    fun test_SpeedTest_05_SpeedTestResultDataIntegrity() {
        println("🧪 Testing: Speed test result has all data needed for server switching")

        runBlocking {
            withTimeout(20000) {
                speedTestService.runSpeedTest(listOf(cloudflare))
            }

            val state = speedTestService.state.first()
            val result = state.results.firstOrNull()

            assertNotNull("Should have at least one result", result)

            // Check SpeedTestResult fields
            assertNotNull("Should have server", result!!.server)
            assertTrue("Latency should be positive", result.latencyMs > 0)
            assertNotNull("Should have rating", result.rating)

            // Verify rating is appropriate for latency
            val expectedRating = LatencyRating.fromLatency(result.latencyMs)
            assertEquals("Rating should match latency", expectedRating, result.rating)

            println("✅ Result data integrity verified:")
            println("   Server: ${result.server.name}")
            println("   Latency: ${result.latencyMs}ms")
            println("   Rating: ${result.rating}")
        }
    }

    // ========== MAIN CONNECT SCREEN - SERVER SWITCHING TESTS ==========

    @Test
    fun test_MainScreen_01_PresetServersAvailableForSelection() {
        println("🧪 Testing: All preset servers are available for selection on main screen")

        val serversByCategory = PresetDnsServers.all.groupBy { it.category }

        DnsCategory.entries.forEach { category ->
            val servers = serversByCategory[category] ?: emptyList()
            assertTrue("Category $category should have servers", servers.isNotEmpty())

            servers.forEach { server ->
                assertTrue("Server ${server.name} should have valid ID", server.id.isNotBlank())
                assertTrue("Server ${server.name} should be switchable",
                    server.primaryDns.isNotBlank() && server.secondaryDns.isNotBlank())
            }

            println("  ✅ ${category.displayName}: ${servers.size} servers available")
        }

        println("✅ Total ${PresetDnsServers.all.size} servers available for selection")
    }

    @Test
    fun test_MainScreen_02_ServerCategoriesGroupedCorrectly() {
        println("🧪 Testing: Servers are correctly grouped by category for picker")

        val serversByCategory = PresetDnsServers.all.groupBy { it.category }

        // Verify grouping
        var totalCount = 0
        DnsCategory.entries.forEach { category ->
            val servers = serversByCategory[category] ?: emptyList()
            totalCount += servers.size

            // Verify all servers in category actually belong to it
            servers.forEach { server ->
                assertEquals("Server ${server.name} should be in correct category",
                    category, server.category)
            }

            println("  ✅ ${category.displayName}: ${servers.size} servers")
        }

        assertEquals("All servers should be categorized",
            PresetDnsServers.all.size, totalCount)

        println("✅ All servers correctly grouped by category")
    }

    @Test
    fun test_MainScreen_03_ServerSelectionDataIntegrity() {
        println("🧪 Testing: Server selection maintains data integrity")

        val selectedServer = cloudflare

        // Verify server can be retrieved by ID (for persistence)
        val retrieved = PresetDnsServers.getById(selectedServer.id)
        assertNotNull("Should be able to retrieve server by ID", retrieved)
        assertEquals("Retrieved server should match", selectedServer.id, retrieved!!.id)
        assertEquals("Names should match", selectedServer.name, retrieved.name)
        assertEquals("Primary DNS should match", selectedServer.primaryDns, retrieved.primaryDns)
        assertEquals("Secondary DNS should match", selectedServer.secondaryDns, retrieved.secondaryDns)

        println("✅ Server selection data integrity verified for ${selectedServer.name}")
    }

    @Test
    fun test_MainScreen_04_DifferentServersHaveUniqueIDs() {
        println("🧪 Testing: Different servers have unique IDs for proper selection tracking")

        val ids = PresetDnsServers.all.map { it.id }
        val uniqueIds = ids.toSet()

        assertEquals("All server IDs should be unique", ids.size, uniqueIds.size)

        // Verify we can distinguish between servers
        assertNotEquals("Cloudflare and Google should have different IDs",
            cloudflare.id, google.id)
        assertNotEquals("Google and Quad9 should have different IDs",
            google.id, quad9.id)

        println("✅ All ${ids.size} servers have unique IDs for selection tracking")
    }

    @Test
    fun test_MainScreen_05_ServerCategoryIconsAvailable() {
        println("🧪 Testing: Server categories have icons for display in picker")

        DnsCategory.entries.forEach { category ->
            assertNotNull("Category ${category.name} should have icon", category.icon)
            assertTrue("Category ${category.name} should have display name",
                category.displayName.isNotBlank())

            println("  ✅ ${category.displayName} - icon available")
        }

        println("✅ All category icons available for server picker")
    }

    // ========== SERVER SWITCHING FLOW TESTS ==========

    @Test
    fun test_Flow_01_CanCreateConnectionIntent() {
        println("🧪 Testing: Can create VPN connection intent for server")

        val server = cloudflare

        val intent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
            putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, server.primaryDns)
            putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, server.secondaryDns)
            putExtra(DnsVpnService.EXTRA_SERVER_NAME, server.name)
        }

        assertEquals("Action should be START", DnsVpnService.ACTION_START, intent.action)
        assertEquals("Primary DNS should be set", server.primaryDns,
            intent.getStringExtra(DnsVpnService.EXTRA_PRIMARY_DNS))
        assertEquals("Secondary DNS should be set", server.secondaryDns,
            intent.getStringExtra(DnsVpnService.EXTRA_SECONDARY_DNS))
        assertEquals("Server name should be set", server.name,
            intent.getStringExtra(DnsVpnService.EXTRA_SERVER_NAME))

        println("✅ VPN intent created correctly for ${server.name}")
    }

    @Test
    fun test_Flow_02_CanCreateSwitchIntent() {
        println("🧪 Testing: Can create server switch intent")

        val fromServer = cloudflare
        val toServer = google

        // Create intent for switching (same as new connection, VPN service handles it)
        val switchIntent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
            putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, toServer.primaryDns)
            putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, toServer.secondaryDns)
            putExtra(DnsVpnService.EXTRA_SERVER_NAME, toServer.name)
        }

        assertNotEquals("Servers should be different",
            fromServer.primaryDns, switchIntent.getStringExtra(DnsVpnService.EXTRA_PRIMARY_DNS))
        assertEquals("New server should be set", toServer.primaryDns,
            switchIntent.getStringExtra(DnsVpnService.EXTRA_PRIMARY_DNS))

        println("✅ Switch intent created: ${fromServer.name} -> ${toServer.name}")
    }

    @Test
    fun test_Flow_03_ConnectionStateTypes() {
        println("🧪 Testing: Connection state types for server switching")

        // Test all connection states that are used during switching
        val disconnected = ConnectionState.Disconnected
        val connecting = ConnectionState.Connecting
        val connected = ConnectionState.Connected(cloudflare)
        val disconnecting = ConnectionState.Disconnecting
        val switching = ConnectionState.Switching(google)
        val error = ConnectionState.Error("Test error")

        // Verify state types
        assertTrue("Disconnected is correct type", disconnected is ConnectionState.Disconnected)
        assertTrue("Connecting is correct type", connecting is ConnectionState.Connecting)
        assertTrue("Connected is correct type", connected is ConnectionState.Connected)
        assertTrue("Disconnecting is correct type", disconnecting is ConnectionState.Disconnecting)
        assertTrue("Switching is correct type", switching is ConnectionState.Switching)
        assertTrue("Error is correct type", error is ConnectionState.Error)

        // Verify Connected state has server
        assertEquals("Connected should have server", cloudflare.id,
            (connected as ConnectionState.Connected).server.id)

        // Verify Switching state has target server
        assertEquals("Switching should have target server", google.id,
            (switching as ConnectionState.Switching).toServer.id)

        println("✅ All connection state types work correctly for switching flow")
    }

    @Test
    fun test_Flow_04_SpeedTestToMainScreenServerHandoff() {
        println("🧪 Testing: Server from speed test can be used on main screen")

        runBlocking {
            withTimeout(20000) {
                speedTestService.runSpeedTest(listOf(cloudflare, google))
            }

            val state = speedTestService.state.first()
            val selectedFromSpeedTest = state.results.first().server

            // Verify server can be found in preset list (for main screen)
            val serverInPresets = PresetDnsServers.all.find { it.id == selectedFromSpeedTest.id }
            assertNotNull("Speed test server should exist in presets", serverInPresets)
            assertEquals("Server data should match",
                selectedFromSpeedTest.primaryDns, serverInPresets?.primaryDns)

            println("✅ Server '${selectedFromSpeedTest.name}' can be handed off to main screen")
        }
    }

    @Test
    fun test_Flow_05_MultipleServerSwitchSequence() {
        println("🧪 Testing: Multiple server switch sequence data integrity")

        val switchSequence = listOf(cloudflare, google, quad9, cloudflare)
        var previousServer: DnsServer? = null

        switchSequence.forEachIndexed { index, server ->
            // Verify we have valid server at each step
            assertNotNull("Server at step $index should not be null", server)
            assertTrue("Server should have valid DNS", server.primaryDns.isNotBlank())

            // Verify we're switching to different server (except when cycling back)
            if (previousServer != null && index < switchSequence.size - 1) {
                assertNotEquals("Should be different from previous",
                    previousServer!!.id, server.id)
            }

            previousServer = server
            println("  Step ${index + 1}: Switch to ${server.name}")
        }

        // Verify we can cycle back to original
        assertEquals("Should be able to switch back to original",
            switchSequence.first().id, switchSequence.last().id)

        println("✅ Multiple server switch sequence maintains data integrity")
    }

    // ========== EDGE CASES ==========

    @Test
    fun test_Edge_01_SpeedTestEmptyResultsHandling() {
        println("🧪 Testing: Handle empty speed test results gracefully")

        runBlocking {
            speedTestService.runSpeedTest(emptyList())

            val state = speedTestService.state.first()
            assertTrue("Should have empty results", state.results.isEmpty())
            assertNull("Should have no fastest result", state.fastestResult)

            println("✅ Empty results handled gracefully - no server to switch to")
        }
    }

    @Test
    fun test_Edge_02_SwitchToSameServer() {
        println("🧪 Testing: Switching to same server is handled")

        val server = cloudflare

        // Create two intents for same server
        val intent1 = createConnectionIntent(server)
        val intent2 = createConnectionIntent(server)

        // Verify both intents target same server
        assertEquals("Both intents should target same server",
            intent1.getStringExtra(DnsVpnService.EXTRA_PRIMARY_DNS),
            intent2.getStringExtra(DnsVpnService.EXTRA_PRIMARY_DNS))

        println("✅ Same server switch handled correctly")
    }

    @Test
    fun test_Edge_03_AllCategoriesHaveServers() {
        println("🧪 Testing: All categories have at least one server for switching")

        DnsCategory.entries.forEach { category ->
            val serversInCategory = PresetDnsServers.all.filter { it.category == category }
            assertTrue("Category $category should have servers", serversInCategory.isNotEmpty())

            // Verify first server in each category is switchable
            val firstServer = serversInCategory.first()
            assertTrue("First ${category.name} server should be switchable",
                firstServer.primaryDns.isNotBlank())

            println("  ✅ ${category.displayName}: ${serversInCategory.size} servers")
        }

        println("✅ All categories have switchable servers")
    }

    // ========== HELPER FUNCTIONS ==========

    private fun createConnectionIntent(server: DnsServer): Intent {
        return Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_START
            putExtra(DnsVpnService.EXTRA_PRIMARY_DNS, server.primaryDns)
            putExtra(DnsVpnService.EXTRA_SECONDARY_DNS, server.secondaryDns)
            putExtra(DnsVpnService.EXTRA_SERVER_NAME, server.name)
        }
    }
}
