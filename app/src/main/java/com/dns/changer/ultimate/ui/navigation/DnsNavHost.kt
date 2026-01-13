package com.dns.changer.ultimate.ui.navigation

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.ui.screens.connect.ConnectScreen
import com.dns.changer.ultimate.ui.screens.leaktest.DnsLeakTestScreen
import com.dns.changer.ultimate.ui.screens.settings.SettingsScreen
import com.dns.changer.ultimate.ui.screens.settings.ThemeMode
import com.dns.changer.ultimate.ui.screens.speedtest.SpeedTestScreen
import com.dns.changer.ultimate.ui.theme.rememberAdaptiveLayoutConfig

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
    // Get adaptive layout configuration for tablets/foldables
    val adaptiveConfig = rememberAdaptiveLayoutConfig()

    NavHost(
        navController = navController,
        startDestination = Screen.Connect.route,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
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
                onRequestVpnPermission = onRequestVpnPermission,
                adaptiveConfig = adaptiveConfig,
                isPremium = isPremium,
                onShowPremiumGate = onShowPremiumGate
            )
        }

        composable(route = Screen.SpeedTest.route) {
            SpeedTestScreen(
                isPremium = isPremium,
                onShowPremiumGate = onShowPremiumGate,
                onRequestVpnPermission = onRequestVpnPermissionWithCallback,
                adaptiveConfig = adaptiveConfig
            )
        }

        composable(route = Screen.LeakTest.route) {
            DnsLeakTestScreen(
                adaptiveConfig = adaptiveConfig
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                onThemeChanged = onThemeChanged,
                adaptiveConfig = adaptiveConfig
            )
        }
    }
}
