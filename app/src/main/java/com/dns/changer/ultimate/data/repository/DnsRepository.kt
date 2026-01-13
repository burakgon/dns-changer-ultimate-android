package com.dns.changer.ultimate.data.repository

import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsRepository @Inject constructor(
    private val preferences: DnsPreferences
) {
    val selectedDnsId: Flow<String?> = preferences.selectedDnsId

    val isConnected: Flow<Boolean> = preferences.isConnected

    val selectedServer: Flow<DnsServer?> = preferences.selectedDnsId.map { id ->
        id?.let { PresetDnsServers.getById(it) ?: getCustomServers().find { server -> server.id == it } }
    }

    val allServers: Flow<List<DnsServer>> = preferences.customDnsList.map { json ->
        val customServers = parseCustomServers(json)
        PresetDnsServers.all + customServers
    }

    suspend fun selectServer(server: DnsServer) {
        preferences.setSelectedDnsId(server.id)
    }

    suspend fun clearSelection() {
        preferences.setSelectedDnsId(null)
    }

    suspend fun setConnected(connected: Boolean) {
        preferences.setConnected(connected)
    }

    suspend fun addCustomServer(server: DnsServer) {
        val current = getCustomServers().toMutableList()
        current.add(server.copy(isCustom = true))
        saveCustomServers(current)
    }

    suspend fun removeCustomServer(serverId: String) {
        val current = getCustomServers().toMutableList()
        current.removeAll { it.id == serverId }
        saveCustomServers(current)
    }

    private fun getCustomServers(): List<DnsServer> {
        // This would need to be called in a coroutine context
        // For now, return empty list - actual implementation would use runBlocking or be suspend
        return emptyList()
    }

    private fun parseCustomServers(json: String?): List<DnsServer> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            // Simple parsing - in production, use proper serialization
            emptyList() // Placeholder - implement JSON parsing
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveCustomServers(servers: List<DnsServer>) {
        // Implement JSON serialization
        val json = "" // Placeholder
        preferences.setCustomDnsList(json)
    }
}
