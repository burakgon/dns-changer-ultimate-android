package com.dns.changer.ultimate.ui.navigation

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.ui.screens.connect.ConnectScreen
import com.dns.changer.ultimate.ui.screens.settings.SettingsScreen
import com.dns.changer.ultimate.ui.screens.settings.ThemeMode
import com.dns.changer.ultimate.ui.screens.speedtest.SpeedTestScreen

@Composable
fun DnsNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    isPremium: Boolean,
    preferences: DnsPreferences,
    onRequestVpnPermission: (Intent) -> Unit,
    onRequestVpnPermissionWithCallback: (Intent, (Boolean) -> Unit) -> Unit,
    onShowPremiumGate: (onUnlock: () -> Unit) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Connect.route,
        modifier = Modifier.padding(innerPadding),
        enterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(route = Screen.Connect.route) {
            ConnectScreen(
                onRequestVpnPermission = onRequestVpnPermission
            )
        }

        composable(route = Screen.SpeedTest.route) {
            SpeedTestScreen(
                isPremium = isPremium,
                onShowPremiumGate = onShowPremiumGate,
                onRequestVpnPermission = onRequestVpnPermissionWithCallback
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                onThemeChanged = onThemeChanged
            )
        }
    }
}
