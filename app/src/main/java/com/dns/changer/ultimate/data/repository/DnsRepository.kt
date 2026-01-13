package com.dns.changer.ultimate.data.repository

import com.dns.changer.ultimate.data.model.DnsCategory
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.PresetDnsServers
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsRepository @Inject constructor(
    private val preferences: DnsPreferences
) {
    val selectedDnsId: Flow<String?> = preferences.selectedDnsId

    val isConnected: Flow<Boolean> = preferences.isConnected

    val selectedServer: Flow<DnsServer?> = preferences.selectedDnsId.map { id ->
        id?.let { serverId ->
            PresetDnsServers.getById(serverId) ?: parseCustomServers(preferences.customDnsList.first()).find { it.id == serverId }
        }
    }

    val allServers: Flow<List<DnsServer>> = preferences.customDnsList.map { json ->
        val customServers = parseCustomServers(json)
        PresetDnsServers.all + customServers
    }

    val customServers: Flow<List<DnsServer>> = preferences.customDnsList.map { json ->
        parseCustomServers(json)
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
        val current = parseCustomServers(preferences.customDnsList.first()).toMutableList()
        current.add(server.copy(isCustom = true, category = DnsCategory.CUSTOM))
        saveCustomServers(current)
    }

    suspend fun removeCustomServer(serverId: String) {
        val current = parseCustomServers(preferences.customDnsList.first()).toMutableList()
        current.removeAll { it.id == serverId }
        saveCustomServers(current)

        // If the removed server was selected, clear selection
        if (preferences.selectedDnsId.first() == serverId) {
            clearSelection()
        }
    }

    suspend fun getCustomServersList(): List<DnsServer> {
        return parseCustomServers(preferences.customDnsList.first())
    }

    private fun parseCustomServers(json: String?): List<DnsServer> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val servers = mutableListOf<DnsServer>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                servers.add(
                    DnsServer(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        primaryDns = obj.getString("primaryDns"),
                        secondaryDns = obj.getString("secondaryDns"),
                        category = DnsCategory.CUSTOM,
                        description = obj.optString("description", "Custom DNS server"),
                        isCustom = true
                    )
                )
            }
            servers
        } catch (e: Exception) {
            android.util.Log.e("DnsRepository", "Error parsing custom servers: ${e.message}")
            emptyList()
        }
    }

    private suspend fun saveCustomServers(servers: List<DnsServer>) {
        val jsonArray = JSONArray()
        servers.forEach { server ->
            val obj = JSONObject().apply {
                put("id", server.id)
                put("name", server.name)
                put("primaryDns", server.primaryDns)
                put("secondaryDns", server.secondaryDns)
                put("description", server.description)
            }
            jsonArray.put(obj)
        }
        preferences.setCustomDnsList(jsonArray.toString())
    }
}
