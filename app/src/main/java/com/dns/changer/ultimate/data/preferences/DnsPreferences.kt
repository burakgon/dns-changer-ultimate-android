package com.dns.changer.ultimate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dns_preferences")

@Singleton
class DnsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SELECTED_DNS_ID = stringPreferencesKey("selected_dns_id")
        val IS_CONNECTED = booleanPreferencesKey("is_connected")
        val CUSTOM_DNS_LIST = stringPreferencesKey("custom_dns_list")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val HAS_WATCHED_AD = booleanPreferencesKey("has_watched_ad")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val DEBUG_SUBSCRIPTION_STATUS = stringPreferencesKey("debug_subscription_status")
        val VPN_DISCLOSURE_ACCEPTED = booleanPreferencesKey("vpn_disclosure_accepted")
    }

    val selectedDnsId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.SELECTED_DNS_ID]
    }

    val isConnected: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_CONNECTED] ?: false
    }

    val customDnsList: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.CUSTOM_DNS_LIST]
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_PREMIUM] ?: false
    }

    val hasWatchedAd: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.HAS_WATCHED_AD] ?: false
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.THEME_MODE] ?: "SYSTEM"
    }

    val startOnBoot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.START_ON_BOOT] ?: false
    }

    val debugSubscriptionStatus: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.DEBUG_SUBSCRIPTION_STATUS] ?: "ACTIVE"
    }

    val vpnDisclosureAccepted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.VPN_DISCLOSURE_ACCEPTED] ?: false
    }

    suspend fun setSelectedDnsId(id: String?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[Keys.SELECTED_DNS_ID] = id
            } else {
                preferences.remove(Keys.SELECTED_DNS_ID)
            }
        }
    }

    suspend fun setConnected(connected: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_CONNECTED] = connected
        }
    }

    suspend fun setCustomDnsList(json: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CUSTOM_DNS_LIST] = json
        }
    }

    suspend fun setPremium(premium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_PREMIUM] = premium
        }
    }

    suspend fun setHasWatchedAd(watched: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HAS_WATCHED_AD] = watched
        }
    }

    suspend fun clearAdWatchStatus() {
        context.dataStore.edit { preferences ->
            preferences[Keys.HAS_WATCHED_AD] = false
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.START_ON_BOOT] = enabled
        }
    }

    suspend fun setDebugSubscriptionStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEBUG_SUBSCRIPTION_STATUS] = status
        }
    }

    suspend fun setVpnDisclosureAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.VPN_DISCLOSURE_ACCEPTED] = accepted
        }
    }
}
