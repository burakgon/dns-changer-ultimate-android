package com.dns.changer.ultimate

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dns.changer.ultimate.ads.AdMobManager
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.ui.components.ConnectionSuccessOverlay
import com.dns.changer.ultimate.ui.components.DisconnectionOverlay
import com.dns.changer.ultimate.ui.components.PremiumGatePopup
import com.dns.changer.ultimate.ui.navigation.DnsNavHost
import com.dns.changer.ultimate.ui.navigation.Screen
import com.dns.changer.ultimate.ui.screens.settings.ThemeMode
import com.dns.changer.ultimate.ui.theme.DnsChangerTheme
import com.dns.changer.ultimate.service.DnsSpeedTestService
import com.dns.changer.ultimate.ui.viewmodel.MainViewModel
import com.dns.changer.ultimate.ui.viewmodel.PremiumViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var dnsPreferences: DnsPreferences

    @Inject
    lateinit var speedTestService: DnsSpeedTestService

    private var pendingVpnPermissionCallback: ((Boolean) -> Unit)? = null

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        pendingVpnPermissionCallback?.invoke(granted)
        pendingVpnPermissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val savedTheme by dnsPreferences.themeMode.collectAsState(initial = "SYSTEM")
            var currentTheme by remember { mutableStateOf(ThemeMode.SYSTEM) }

            // Update theme when saved preference changes
            LaunchedEffect(savedTheme) {
                currentTheme = try {
                    ThemeMode.valueOf(savedTheme)
                } catch (e: Exception) {
                    ThemeMode.SYSTEM
                }
            }

            DnsChangerTheme(themeMode = currentTheme) {
                DnsChangerApp(
                    onRequestVpnPermission = { intent, callback ->
                        pendingVpnPermissionCallback = callback
                        vpnPermissionLauncher.launch(intent)
                    },
                    onShowRewardedAd = { onRewarded, onError ->
                        adMobManager.showRewardedAd(
                            activity = this,
                            onRewarded = onRewarded,
                            onError = onError
                        )
                    },
                    activity = this,
                    preferences = dnsPreferences,
                    onThemeChanged = { theme ->
                        currentTheme = theme
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop and clear speed test when app is closed
        speedTestService.stopAndClear()
    }
}

@Composable
fun DnsChangerApp(
    onRequestVpnPermission: (Intent, (Boolean) -> Unit) -> Unit,
    onShowRewardedAd: (() -> Unit, (String) -> Unit) -> Unit,
    activity: Activity,
    preferences: DnsPreferences,
    onThemeChanged: (ThemeMode) -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isPremium by premiumViewModel.isPremium.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsState()

    var showPremiumGate by remember { mutableStateOf(false) }
    var onPremiumUnlock by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Handle VPN permission dialog
    LaunchedEffect(mainUiState.vpnPermissionIntent) {
        mainUiState.vpnPermissionIntent?.let { intent ->
            onRequestVpnPermission(intent) { granted ->
                mainViewModel.onVpnPermissionResult(granted)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(80.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        val iconColor by animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(200),
                            label = "iconColor"
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = null,
                                    tint = iconColor
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(screen.titleResId),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            DnsNavHost(
                navController = navController,
                innerPadding = innerPadding,
                isPremium = isPremium,
                preferences = preferences,
                onRequestVpnPermission = { intent ->
                    onRequestVpnPermission(intent) { granted ->
                        mainViewModel.onVpnPermissionResult(granted)
                    }
                },
                onRequestVpnPermissionWithCallback = onRequestVpnPermission,
                onShowPremiumGate = { unlockCallback ->
                    onPremiumUnlock = unlockCallback
                    showPremiumGate = true
                },
                onThemeChanged = onThemeChanged
            )
        }

        // Premium Gate Popup
        PremiumGatePopup(
            visible = showPremiumGate,
            featureIcon = Icons.Default.Speed,
            featureTitle = stringResource(R.string.unlock_feature),
            featureDescription = stringResource(R.string.premium_description),
            onDismiss = { showPremiumGate = false },
            onWatchAd = {
                showPremiumGate = false
                val unlockCallback = onPremiumUnlock
                onPremiumUnlock = null
                onShowRewardedAd(
                    {
                        // Call the session-specific unlock callback
                        unlockCallback?.invoke()
                    },
                    { error ->
                        // Handle ad error - could show a toast/snackbar
                    }
                )
            },
            onGoPremium = {
                showPremiumGate = false
                premiumViewModel.purchasePremium(activity)
            }
        )

        // Overlay display logic:
        // - If switching servers (pendingSwitchToServer != null), don't show any overlays
        // - If both overlays are flagged (error state), show neither
        // - Otherwise show the appropriate overlay
        val isSwitchingServers = mainUiState.pendingSwitchToServer != null
        val bothOverlaysSet = mainUiState.showConnectionSuccessOverlay && mainUiState.showDisconnectionOverlay
        val showSuccessOverlay = mainUiState.showConnectionSuccessOverlay && !isSwitchingServers && !bothOverlaysSet
        val showDisconnectOverlay = mainUiState.showDisconnectionOverlay && !isSwitchingServers && !bothOverlaysSet

        // Debug logging for overlay decisions
        if (mainUiState.showConnectionSuccessOverlay || mainUiState.showDisconnectionOverlay) {
            android.util.Log.d("MainActivity", "Overlay state: success=${mainUiState.showConnectionSuccessOverlay}, disconnect=${mainUiState.showDisconnectionOverlay}, switching=$isSwitchingServers, bothSet=$bothOverlaysSet -> showSuccess=$showSuccessOverlay, showDisconnect=$showDisconnectOverlay")
        }

        // Connection Success Overlay
        ConnectionSuccessOverlay(
            visible = showSuccessOverlay,
            server = mainUiState.lastConnectedServer,
            onContinue = { mainViewModel.dismissConnectionSuccessOverlay() }
        )

        // Disconnection Overlay
        DisconnectionOverlay(
            visible = showDisconnectOverlay,
            previousServer = mainUiState.lastConnectedServer,
            onContinue = { mainViewModel.dismissDisconnectionOverlay() }
        )
    }
}
