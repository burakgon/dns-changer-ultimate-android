package com.dns.changer.ultimate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.ratingDataStore: DataStore<Preferences> by preferencesDataStore(name = "rating_preferences")

@Singleton
class RatingPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val HAS_RESPONDED = booleanPreferencesKey("has_responded")
        val LIKED_APP = booleanPreferencesKey("liked_app")
        val HAS_SHOWN_NATIVE_PROMPT = booleanPreferencesKey("has_shown_native_prompt")
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
        val SUCCESSFUL_CONNECTIONS = intPreferencesKey("successful_connections")
    }

    val hasResponded: Flow<Boolean> = context.ratingDataStore.data.map { preferences ->
        preferences[Keys.HAS_RESPONDED] ?: false
    }

    val likedApp: Flow<Boolean> = context.ratingDataStore.data.map { preferences ->
        preferences[Keys.LIKED_APP] ?: false
    }

    val hasShownNativePrompt: Flow<Boolean> = context.ratingDataStore.data.map { preferences ->
        preferences[Keys.HAS_SHOWN_NATIVE_PROMPT] ?: false
    }

    val launchCount: Flow<Int> = context.ratingDataStore.data.map { preferences ->
        preferences[Keys.LAUNCH_COUNT] ?: 0
    }

    val successfulConnections: Flow<Int> = context.ratingDataStore.data.map { preferences ->
        preferences[Keys.SUCCESSFUL_CONNECTIONS] ?: 0
    }

    suspend fun setHasResponded(responded: Boolean) {
        context.ratingDataStore.edit { preferences ->
            preferences[Keys.HAS_RESPONDED] = responded
        }
    }

    suspend fun setLikedApp(liked: Boolean) {
        context.ratingDataStore.edit { preferences ->
            preferences[Keys.LIKED_APP] = liked
        }
    }

    suspend fun setHasShownNativePrompt(shown: Boolean) {
        context.ratingDataStore.edit { preferences ->
            preferences[Keys.HAS_SHOWN_NATIVE_PROMPT] = shown
        }
    }

    suspend fun incrementLaunchCount() {
        context.ratingDataStore.edit { preferences ->
            val current = preferences[Keys.LAUNCH_COUNT] ?: 0
            preferences[Keys.LAUNCH_COUNT] = current + 1
        }
    }

    suspend fun incrementSuccessfulConnections() {
        context.ratingDataStore.edit { preferences ->
            val current = preferences[Keys.SUCCESSFUL_CONNECTIONS] ?: 0
            preferences[Keys.SUCCESSFUL_CONNECTIONS] = current + 1
        }
    }

    suspend fun shouldShowPrompt(): Boolean {
        val hasResponded = hasResponded.first()
        if (hasResponded) return false

        val launchCount = launchCount.first()
        val connections = successfulConnections.first()

        // Show on 2nd launch OR after 2 successful connections
        return launchCount >= 2 || connections >= 2
    }
}
