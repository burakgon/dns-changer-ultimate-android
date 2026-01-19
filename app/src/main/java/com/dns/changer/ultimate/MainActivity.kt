package com.dns.changer.ultimate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.dns.changer.ultimate.ads.ConsentManager
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.data.preferences.RatingPreferences
import com.dns.changer.ultimate.service.DnsSpeedTestService
import com.dns.changer.ultimate.ui.components.ConnectionSuccessOverlay
import com.dns.changer.ultimate.ui.components.DisconnectionOverlay
import com.dns.changer.ultimate.ui.components.PremiumGatePopup
import com.dns.changer.ultimate.ui.components.RatingDialog
import com.dns.changer.ultimate.ui.components.VpnDisclosureDialog
import com.dns.changer.ultimate.ui.navigation.DnsNavHost
import com.dns.changer.ultimate.ui.navigation.Screen
import com.dns.changer.ultimate.ui.screens.paywall.PaywallScreen
import com.dns.changer.ultimate.ui.screens.settings.ThemeMode
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.dns.changer.ultimate.ui.theme.DnsChangerTheme
import com.dns.changer.ultimate.ui.viewmodel.MainViewModel
import com.dns.changer.ultimate.ui.viewmodel.PendingAction
import com.dns.changer.ultimate.ui.viewmodel.PremiumViewModel
import com.dns.changer.ultimate.ui.viewmodel.RatingViewModel
import com.dns.changer.ultimate.service.DnsQuickSettingsTile
import com.dns.changer.ultimate.widget.ToggleDnsAction
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var dnsPreferences: DnsPreferences

    @Inject
    lateinit var speedTestService: DnsSpeedTestService

    @Inject
    lateinit var ratingPreferences: RatingPreferences

    private var pendingVpnPermissionCallback: ((Boolean) -> Unit)? = null

    // Widget action state flow to handle both onCreate and onNewIntent
    private val _widgetAction = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    // Track if current action was triggered from widget or quick settings
    private val _launchedFromWidgetOrQS = kotlinx.coroutines.flow.MutableStateFlow(false)

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

        // Increment launch count for rating prompt using lifecycle-aware scope
        lifecycleScope.launch(Dispatchers.IO) {
            ratingPreferences.incrementLaunchCount()
        }

        // Gather GDPR consent and initialize ads
        // This shows consent form for EU users if required
        consentManager.gatherConsent(this) { formError ->
            // Consent gathering complete (form shown or not required)
            // Initialize AdMob after consent is gathered
            if (consentManager.canRequestAdsSync()) {
                adMobManager.initialize()
            }
        }

        // Also initialize ads if consent was already obtained in a previous session
        if (consentManager.canRequestAdsSync()) {
            adMobManager.initialize()
        }

        // Check if launched from Quick Settings tile for paywall
        val showTilePaywall = intent?.getBooleanExtra(DnsQuickSettingsTile.EXTRA_SHOW_TILE_PAYWALL, false) ?: false

        // Check if launched from widget with action and set it in the StateFlow
        // Only process on fresh launch (savedInstanceState == null), not on recreation (resize, rotation)
        if (savedInstanceState == null) {
            intent?.getStringExtra(ToggleDnsAction.EXTRA_WIDGET_ACTION)?.let { action ->
                _widgetAction.value = action
                _launchedFromWidgetOrQS.value = true
            }
        }

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
                    onLoadInterstitialAd = { onLoaded ->
                        adMobManager.loadInterstitialAd(onLoaded)
                    },
                    onShowInterstitialAd = { onDismissed ->
                        adMobManager.showInterstitialAd(
                            activity = this,
                            onDismissed = onDismissed
                        )
                    },
                    activity = this,
                    preferences = dnsPreferences,
                    consentManager = consentManager,
                    onThemeChanged = { theme ->
                        currentTheme = theme
                    },
                    showTilePaywallOnLaunch = showTilePaywall,
                    widgetActionFlow = _widgetAction,
                    onWidgetActionConsumed = { _widgetAction.value = null },
                    launchedFromWidgetOrQSFlow = _launchedFromWidgetOrQS,
                    onWidgetOrQSFlowConsumed = { _launchedFromWidgetOrQS.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle widget action when activity is already running
        intent.getStringExtra(ToggleDnsAction.EXTRA_WIDGET_ACTION)?.let { action ->
            _widgetAction.value = action
            _launchedFromWidgetOrQS.value = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only clear speed test when app is actually closing, not on config change (resize/fold)
        if (isFinishing) {
            speedTestService.stopAndClear()
        }
    }
}

@Composable
fun DnsChangerApp(
    onRequestVpnPermission: (Intent, (Boolean) -> Unit) -> Unit,
    onShowRewardedAd: (() -> Unit, (String) -> Unit) -> Unit,
    onLoadInterstitialAd: (onLoaded: () -> Unit) -> Unit,
    onShowInterstitialAd: (onDismissed: () -> Unit) -> Unit,
    activity: Activity,
    preferences: DnsPreferences,
    consentManager: ConsentManager,
    onThemeChanged: (ThemeMode) -> Unit,
    showTilePaywallOnLaunch: Boolean = false,
    widgetActionFlow: kotlinx.coroutines.flow.StateFlow<String?>,
    onWidgetActionConsumed: () -> Unit,
    launchedFromWidgetOrQSFlow: kotlinx.coroutines.flow.StateFlow<Boolean>,
    onWidgetOrQSFlowConsumed: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
    ratingViewModel: RatingViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isPremium by premiumViewModel.isPremium.collectAsState()
    val premiumState by premiumViewModel.premiumState.collectAsState()
    val products by premiumViewModel.products.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsState()
    val connectionState by mainViewModel.connectionState.collectAsState()

    // VPN disclosure consent state (Google Play policy compliance)
    val vpnDisclosureAccepted by preferences.vpnDisclosureAccepted.collectAsState(initial = false)
    var showVpnDisclosure by remember { mutableStateOf(false) }
    var pendingConnectionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val disclosureScope = rememberCoroutineScope()

    // GDPR consent state (for showing privacy options in Settings)
    val isPrivacyOptionsRequired by consentManager.isPrivacyOptionsRequired.collectAsState()

    // Local state to track if we're in ad flow (to hide navigation)
    var showingAdFlow by remember { mutableStateOf(false) }

    // Hide navigation during ad flow or connection transitions
    val hideNavigation = showingAdFlow ||
        connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Connecting ||
        connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Disconnecting ||
        connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Switching

    // Rating state
    val showRatingDialog by ratingViewModel.showRatingDialog.collectAsState()
    val shouldRequestReview by ratingViewModel.shouldRequestReview.collectAsState()

    var showPremiumGate by remember { mutableStateOf(false) }
    var onPremiumUnlock by remember { mutableStateOf<(() -> Unit)?>(null) }
    var premiumGateTitle by remember { mutableStateOf("") }
    var premiumGateDescription by remember { mutableStateOf("") }
    var showPaywall by remember { mutableStateOf(false) }

    // Track if action was triggered from widget/quick settings (for premium upsell)
    val launchedFromWidgetOrQS by launchedFromWidgetOrQSFlow.collectAsState()

    // Context for showing toasts
    val context = LocalContext.current

    // Show paywall if launched from Quick Settings tile (premium feature)
    LaunchedEffect(showTilePaywallOnLaunch) {
        if (showTilePaywallOnLaunch && !isPremium) {
            showPaywall = true
        }
    }

    // Coroutine scope for ad loading to ensure UI has time to update
    val adCoroutineScope = rememberCoroutineScope()

    // Inner function that performs actual connection (after disclosure accepted)
    val performConnect: () -> Unit = performConnect@{
        // Check internet connection first
        if (!mainViewModel.isInternetAvailable()) {
            Toast.makeText(context, context.getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
            return@performConnect
        }

        // Check VPN permission first
        val vpnIntent = mainViewModel.checkVpnPermission()
        if (vpnIntent != null) {
            // Need VPN permission - this will trigger the permission dialog
            mainViewModel.connect()
            return@performConnect
        }

        // Premium users: connect directly without ads
        if (isPremium) {
            mainViewModel.connect()
            return@performConnect
        }

        // Non-premium users: show ad flow
        adCoroutineScope.launch {
            showingAdFlow = true
            // Set visual "Connecting" state (doesn't actually start VPN yet)
            mainViewModel.setConnectingState()
            // Wait 1 sec so user sees "Connecting..."
            kotlinx.coroutines.delay(1000)
            // Load and show ad
            onLoadInterstitialAd {
                onShowInterstitialAd {
                    // Ad dismissed - NOW actually connect
                    mainViewModel.connect()
                    showingAdFlow = false
                }
            }
        }
    }

    // Callback: check VPN disclosure first, then connect
    val handleConnectWithAd: () -> Unit = handleConnectWithAd@{
        // Check if VPN disclosure has been accepted (Google Play policy requirement)
        if (!vpnDisclosureAccepted) {
            // Store the action to perform after disclosure is accepted
            pendingConnectionAction = { performConnect() }
            showVpnDisclosure = true
            return@handleConnectWithAd
        }

        // Disclosure already accepted, proceed with connection
        performConnect()
    }

    val handleDisconnectWithAd: () -> Unit = handleDisconnectWithAd@{
        // Premium users: disconnect directly without ads
        if (isPremium) {
            mainViewModel.disconnect()
            return@handleDisconnectWithAd
        }

        // Non-premium users: show ad flow
        adCoroutineScope.launch {
            showingAdFlow = true
            // Set visual "Disconnecting" state (doesn't actually stop VPN yet)
            mainViewModel.setDisconnectingState()
            // Wait 1 sec so user sees "Disconnecting..."
            kotlinx.coroutines.delay(1000)
            // Load and show ad
            onLoadInterstitialAd {
                onShowInterstitialAd {
                    // Ad dismissed - NOW actually disconnect
                    mainViewModel.disconnect()
                    showingAdFlow = false
                }
            }
        }
    }

    // Rating prompt is automatically checked via Flow observation in RatingViewModel

    // Handle In-App Review request
    LaunchedEffect(shouldRequestReview) {
        if (shouldRequestReview) {
            ratingViewModel.requestInAppReview(activity)
        }
    }

    // Handle VPN permission dialog
    LaunchedEffect(mainUiState.vpnPermissionIntent) {
        mainUiState.vpnPermissionIntent?.let { intent ->
            onRequestVpnPermission(intent) { granted ->
                mainViewModel.onVpnPermissionResult(granted)
            }
        }
    }

    // Handle widget action by observing the StateFlow
    val widgetAction by widgetActionFlow.collectAsState()
    LaunchedEffect(widgetAction) {
        if (widgetAction != null) {
            when (widgetAction) {
                ToggleDnsAction.ACTION_CONNECT -> {
                    handleConnectWithAd()
                }
                ToggleDnsAction.ACTION_DISCONNECT -> {
                    handleDisconnectWithAd()
                }
            }
            // Consume the action so it doesn't repeat
            onWidgetActionConsumed()
        }
    }

    // Callback for ConnectScreen - connect first, then show ad
    val onConnectWithAd: () -> Unit = { handleConnectWithAd() }
    val onDisconnectWithAd: () -> Unit = { handleDisconnectWithAd() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Common content composable to avoid duplication
        val content: @Composable () -> Unit = {
            DnsNavHost(
                navController = navController,
                innerPadding = PaddingValues(0.dp),
                isPremium = isPremium,
                preferences = preferences,
                onRequestVpnPermission = { intent ->
                    onRequestVpnPermission(intent) { granted ->
                        mainViewModel.onVpnPermissionResult(granted)
                    }
                },
                onRequestVpnPermissionWithCallback = onRequestVpnPermission,
                onShowPremiumGate = { title, description, unlockCallback ->
                    premiumGateTitle = title
                    premiumGateDescription = description
                    onPremiumUnlock = unlockCallback
                    showPremiumGate = true
                },
                onThemeChanged = onThemeChanged,
                // Paywall parameters
                products = products,
                isLoadingPurchase = premiumState.isLoading,
                onPurchase = { product ->
                    premiumViewModel.purchaseProduct(activity, product)
                },
                onRestorePurchases = {
                    premiumViewModel.restorePurchases()
                },
                onShowPaywall = {
                    showPaywall = true
                },
                onConnectWithAd = onConnectWithAd,
                onDisconnectWithAd = onDisconnectWithAd,
                subscriptionStatus = premiumState.subscriptionStatus,
                subscriptionDetails = premiumState.subscriptionDetails,
                // GDPR Privacy Options
                isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                onShowPrivacyOptions = {
                    consentManager.showPrivacyOptionsForm(activity) { /* form dismissed */ }
                },
                // Error handling
                purchaseErrorMessage = premiumState.errorMessage,
                onClearPurchaseError = { premiumViewModel.clearError() }
            )
        }

        // Hide navigation completely during transitions for ad policy compliance
        if (hideNavigation) {
            // Just show content without navigation during transitions
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        } else {
            // Get the current navigation suite type (NavigationBar on phones, NavigationRail on tablets)
            val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                androidx.compose.material3.adaptive.currentWindowAdaptiveInfo()
            )

            // Use custom layout for NavigationRail to center items vertically
            val useRail = layoutType == NavigationSuiteType.NavigationRail

            if (useRail) {
                // Custom layout with centered NavigationRail
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Screen.bottomNavItems.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                                NavigationRailItem(
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
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(screen.titleResId),
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }
                    // Content area
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        content()
                    }
                }
            } else {
                // Use default NavigationSuiteScaffold for bottom navigation
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                            item(
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
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(screen.titleResId),
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    },
                    layoutType = layoutType,
                    navigationSuiteColors = NavigationSuiteDefaults.colors(
                        navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        navigationBarContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    content()
                }
            }
        }

        // Premium Gate Popup
        PremiumGatePopup(
            visible = showPremiumGate,
            featureTitle = premiumGateTitle.ifEmpty { stringResource(R.string.unlock_feature) },
            featureDescription = premiumGateDescription.ifEmpty { stringResource(R.string.premium_description) },
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
                showPaywall = true
            }
        )

        // Overlay display logic:
        // - If showing ad flow, suppress overlays (show AFTER ad closes)
        // - If switching servers (pendingSwitchToServer != null), don't show any overlays
        // - If both overlays are flagged (error state), show neither
        // - Otherwise show the appropriate overlay
        val isSwitchingServers = mainUiState.pendingSwitchToServer != null
        val bothOverlaysSet = mainUiState.showConnectionSuccessOverlay && mainUiState.showDisconnectionOverlay
        val showSuccessOverlay = mainUiState.showConnectionSuccessOverlay && !isSwitchingServers && !bothOverlaysSet && !showingAdFlow
        val showDisconnectOverlay = mainUiState.showDisconnectionOverlay && !isSwitchingServers && !bothOverlaysSet && !showingAdFlow

        // Debug logging for overlay decisions
        if (mainUiState.showConnectionSuccessOverlay || mainUiState.showDisconnectionOverlay) {
            android.util.Log.d("MainActivity", "Overlay state: success=${mainUiState.showConnectionSuccessOverlay}, disconnect=${mainUiState.showDisconnectionOverlay}, switching=$isSwitchingServers, bothSet=$bothOverlaysSet -> showSuccess=$showSuccessOverlay, showDisconnect=$showDisconnectOverlay")
        }

        // Connection Success Overlay
        ConnectionSuccessOverlay(
            visible = showSuccessOverlay,
            server = mainUiState.lastConnectedServer,
            showPremiumCard = launchedFromWidgetOrQS && !isPremium,
            onGoPremium = { showPaywall = true },
            onContinue = {
                mainViewModel.dismissConnectionSuccessOverlay()
                onWidgetOrQSFlowConsumed()
            }
        )

        // Disconnection Overlay
        DisconnectionOverlay(
            visible = showDisconnectOverlay,
            previousServer = mainUiState.lastConnectedServer,
            showPremiumCard = launchedFromWidgetOrQS && !isPremium,
            onGoPremium = { showPaywall = true },
            onContinue = {
                mainViewModel.dismissDisconnectionOverlay()
                onWidgetOrQSFlowConsumed()
            }
        )

        // Rating Dialog
        RatingDialog(
            visible = showRatingDialog,
            onDismiss = { ratingViewModel.dismissDialog() },
            onPositive = { ratingViewModel.onPositiveResponse() },
            onNegative = { ratingViewModel.onNegativeResponse() },
            onFeedbackSubmit = { feedback ->
                ratingViewModel.onFeedbackSubmitted(feedback, activity)
            },
            onFeedbackSkip = { ratingViewModel.onFeedbackSkipped() }
        )

        // Full-screen Paywall (for premium-only features like DoH)
        if (showPaywall) {
            Dialog(
                onDismissRequest = { showPaywall = false },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                PaywallScreen(
                    products = products,
                    isLoading = premiumState.isLoading,
                    onPurchase = { product ->
                        premiumViewModel.purchaseProduct(activity, product)
                    },
                    onRestore = {
                        premiumViewModel.restorePurchases()
                    },
                    onDismiss = { showPaywall = false },
                    errorMessage = premiumState.errorMessage,
                    onClearError = { premiumViewModel.clearError() }
                )
            }
        }

        // VPN Disclosure Dialog (Google Play policy compliance)
        // Shown before first VPN connection to explain VpnService usage
        if (showVpnDisclosure) {
            VpnDisclosureDialog(
                onAccept = {
                    // Save acceptance and proceed with pending action
                    disclosureScope.launch {
                        preferences.setVpnDisclosureAccepted(true)
                    }
                    showVpnDisclosure = false
                    // Execute the pending connection action
                    pendingConnectionAction?.invoke()
                    pendingConnectionAction = null
                }
            )
        }
    }
}
