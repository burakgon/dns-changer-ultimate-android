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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.dns.changer.ultimate.ui.viewmodel.SpeedTestViewModel
import com.revenuecat.purchases.models.StoreProduct

@Composable
fun DnsNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    isPremium: Boolean,
    preferences: DnsPreferences,
    onRequestVpnPermission: (Intent) -> Unit,
    onRequestVpnPermissionWithCallback: (Intent, (Boolean) -> Unit) -> Unit,
    onShowPremiumGate: (title: String, description: String, onUnlock: () -> Unit) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    // Paywall parameters
    products: Map<String, StoreProduct> = emptyMap(),
    isLoadingPurchase: Boolean = false,
    onPurchase: (StoreProduct) -> Unit = {},
    onRestorePurchases: () -> Unit = {},
    onShowPaywall: () -> Unit = {}
) {
    // Get adaptive layout configuration for tablets/foldables
    val adaptiveConfig = rememberAdaptiveLayoutConfig()

    // Shared SpeedTestViewModel for auto-start communication
    val speedTestViewModel: SpeedTestViewModel = hiltViewModel()

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
                onShowPremiumGate = onShowPremiumGate,
                onShowPaywall = onShowPaywall,
                onNavigateToSpeedTest = {
                    speedTestViewModel.requestAutoStart()
                    navController.navigate(Screen.SpeedTest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(route = Screen.SpeedTest.route) {
            SpeedTestScreen(
                viewModel = speedTestViewModel,
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
                products = products,
                isLoadingPurchase = isLoadingPurchase,
                onPurchase = onPurchase,
                onRestorePurchases = onRestorePurchases,
                adaptiveConfig = adaptiveConfig
            )
        }
    }
}
