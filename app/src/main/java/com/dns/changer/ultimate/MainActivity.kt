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
import androidx.compose.runtime.CompositionLocalProvider
import com.dns.changer.ultimate.ads.AdMobManager
import com.dns.changer.ultimate.ads.AnalyticsEvents
import com.dns.changer.ultimate.ads.AnalyticsManager
import com.dns.changer.ultimate.ads.AnalyticsParams
import com.dns.changer.ultimate.ads.ConsentManager
import com.dns.changer.ultimate.ads.LocalAnalyticsManager
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.data.preferences.RatingPreferences
import com.dns.changer.ultimate.service.DnsSpeedTestService
import com.dns.changer.ultimate.ui.components.ConnectionSuccessOverlay
import com.dns.changer.ultimate.ui.components.DisconnectionOverlay
import com.dns.changer.ultimate.ui.components.BillingIssueDialog
import com.dns.changer.ultimate.ui.components.openSubscriptionManagement
import com.dns.changer.ultimate.ui.components.PremiumGatePopup
import com.dns.changer.ultimate.ui.components.RatingDialog
import com.dns.changer.ultimate.ui.components.DataDisclosureDialog
import com.dns.changer.ultimate.ui.components.VpnDisclosureDialog
import com.dns.changer.ultimate.data.model.SubscriptionStatus
import com.dns.changer.ultimate.ui.navigation.DnsNavHost
import com.dns.changer.ultimate.ui.navigation.Screen
import com.dns.changer.ultimate.ui.screens.paywall.PaywallScreen
import com.dns.changer.ultimate.ui.screens.settings.ThemeMode
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.dns.changer.ultimate.ui.theme.DnsChangerTheme
import com.dns.changer.ultimate.ui.theme.rememberAdaptiveLayoutConfig
import com.dns.changer.ultimate.ui.viewmodel.AppLockViewModel
import com.dns.changer.ultimate.ui.viewmodel.MainViewModel
import com.dns.changer.ultimate.ui.viewmodel.PendingAction
import com.dns.changer.ultimate.ui.viewmodel.PremiumViewModel
import com.dns.changer.ultimate.ui.viewmodel.RatingViewModel
import com.dns.changer.ultimate.ui.screens.applock.AppLockScreen
import com.dns.changer.ultimate.ui.screens.applock.PinSetupDialog
import androidx.fragment.app.FragmentActivity
import com.dns.changer.ultimate.service.DnsQuickSettingsTile
import com.dns.changer.ultimate.widget.ToggleDnsAction
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var consentManager: ConsentManager

    @Inject
    lateinit var dnsPreferences: DnsPreferences

    @Inject
    lateinit var speedTestService: DnsSpeedTestService

    @Inject
    lateinit var ratingPreferences: RatingPreferences

    private var pendingVpnPermissionCallback: ((Boolean) -> Unit)? = null

    // Widget/QS action state flow to handle both onCreate and onNewIntent
    private val _widgetAction = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    // Track the source of the action: "widget", "qs", or null (in-app)
    private val _actionSource = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

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

        // Note: GDPR consent gathering is now handled in the Composable
        // AFTER the data disclosure dialog has been accepted.
        // This ensures proper order: Data Disclosure -> UMP Consent Form

        // Check if launched from Quick Settings tile for paywall
        val showTilePaywall = intent?.getBooleanExtra(DnsQuickSettingsTile.EXTRA_SHOW_TILE_PAYWALL, false) ?: false

        // Check if launched from widget with action and set it in the StateFlow
        // Only process on fresh launch (savedInstanceState == null), not on recreation (resize, rotation)
        if (savedInstanceState == null) {
            intent?.getStringExtra(ToggleDnsAction.EXTRA_WIDGET_ACTION)?.let { action ->
                _widgetAction.value = action
                _actionSource.value = intent.getStringExtra(ToggleDnsAction.EXTRA_ACTION_SOURCE)
                    ?: ToggleDnsAction.SOURCE_WIDGET // default to widget for backwards compat
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
                CompositionLocalProvider(LocalAnalyticsManager provides analyticsManager) {
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
                    analyticsManager = analyticsManager,
                    adMobManager = adMobManager,
                    onThemeChanged = { theme ->
                        currentTheme = theme
                    },
                    showTilePaywallOnLaunch = showTilePaywall,
                    widgetActionFlow = _widgetAction,
                    onWidgetActionConsumed = { _widgetAction.value = null },
                    onSetWidgetAction = { action -> _widgetAction.value = action },
                    actionSourceFlow = _actionSource,
                    onActionSourceConsumed = { _actionSource.value = null }
                )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle widget/QS action when activity is already running
        intent.getStringExtra(ToggleDnsAction.EXTRA_WIDGET_ACTION)?.let { action ->
            _widgetAction.value = action
            _actionSource.value = intent.getStringExtra(ToggleDnsAction.EXTRA_ACTION_SOURCE)
                ?: ToggleDnsAction.SOURCE_WIDGET
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
    analyticsManager: AnalyticsManager,
    adMobManager: AdMobManager,
    onThemeChanged: (ThemeMode) -> Unit,
    showTilePaywallOnLaunch: Boolean = false,
    widgetActionFlow: kotlinx.coroutines.flow.StateFlow<String?>,
    onWidgetActionConsumed: () -> Unit,
    onSetWidgetAction: (String) -> Unit,
    actionSourceFlow: kotlinx.coroutines.flow.StateFlow<String?>,
    onActionSourceConsumed: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
    ratingViewModel: RatingViewModel = hiltViewModel(),
    appLockViewModel: AppLockViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isPremium by premiumViewModel.isPremium.collectAsState()
    val premiumState by premiumViewModel.premiumState.collectAsState()
    val products by premiumViewModel.products.collectAsState()
    val mainUiState by mainViewModel.uiState.collectAsState()
    val connectionState by mainViewModel.connectionState.collectAsState()

    // App Lock state
    val appLockUiState by appLockViewModel.uiState.collectAsState()
    val isAppLockEnabled by appLockViewModel.isAppLockEnabled.collectAsState()
    val isBiometricEnabled by appLockViewModel.isBiometricEnabled.collectAsState()
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var isChangingPin by remember { mutableStateOf(false) }
    val isBiometricAvailable = remember { appLockViewModel.isBiometricAvailable() }

    // Check if app should be locked on start
    LaunchedEffect(Unit) {
        val shouldLock = appLockViewModel.checkShouldLock()
        appLockViewModel.setLocked(shouldLock)
    }

    // Trigger biometric prompt when app is locked and biometric is available (premium only)
    LaunchedEffect(appLockUiState.isLocked, appLockUiState.isInitialized, isPremium) {
        if (appLockUiState.isLocked &&
            appLockUiState.isInitialized &&
            isPremium &&
            isBiometricAvailable &&
            isBiometricEnabled &&
            !appLockUiState.isLockout) {
            appLockViewModel.showBiometricPrompt(
                activity = activity as FragmentActivity,
                onSuccess = { appLockViewModel.onBiometricSuccess() },
                onError = { error -> appLockViewModel.onBiometricError(error) }
            )
        }
    }

    // GDPR consent state (for showing privacy options in Settings)
    val isPrivacyOptionsRequired by consentManager.isPrivacyOptionsRequired.collectAsState()
    val canRequestAds by consentManager.canRequestAds.collectAsState()

    // Data disclosure consent state (Google Play User Data policy compliance)
    // This must be shown BEFORE any data collection (Firebase Analytics, etc.)
    // Use null as initial to detect when the actual preference value has loaded
    val dataDisclosureAcceptedNullable by preferences.dataDisclosureAccepted
        .map<Boolean, Boolean?> { it }
        .collectAsState(initial = null)
    var showDataDisclosure by remember { mutableStateOf(false) }
    val dataDisclosureScope = rememberCoroutineScope()

    // Track if GDPR consent has been gathered - use ONLY the singleton state
    // This survives recomposition and subscription changes
    val isConsentGathered by consentManager.isConsentGathered.collectAsState()

    // Check if we need to show data disclosure on first launch
    // or gather GDPR consent and enable analytics if already accepted
    // IMPORTANT: Use LaunchedEffect(Unit) to run ONCE, and check values inside
    // This prevents re-triggering when subscription status changes cause recomposition
    LaunchedEffect(Unit) {
        // Wait for the actual value to load from DataStore
        val accepted = preferences.dataDisclosureAccepted.first()

        if (accepted) {
            // Data disclosure already accepted - trigger GDPR consent if not yet gathered
            // Use ONLY the singleton ConsentManager state which survives recomposition
            if (!consentManager.isConsentGathered.value) {
                consentManager.gatherConsent(activity) { _ ->
                    // Consent gathering complete - initialize AdMob if allowed
                    if (consentManager.canRequestAdsSync()) {
                        adMobManager.initialize()
                    }
                }
            }
        } else {
            // Not accepted yet - show the disclosure dialog
            showDataDisclosure = true
        }
    }

    // Enable/update analytics when consent state changes
    LaunchedEffect(dataDisclosureAcceptedNullable, canRequestAds, isConsentGathered) {
        val accepted = dataDisclosureAcceptedNullable ?: return@LaunchedEffect
        if (accepted && isConsentGathered) {
            // Both data disclosure accepted and GDPR consent gathered
            if (!analyticsManager.isAnalyticsEnabled()) {
                analyticsManager.enableAnalytics(adConsentGranted = canRequestAds)
            } else {
                // Analytics already enabled, but UMP consent may have changed
                analyticsManager.updateAdConsent(canRequestAds)
            }
        }
    }

    // VPN disclosure consent state (Google Play VpnService policy compliance)
    // The actual check is done in handleConnectWithAd using .first() to avoid race conditions
    var showVpnDisclosure by remember { mutableStateOf(false) }
    var pendingConnectionAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val disclosureScope = rememberCoroutineScope()

    // Local state to track if we're in ad flow (to hide navigation)
    var showingAdFlow by remember { mutableStateOf(false) }

    // Track if we're waiting for connection/disconnection to complete after ad
    // This prevents rapid action changes that break the overlay flow
    var awaitingActionCompletion by remember { mutableStateOf(false) }

    // Hide navigation during ad flow or connection transitions
    val hideNavigation = showingAdFlow ||
        connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Connecting ||
        connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Disconnecting ||
        connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Switching

    // Clear awaitingActionCompletion when connection reaches a stable state
    // This allows the overlay to show before enabling button again
    LaunchedEffect(connectionState, awaitingActionCompletion) {
        if (awaitingActionCompletion) {
            val isStableState = connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Connected ||
                connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Disconnected ||
                connectionState is com.dns.changer.ultimate.data.model.ConnectionState.Error
            if (isStableState) {
                // Wait for overlay to render before allowing new actions
                kotlinx.coroutines.delay(500)
                awaitingActionCompletion = false
            }
        }
    }

    // Rating state
    val showRatingDialog by ratingViewModel.showRatingDialog.collectAsState()
    val shouldRequestReview by ratingViewModel.shouldRequestReview.collectAsState()

    var showPremiumGate by remember { mutableStateOf(false) }
    var onPremiumUnlock by remember { mutableStateOf<(() -> Unit)?>(null) }
    var premiumGateTitle by remember { mutableStateOf("") }
    var premiumGateDescription by remember { mutableStateOf("") }
    var showPaywall by remember { mutableStateOf(false) }
    var showSubscriptionStatusDialog by remember { mutableStateOf(false) }

    // Helper: Show subscription status dialog instead of paywall when subscription exists but inactive
    // This prevents showing "buy" options when user just needs to fix/resume their existing subscription
    val handleShowPaywall: () -> Unit = {
        when (premiumState.subscriptionStatus) {
            SubscriptionStatus.PAUSED,
            SubscriptionStatus.BILLING_ISSUE -> {
                // Subscription exists but is inactive - show status dialog to fix it
                showSubscriptionStatusDialog = true
            }
            else -> {
                // No subscription or active/cancelled - show paywall
                showPaywall = true
            }
        }
    }

    // Show subscription status dialog for states that need user attention (app-wide)
    LaunchedEffect(premiumState.subscriptionStatus) {
        when (premiumState.subscriptionStatus) {
            SubscriptionStatus.GRACE_PERIOD,
            SubscriptionStatus.BILLING_ISSUE,
            SubscriptionStatus.PAUSED,
            SubscriptionStatus.CANCELLED -> {
                showSubscriptionStatusDialog = true
            }
            else -> {}
        }
    }

    // Auto-close paywall when subscription completes successfully
    LaunchedEffect(isPremium) {
        if (isPremium && showPaywall) {
            showPaywall = false
        }
    }

    // Track the source of the action (widget, qs, or null for in-app)
    val actionSource by actionSourceFlow.collectAsState()

    // Context for showing toasts
    val context = LocalContext.current

    // Show paywall if launched from Quick Settings tile (premium feature)
    LaunchedEffect(showTilePaywallOnLaunch) {
        if (showTilePaywallOnLaunch && !isPremium) {
            handleShowPaywall()
        }
    }

    // Coroutine scope for ad loading to ensure UI has time to update
    val adCoroutineScope = rememberCoroutineScope()

    // Inner function that performs actual connection (after disclosure accepted)
    val performConnect: () -> Unit = performConnect@{
        // Prevent rapid action changes - if we're still waiting for previous action to complete
        if (awaitingActionCompletion) {
            android.util.Log.d("MainActivity", "performConnect blocked - awaiting previous action completion")
            return@performConnect
        }

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
            awaitingActionCompletion = true
            // Clear any pending disconnect overlay to prevent inconsistent state
            mainViewModel.dismissDisconnectionOverlay()
            // Set visual "Connecting" state (doesn't actually start VPN yet)
            mainViewModel.setConnectingState()
            // Wait 1 sec so user sees "Connecting..."
            kotlinx.coroutines.delay(1000)
            // Load and show ad
            analyticsManager.logEvent(AnalyticsEvents.INTERSTITIAL_AD_SHOWN)
            onLoadInterstitialAd {
                onShowInterstitialAd {
                    // Ad dismissed - NOW actually connect
                    mainViewModel.connect()
                    // Don't set showingAdFlow = false immediately
                    // Let the LaunchedEffect handle it after state stabilizes
                    showingAdFlow = false
                }
            }
        }
    }

    // Callback: check VPN disclosure first, then connect
    // Uses coroutine with .first() to get actual DataStore value, avoiding race conditions during recomposition
    val handleConnectWithAd: () -> Unit = {
        disclosureScope.launch {
            // Get the actual value from DataStore to avoid race conditions
            val vpnAccepted = preferences.vpnDisclosureAccepted.first()
            if (!vpnAccepted) {
                // Store the action to perform after disclosure is accepted
                pendingConnectionAction = { performConnect() }
                showVpnDisclosure = true
            } else {
                // Disclosure already accepted, proceed with connection
                performConnect()
            }
        }
    }

    val handleDisconnectWithAd: () -> Unit = handleDisconnectWithAd@{
        // Prevent rapid action changes - if we're still waiting for previous action to complete
        if (awaitingActionCompletion) {
            android.util.Log.d("MainActivity", "handleDisconnectWithAd blocked - awaiting previous action completion")
            return@handleDisconnectWithAd
        }

        // Premium users: disconnect directly without ads
        if (isPremium) {
            mainViewModel.disconnect()
            return@handleDisconnectWithAd
        }

        // Non-premium users: show ad flow
        adCoroutineScope.launch {
            showingAdFlow = true
            awaitingActionCompletion = true
            // Clear any pending success overlay to prevent inconsistent state
            mainViewModel.dismissConnectionSuccessOverlay()
            // Set visual "Disconnecting" state (doesn't actually stop VPN yet)
            mainViewModel.setDisconnectingState()
            // Wait 1 sec so user sees "Disconnecting..."
            kotlinx.coroutines.delay(1000)
            // Load and show ad
            analyticsManager.logEvent(AnalyticsEvents.INTERSTITIAL_AD_SHOWN)
            onLoadInterstitialAd {
                onShowInterstitialAd {
                    // Ad dismissed - NOW actually disconnect
                    mainViewModel.disconnect()
                    // Don't set showingAdFlow = false immediately
                    // Let the LaunchedEffect handle it after state stabilizes
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

    // Handle widget/QS action by observing the StateFlow
    val widgetAction by widgetActionFlow.collectAsState()
    LaunchedEffect(widgetAction) {
        if (widgetAction != null) {
            when (widgetAction) {
                ToggleDnsAction.ACTION_CONNECT -> {
                    // Log source-specific event (skip for in-app triggers like SpeedTest → Connect)
                    when (actionSource) {
                        ToggleDnsAction.SOURCE_WIDGET -> analyticsManager.logEvent(AnalyticsEvents.WIDGET_CONNECT_TAP)
                        ToggleDnsAction.SOURCE_QS -> analyticsManager.logEvent(AnalyticsEvents.QS_CONNECT_TAP)
                    }
                    handleConnectWithAd()
                }
                ToggleDnsAction.ACTION_DISCONNECT -> {
                    when (actionSource) {
                        ToggleDnsAction.SOURCE_WIDGET -> analyticsManager.logEvent(AnalyticsEvents.WIDGET_DISCONNECT_TAP)
                        ToggleDnsAction.SOURCE_QS -> analyticsManager.logEvent(AnalyticsEvents.QS_DISCONNECT_TAP)
                    }
                    handleDisconnectWithAd()
                }
            }
            // Consume the action and source so they don't repeat
            onWidgetActionConsumed()
            onActionSourceConsumed()
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
                    handleShowPaywall()
                },
                onConnectWithAd = onConnectWithAd,
                onDisconnectWithAd = onDisconnectWithAd,
                onTriggerConnectAction = { onSetWidgetAction(ToggleDnsAction.ACTION_CONNECT) },
                subscriptionStatus = premiumState.subscriptionStatus,
                subscriptionDetails = premiumState.subscriptionDetails,
                // GDPR Privacy Options
                isPrivacyOptionsRequired = isPrivacyOptionsRequired,
                onShowPrivacyOptions = {
                    consentManager.showPrivacyOptionsForm(activity) { /* form dismissed */ }
                },
                // Error handling
                purchaseErrorMessage = premiumState.errorMessage,
                onClearPurchaseError = { premiumViewModel.clearError() },
                // App Lock
                isAppLockEnabled = isAppLockEnabled,
                isBiometricAvailable = isBiometricAvailable,
                isBiometricEnabled = isBiometricEnabled,
                onToggleAppLock = { enabled ->
                    if (!enabled) {
                        appLockViewModel.disableAppLock()
                    }
                },
                onSetupPin = {
                    isChangingPin = isAppLockEnabled
                    showPinSetupDialog = true
                },
                onToggleBiometric = { enabled ->
                    appLockViewModel.setBiometricEnabled(enabled)
                }
            )
        }

        // Hide navigation completely during transitions for ad policy compliance
        if (hideNavigation) {
            // Just show content without navigation during transitions
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        } else {
            // Get adaptive layout configuration (includes phone landscape detection)
            val adaptiveConfig = rememberAdaptiveLayoutConfig()

            // Get the default navigation suite type (NavigationBar on phones, NavigationRail on tablets)
            val defaultLayoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                androidx.compose.material3.adaptive.currentWindowAdaptiveInfo()
            )

            // Use NavigationRail when: default says so (tablets) OR phone landscape mode
            val useRail = defaultLayoutType == NavigationSuiteType.NavigationRail || adaptiveConfig.showNavigationRail

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
                                        analyticsManager.logEvent(AnalyticsEvents.TAB_SELECTED, mapOf(AnalyticsParams.TAB_NAME to screen.route))
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
                                    analyticsManager.logEvent(AnalyticsEvents.TAB_SELECTED, mapOf(AnalyticsParams.TAB_NAME to screen.route))
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
                    layoutType = defaultLayoutType,
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
        if (showPremiumGate) {
            LaunchedEffect(Unit) {
                analyticsManager.logEvent(AnalyticsEvents.PREMIUM_GATE_SHOWN, mapOf(AnalyticsParams.FEATURE_NAME to premiumGateTitle))
            }
        }
        PremiumGatePopup(
            visible = showPremiumGate,
            featureTitle = premiumGateTitle.ifEmpty { stringResource(R.string.unlock_feature) },
            featureDescription = premiumGateDescription.ifEmpty { stringResource(R.string.premium_description) },
            onDismiss = {
                analyticsManager.logEvent(AnalyticsEvents.PREMIUM_GATE_DISMISSED)
                showPremiumGate = false
            },
            onWatchAd = {
                analyticsManager.logEvent(AnalyticsEvents.PREMIUM_GATE_WATCH_AD)
                showPremiumGate = false
                val unlockCallback = onPremiumUnlock
                onPremiumUnlock = null
                analyticsManager.logEvent(AnalyticsEvents.REWARDED_AD_SHOWN)
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
                analyticsManager.logEvent(AnalyticsEvents.PREMIUM_GATE_GO_PREMIUM)
                showPremiumGate = false
                handleShowPaywall()
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

        // Log overlay visibility events
        LaunchedEffect(showSuccessOverlay) {
            if (showSuccessOverlay) {
                analyticsManager.logEvent(AnalyticsEvents.CONNECTION_OVERLAY_SHOWN)
            }
        }
        LaunchedEffect(showDisconnectOverlay) {
            if (showDisconnectOverlay) {
                analyticsManager.logEvent(AnalyticsEvents.DISCONNECTION_OVERLAY_SHOWN)
            }
        }

        // Connection Success Overlay
        ConnectionSuccessOverlay(
            visible = showSuccessOverlay,
            server = mainUiState.lastConnectedServer,
            showPremiumCard = actionSource != null && !isPremium,
            onGoPremium = { handleShowPaywall() },
            onContinue = {
                mainViewModel.dismissConnectionSuccessOverlay()
                onActionSourceConsumed()
            }
        )

        // Disconnection Overlay
        DisconnectionOverlay(
            visible = showDisconnectOverlay,
            previousServer = mainUiState.lastConnectedServer,
            showPremiumCard = actionSource != null && !isPremium,
            onGoPremium = { handleShowPaywall() },
            onContinue = {
                mainViewModel.dismissDisconnectionOverlay()
                onActionSourceConsumed()
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

        // Data Disclosure Dialog (Google Play User Data policy compliance)
        // MUST be shown at first launch BEFORE any data collection (Firebase Analytics)
        if (showDataDisclosure) {
            // Log data disclosure shown (note: analytics may not be enabled yet at first launch)
            LaunchedEffect(Unit) {
                analyticsManager.logEvent(AnalyticsEvents.DATA_DISCLOSURE_SHOWN)
            }
            DataDisclosureDialog(
                onAccept = {
                    analyticsManager.logEvent(AnalyticsEvents.DATA_DISCLOSURE_ACCEPTED)
                    // Save acceptance
                    dataDisclosureScope.launch {
                        preferences.setDataDisclosureAccepted(true)
                    }
                    showDataDisclosure = false

                    // Immediately trigger GDPR consent gathering (shows UMP form for EU users)
                    consentManager.gatherConsent(activity) { _ ->
                        // Consent gathering complete - initialize AdMob if allowed
                        if (consentManager.canRequestAdsSync()) {
                            adMobManager.initialize()
                        }
                    }
                }
            )
        }

        // VPN Disclosure Dialog (Google Play VpnService policy compliance)
        // Shown before first VPN connection to explain VpnService usage
        if (showVpnDisclosure) {
            LaunchedEffect(Unit) {
                analyticsManager.logEvent(AnalyticsEvents.VPN_DISCLOSURE_SHOWN)
            }
            VpnDisclosureDialog(
                onAccept = {
                    analyticsManager.logEvent(AnalyticsEvents.VPN_DISCLOSURE_ACCEPTED)
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

        // Subscription Status Dialog (app-wide for status alerts)
        if (showSubscriptionStatusDialog &&
            premiumState.subscriptionStatus in listOf(
                SubscriptionStatus.GRACE_PERIOD,
                SubscriptionStatus.BILLING_ISSUE,
                SubscriptionStatus.PAUSED,
                SubscriptionStatus.CANCELLED
            )) {
            LaunchedEffect(Unit) {
                analyticsManager.logEvent(AnalyticsEvents.SUBSCRIPTION_STATUS_DIALOG, mapOf(AnalyticsParams.SUBSCRIPTION_STATUS to premiumState.subscriptionStatus.name))
            }
            BillingIssueDialog(
                status = premiumState.subscriptionStatus,
                subscriptionDetails = premiumState.subscriptionDetails,
                onDismiss = { showSubscriptionStatusDialog = false },
                onManageSubscription = {
                    showSubscriptionStatusDialog = false
                    openSubscriptionManagement(context, premiumState.subscriptionDetails?.managementUrl)
                }
            )
        }

        // PIN Setup Dialog
        if (showPinSetupDialog) {
            PinSetupDialog(
                onDismiss = { showPinSetupDialog = false },
                onPinSet = { pin ->
                    appLockViewModel.setPin(pin)
                    showPinSetupDialog = false
                },
                isChangingPin = isChangingPin
            )
        }

        // App Lock Screen (shown when app is locked - premium feature only)
        if (appLockUiState.isLocked && appLockUiState.isInitialized && isAppLockEnabled && isPremium) {
            AppLockScreen(
                onPinEntered = { pin ->
                    appLockViewModel.verifyPin(pin)
                },
                onBiometricRequest = {
                    appLockViewModel.showBiometricPrompt(
                        activity = activity as FragmentActivity,
                        onSuccess = { appLockViewModel.onBiometricSuccess() },
                        onError = { error -> appLockViewModel.onBiometricError(error) }
                    )
                },
                isPinError = appLockUiState.isPinError,
                isLockout = appLockUiState.isLockout,
                lockoutRemainingSeconds = appLockUiState.lockoutRemainingSeconds,
                isBiometricAvailable = isBiometricAvailable && isBiometricEnabled,
                errorMessage = appLockUiState.errorMessage
            )
        }
    }
}
