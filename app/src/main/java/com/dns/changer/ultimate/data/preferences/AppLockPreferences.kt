package com.dns.changer.ultimate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock_preferences")

enum class LockType {
    PIN,
    BIOMETRIC,
    BOTH  // Biometric with PIN fallback
}

@Singleton
class AppLockPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LOCK_TYPE = stringPreferencesKey("lock_type")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val LAST_UNLOCK_TIME = longPreferencesKey("last_unlock_time")
        val LOCK_TIMEOUT = longPreferencesKey("lock_timeout")  // in milliseconds
        val FAILED_ATTEMPTS = longPreferencesKey("failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")  // timestamp
    }

    // Default timeout: 0 = always lock when app opens, or specific durations
    companion object {
        const val TIMEOUT_ALWAYS = 0L
        const val TIMEOUT_30_SECONDS = 30_000L
        const val TIMEOUT_1_MINUTE = 60_000L
        const val TIMEOUT_5_MINUTES = 300_000L
        const val TIMEOUT_15_MINUTES = 900_000L

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION = 30_000L  // 30 seconds lockout
    }

    val isAppLockEnabled: Flow<Boolean> = context.appLockDataStore.data.map { preferences ->
        preferences[Keys.APP_LOCK_ENABLED] ?: false
    }

    val lockType: Flow<LockType> = context.appLockDataStore.data.map { preferences ->
        try {
            LockType.valueOf(preferences[Keys.LOCK_TYPE] ?: LockType.BOTH.name)
        } catch (e: Exception) {
            LockType.BOTH
        }
    }

    val isBiometricEnabled: Flow<Boolean> = context.appLockDataStore.data.map { preferences ->
        preferences[Keys.BIOMETRIC_ENABLED] ?: true
    }

    val lastUnlockTime: Flow<Long> = context.appLockDataStore.data.map { preferences ->
        preferences[Keys.LAST_UNLOCK_TIME] ?: 0L
    }

    val lockTimeout: Flow<Long> = context.appLockDataStore.data.map { preferences ->
        preferences[Keys.LOCK_TIMEOUT] ?: TIMEOUT_ALWAYS
    }

    val failedAttempts: Flow<Long> = context.appLockDataStore.data.map { preferences ->
        preferences[Keys.FAILED_ATTEMPTS] ?: 0L
    }

    val lockoutUntil: Flow<Long> = context.appLockDataStore.data.map { preferences ->
        preferences[Keys.LOCKOUT_UNTIL] ?: 0L
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLockType(type: LockType) {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.LOCK_TYPE] = type.name
        }
    }

    suspend fun setPin(pin: String) {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.PIN_HASH] = hashPin(pin)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val storedHash = context.appLockDataStore.data.first()[Keys.PIN_HASH]
        return storedHash != null && storedHash == hashPin(pin)
    }

    suspend fun hasPin(): Boolean {
        return context.appLockDataStore.data.first()[Keys.PIN_HASH] != null
    }

    suspend fun clearPin() {
        context.appLockDataStore.edit { preferences ->
            preferences.remove(Keys.PIN_HASH)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setLastUnlockTime(time: Long) {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.LAST_UNLOCK_TIME] = time
        }
    }

    suspend fun setLockTimeout(timeout: Long) {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.LOCK_TIMEOUT] = timeout
        }
    }

    suspend fun incrementFailedAttempts(): Long {
        var newCount = 0L
        context.appLockDataStore.edit { preferences ->
            val current = preferences[Keys.FAILED_ATTEMPTS] ?: 0L
            newCount = current + 1
            preferences[Keys.FAILED_ATTEMPTS] = newCount

            // Set lockout if max attempts reached
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                preferences[Keys.LOCKOUT_UNTIL] = System.currentTimeMillis() + LOCKOUT_DURATION
            }
        }
        return newCount
    }

    suspend fun resetFailedAttempts() {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.FAILED_ATTEMPTS] = 0L
            preferences[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun isLockedOut(): Boolean {
        val lockoutTime = context.appLockDataStore.data.first()[Keys.LOCKOUT_UNTIL] ?: 0L
        return lockoutTime > System.currentTimeMillis()
    }

    suspend fun getRemainingLockoutTime(): Long {
        val lockoutTime = context.appLockDataStore.data.first()[Keys.LOCKOUT_UNTIL] ?: 0L
        val remaining = lockoutTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    suspend fun shouldLock(): Boolean {
        val enabled = context.appLockDataStore.data.first()[Keys.APP_LOCK_ENABLED] ?: false
        if (!enabled) return false

        val timeout = context.appLockDataStore.data.first()[Keys.LOCK_TIMEOUT] ?: TIMEOUT_ALWAYS
        if (timeout == TIMEOUT_ALWAYS) return true

        val lastUnlock = context.appLockDataStore.data.first()[Keys.LAST_UNLOCK_TIME] ?: 0L
        return System.currentTimeMillis() - lastUnlock > timeout
    }

    suspend fun disableAppLock() {
        context.appLockDataStore.edit { preferences ->
            preferences[Keys.APP_LOCK_ENABLED] = false
            preferences.remove(Keys.PIN_HASH)
            preferences[Keys.BIOMETRIC_ENABLED] = true
            preferences[Keys.FAILED_ATTEMPTS] = 0L
            preferences[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
