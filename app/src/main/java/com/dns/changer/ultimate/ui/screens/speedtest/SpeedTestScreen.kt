package com.dns.changer.ultimate.ui.screens.speedtest

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.ads.AnalyticsEvents
import com.dns.changer.ultimate.ads.AnalyticsParams
import com.dns.changer.ultimate.ads.LocalAnalyticsManager
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.data.model.LatencyRating
import com.dns.changer.ultimate.data.model.SpeedTestResult
import com.dns.changer.ultimate.data.model.SpeedTestState
import com.dns.changer.ultimate.ui.screens.connect.CategoryColors
import com.dns.changer.ultimate.ui.screens.connect.isAppInDarkTheme
import com.dns.changer.ultimate.ui.theme.AdaptiveLayoutConfig
import com.dns.changer.ultimate.ui.theme.DnsShapes
import com.dns.changer.ultimate.ui.theme.WindowSize
import com.dns.changer.ultimate.ui.theme.isAndroidTv
import com.dns.changer.ultimate.ui.viewmodel.MainViewModel
import com.dns.changer.ultimate.ui.viewmodel.SpeedTestViewModel
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.sin

// Fixed semantic colors for latency ratings (not dynamic Material You)
private object RatingColors {
    // Light mode
    private val excellentLight = Color(0xFF2E7D32) // Green 800
    private val goodLight = Color(0xFF1565C0) // Blue 800
    private val fairLight = Color(0xFFE65100) // Deep Orange 800
    private val poorLight = Color(0xFFC62828) // Red 800

    // Dark mode (lighter for contrast)
    private val excellentDark = Color(0xFF81C784) // Green 300
    private val goodDark = Color(0xFF64B5F6) // Blue 300
    private val fairDark = Color(0xFFFFB74D) // Orange 300
    private val poorDark = Color(0xFFE57373) // Red 300

    fun forRating(rating: LatencyRating, isDarkTheme: Boolean): Color = when (rating) {
        LatencyRating.EXCELLENT -> if (isDarkTheme) excellentDark else excellentLight
        LatencyRating.GOOD -> if (isDarkTheme) goodDark else goodLight
        LatencyRating.FAIR -> if (isDarkTheme) fairDark else fairLight
        LatencyRating.POOR -> if (isDarkTheme) poorDark else poorLight
    }
}

@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    isPremium: Boolean,
    onShowPremiumGate: (title: String, description: String, onUnlock: () -> Unit) -> Unit,
    onNavigateToConnectAndStart: () -> Unit,
    adaptiveConfig: AdaptiveLayoutConfig
) {
    val analytics = LocalAnalyticsManager.current

    val speedTestState by viewModel.speedTestState.collectAsState()
    val totalServerCount by viewModel.totalServerCount.collectAsState()
    val sessionUnlocked by viewModel.resultsUnlockedForSession.collectAsState()
    val shouldAutoStart by viewModel.shouldAutoStart.collectAsState()

    val isInitialState = speedTestState.results.isEmpty() && !speedTestState.isRunning

    // Log screen view
    LaunchedEffect(Unit) {
        analytics.logScreenView("speed_test")
    }

    // Auto-start test if requested (from Find Fastest button)
    LaunchedEffect(shouldAutoStart) {
        if (shouldAutoStart && isInitialState) {
            viewModel.clearAutoStart()
            viewModel.startSpeedTest(isPremium = isPremium)
        }
    }

    // Results are unlocked if premium OR unlocked for current session (via ad watch)
    val resultsUnlocked = isPremium || sessionUnlocked

    // Premium gate strings for speed test
    val speedTestTitle = stringResource(R.string.unlock_feature)
    val speedTestDescription = stringResource(R.string.premium_description)

    // Clear ViewModel state when app is closed (activity finishing), not on config changes
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose {
            val activity = context as? Activity
            if (activity?.isFinishing == true) {
                viewModel.stopAndClearTest()
            }
        }
    }

    // Speed test always runs freely - no pre-gate
    val noInternetMessage = stringResource(R.string.no_internet_connection)
    val onStartTest = {
        if (!mainViewModel.isInternetAvailable()) {
            Toast.makeText(context, noInternetMessage, Toast.LENGTH_SHORT).show()
        } else {
            analytics.logEvent(AnalyticsEvents.SPEED_TEST_STARTED, mapOf(
                AnalyticsParams.SERVER_COUNT to totalServerCount
            ))
            viewModel.startSpeedTest(isPremium = isPremium)
        }
    }

    // Handle server selection: navigate to main screen and go through full connection flow
    val onServerSelected: (DnsServer) -> Unit = { server ->
        // Find the result to get the latency
        val result = speedTestState.results.find { it.server.id == server.id }
        analytics.logEvent(AnalyticsEvents.SPEED_TEST_APPLY_SERVER, mapOf(
            AnalyticsParams.SERVER_NAME to server.name,
            AnalyticsParams.LATENCY_MS to (result?.latencyMs ?: 0L)
        ))
        analytics.logEvent(AnalyticsEvents.SPEED_TEST_CONNECT_TAP)
        mainViewModel.selectServer(server)
        onNavigateToConnectAndStart()
    }

    // Gauge size based on window size and orientation
    val gaugeSize = when {
        adaptiveConfig.isCompactLandscape -> if (speedTestState.isRunning) 160.dp else 150.dp
        adaptiveConfig.windowSize == WindowSize.COMPACT -> if (speedTestState.isRunning) 200.dp else 180.dp
        adaptiveConfig.windowSize == WindowSize.MEDIUM -> if (speedTestState.isRunning) 220.dp else 200.dp
        else -> if (speedTestState.isRunning) 260.dp else 240.dp
    }

    // Use horizontal layout for tablets/foldables (EXPANDED) or phone landscape with results
    if (adaptiveConfig.useHorizontalLayout && !isInitialState) {
        // Two-pane layout for tablets: gauge on left, results on right
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(horizontal = adaptiveConfig.horizontalPadding, vertical = 16.dp)
        ) {
            // Left pane: Gauge and status
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpeedTestGauge(
                    progress = speedTestState.progress,
                    fastestLatency = speedTestState.fastestResult?.latencyMs,
                    isRunning = speedTestState.isRunning,
                    hasResults = speedTestState.results.isNotEmpty(),
                    currentServerName = speedTestState.currentServer?.name,
                    testedCount = speedTestState.results.size,
                    onClick = onStartTest,
                    modifier = Modifier.size(gaugeSize)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Testing status
                if (speedTestState.isRunning) {
                    Text(
                        text = "Testing ${speedTestState.results.size + 1} of ${speedTestState.totalServers} servers...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (speedTestState.results.isNotEmpty()) {
                    // Retest button with TV focus support
                    val isTv = isAndroidTv()
                    val retestInteractionSource = remember { MutableInteractionSource() }
                    val retestFocused by retestInteractionSource.collectIsFocusedAsState()
                    val retestFocusScale = if (isTv && retestFocused) 1.05f else 1f

                    Surface(
                        modifier = Modifier
                            .scale(retestFocusScale)
                            .then(
                                if (isTv && retestFocused) {
                                    Modifier
                                        .shadow(
                                            elevation = 8.dp,
                                            shape = DnsShapes.Chip,
                                            ambientColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.3f),
                                            spotColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.3f)
                                        )
                                        .border(
                                            width = 3.dp,
                                            color = MaterialTheme.colorScheme.inverseSurface,
                                            shape = DnsShapes.Chip
                                        )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = retestInteractionSource,
                                indication = LocalIndication.current,
                                onClick = onStartTest
                            ),
                        shape = DnsShapes.Chip,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.test_again),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Right pane: Results list
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(start = 24.dp)
            ) {
                // Results header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.results),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (speedTestState.results.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "${speedTestState.results.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Results list content
                SpeedTestResultsList(
                    speedTestState = speedTestState,
                    resultsUnlocked = resultsUnlocked,
                    onShowPremiumGate = onShowPremiumGate,
                    premiumGateTitle = speedTestTitle,
                    premiumGateDescription = speedTestDescription,
                    onAdWatched = { viewModel.onAdWatched() },
                    onConnectToServer = onServerSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        // Vertical layout for compact/medium screens and initial state
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(adaptiveConfig.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isInitialState) {
                // Fancy initial state
                Spacer(modifier = Modifier.weight(0.8f))

                InitialSpeedTestView(
                    onClick = onStartTest,
                    totalServerCount = totalServerCount,
                    viewSize = when (adaptiveConfig.windowSize) {
                        WindowSize.COMPACT -> 300.dp
                        WindowSize.MEDIUM -> 340.dp
                        WindowSize.EXPANDED -> 380.dp
                    }
                )

                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                SpeedTestGauge(
                    progress = speedTestState.progress,
                    fastestLatency = speedTestState.fastestResult?.latencyMs,
                    isRunning = speedTestState.isRunning,
                    hasResults = speedTestState.results.isNotEmpty(),
                    currentServerName = speedTestState.currentServer?.name,
                    testedCount = speedTestState.results.size,
                    onClick = onStartTest,
                    modifier = Modifier.size(gaugeSize)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Testing status
                if (speedTestState.isRunning) {
                    Text(
                        text = "Testing ${speedTestState.results.size + 1} of ${speedTestState.totalServers} servers...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Results List
                if (speedTestState.results.isNotEmpty() || speedTestState.isRunning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = adaptiveConfig.contentMaxWidth)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.results),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (speedTestState.results.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "${speedTestState.results.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Retest button with TV focus support
                        if (!speedTestState.isRunning && speedTestState.results.isNotEmpty()) {
                            val isTvCompact = isAndroidTv()
                            val retestInteractionSourceCompact = remember { MutableInteractionSource() }
                            val retestFocusedCompact by retestInteractionSourceCompact.collectIsFocusedAsState()
                            val retestFocusScaleCompact = if (isTvCompact && retestFocusedCompact) 1.05f else 1f

                            Surface(
                                modifier = Modifier
                                    .scale(retestFocusScaleCompact)
                                    .then(
                                        if (isTvCompact && retestFocusedCompact) {
                                            Modifier
                                                .shadow(
                                                    elevation = 8.dp,
                                                    shape = DnsShapes.Chip,
                                                    ambientColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.3f),
                                                    spotColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.3f)
                                                )
                                                .border(
                                                    width = 3.dp,
                                                    color = MaterialTheme.colorScheme.inverseSurface,
                                                    shape = DnsShapes.Chip
                                                )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        interactionSource = retestInteractionSourceCompact,
                                        indication = LocalIndication.current,
                                        onClick = onStartTest
                                    ),
                                shape = DnsShapes.Chip,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.test_again),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    SpeedTestResultsList(
                        speedTestState = speedTestState,
                        resultsUnlocked = resultsUnlocked,
                        onShowPremiumGate = onShowPremiumGate,
                        premiumGateTitle = speedTestTitle,
                        premiumGateDescription = speedTestDescription,
                        onAdWatched = { viewModel.onAdWatched() },
                        onConnectToServer = onServerSelected,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = adaptiveConfig.contentMaxWidth)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Extracted results list to be reused in both layouts
@Composable
private fun SpeedTestResultsList(
    speedTestState: SpeedTestState,
    resultsUnlocked: Boolean,
    onShowPremiumGate: (title: String, description: String, onUnlock: () -> Unit) -> Unit,
    premiumGateTitle: String,
    premiumGateDescription: String,
    onAdWatched: () -> Unit,
    onConnectToServer: (DnsServer) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    // Auto-scroll to top when results change during testing or when test completes
    // This ensures users always see the fastest (top) results
    val resultsCount = speedTestState.results.size
    var previousResultsCount by remember { mutableStateOf(0) }
    var wasRunning by remember { mutableStateOf(false) }

    LaunchedEffect(resultsCount, speedTestState.isRunning) {
        // Scroll to top when:
        // 1. New results are added during testing (results count increased)
        // 2. Test just completed (was running, now stopped)
        val resultsAdded = resultsCount > previousResultsCount
        val testJustCompleted = wasRunning && !speedTestState.isRunning

        if ((speedTestState.isRunning && resultsAdded) || testJustCompleted) {
            if (resultsCount > 0) {
                listState.animateScrollToItem(0)
            }
        }

        previousResultsCount = resultsCount
        wasRunning = speedTestState.isRunning
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Show locked top 3 overlay if not unlocked and has 3+ results (even while running)
        val showLockedOverlay = !resultsUnlocked && speedTestState.results.size >= 3

        if (showLockedOverlay) {
            // Locked Top 3 Results Card
            item(key = "locked_top_3") {
                LockedTop3ResultsCard(
                    top3Results = speedTestState.results.take(3),
                    onUnlockClick = {
                        onShowPremiumGate(premiumGateTitle, premiumGateDescription) { onAdWatched() }
                    },
                    isRunning = speedTestState.isRunning
                )
            }

            // Show results from position 4 onwards
            val remainingResults = speedTestState.results.drop(3)
            itemsIndexed(
                items = remainingResults,
                key = { _, result -> result.server.id }
            ) { index, result ->
                SpeedTestResultItem(
                    result = result,
                    rank = index + 4, // Start from rank 4
                    isFastest = false,
                    isNew = speedTestState.isRunning && index == 0,
                    onClick = { onConnectToServer(result.server) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(200, easing = FastOutSlowInEasing),
                        fadeOutSpec = tween(150),
                        placementSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = 400f
                        )
                    )
                )
            }
        } else if (resultsUnlocked) {
            // Premium/unlocked users see all results as list
            itemsIndexed(
                items = speedTestState.results,
                key = { _, result -> result.server.id }
            ) { index, result ->
                SpeedTestResultItem(
                    result = result,
                    rank = index + 1,
                    isFastest = index == 0 && !speedTestState.isRunning,
                    isNew = speedTestState.isRunning && index == 0,
                    onClick = { onConnectToServer(result.server) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(200, easing = FastOutSlowInEasing),
                        fadeOutSpec = tween(150),
                        placementSpec = spring(
                            dampingRatio = 0.9f,
                            stiffness = 400f
                        )
                    )
                )
            }
        } else {
            // Not unlocked but less than 3 results - show nothing (still collecting)
            // Or show a placeholder indicating results are being collected
            if (speedTestState.results.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = DnsShapes.Card,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Testing servers...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialSpeedTestView(
    onClick: () -> Unit,
    totalServerCount: Int,
    viewSize: Dp = 300.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "initialView")

    // Calculate relative sizes based on container size
    val containerSize = viewSize
    val glowSize = viewSize * 0.93f
    val outerRingSize = viewSize * 0.93f
    val middleRingSize = viewSize * 0.8f
    val orbitingDotsSize = viewSize * 0.87f
    val centerButtonSize = viewSize * 0.6f
    val iconSize = viewSize * 0.21f

    // Outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    // Inner ring rotation (opposite direction)
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    // Pulse for the center button
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow intensity animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Orbiting dots phase
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitPhase"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(containerSize),
            contentAlignment = Alignment.Center
        ) {
            // Background glow effect
            Box(
                modifier = Modifier
                    .size(glowSize)
                    .alpha(glowAlpha)
                    .blur(40.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.5f),
                                tertiaryColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Outer decorative ring with gradient
            Canvas(
                modifier = Modifier
                    .size(outerRingSize)
                    .rotate(outerRotation)
            ) {
                val strokeWidth = 2.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Dashed arc segments
                for (i in 0 until 12) {
                    val startAngle = i * 30f
                    drawArc(
                        color = primaryColor.copy(alpha = 0.3f),
                        startAngle = startAngle,
                        sweepAngle = 20f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Middle ring with gradient sweep - uses Material You colors
            Canvas(
                modifier = Modifier
                    .size(middleRingSize)
                    .rotate(innerRotation)
            ) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.8f),
                            secondaryColor.copy(alpha = 0.6f),
                            tertiaryColor.copy(alpha = 0.8f),
                            secondaryColor.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.8f)
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Orbiting dots - uses Material You colors
            Canvas(modifier = Modifier.size(orbitingDotsSize)) {
                val orbitRadius = size.minDimension / 2 - 20.dp.toPx()
                val center = Offset(size.width / 2, size.height / 2)

                val dotColors = listOf(primaryColor, secondaryColor, tertiaryColor)
                for (i in 0 until 3) {
                    val angle = Math.toRadians((orbitPhase + i * 120).toDouble())
                    val x = center.x + orbitRadius * cos(angle).toFloat()
                    val y = center.y + orbitRadius * sin(angle).toFloat()

                    drawCircle(
                        color = dotColors[i],
                        radius = 6.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // Main tappable button with TV focus support
            val isTv = isAndroidTv()
            val startInteractionSource = remember { MutableInteractionSource() }
            val startFocused by startInteractionSource.collectIsFocusedAsState()
            val startFocusScale = if (isTv && startFocused) 1.08f else 1f
            val onPrimaryContainerColor = MaterialTheme.colorScheme.onPrimaryContainer

            Surface(
                modifier = Modifier
                    .size(centerButtonSize)
                    .scale(pulse * startFocusScale)
                    .then(
                        if (isTv && startFocused) {
                            Modifier
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    ambientColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.4f),
                                    spotColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = 4.dp,
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    shape = CircleShape
                                )
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = startInteractionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    onPrimaryContainerColor.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = onPrimaryContainerColor,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "START",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = onPrimaryContainerColor,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "DNS Speed Test",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Find the fastest DNS for your connection",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feature pills - uses Material You colors
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeaturePill(text = "$totalServerCount Servers", color = primaryColor)
            FeaturePill(text = "Fast", color = secondaryColor)
            FeaturePill(text = "Accurate", color = tertiaryColor)
        }
    }
}

@Composable
private fun FeaturePill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SpeedTestGauge(
    progress: Float,
    fastestLatency: Long?,
    isRunning: Boolean,
    hasResults: Boolean,
    currentServerName: String?,
    testedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isAppInDarkTheme()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )

    // Rotating animation for the progress arc when running
    val infiniteTransition = rememberInfiniteTransition(label = "gauge")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation when running
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Fixed green color for fastest result (consistent with rating colors)
    val fastestColor = remember(isDarkTheme) {
        RatingColors.forRating(LatencyRating.EXCELLENT, isDarkTheme)
    }

    // Gradient colors for progress - uses Material You colors
    val gradientColors = listOf(
        primaryColor,
        secondaryColor,
        tertiaryColor,
        secondaryColor,
        primaryColor
    )

    // TV Focus support
    val isTv = isAndroidTv()
    val gaugeInteractionSource = remember { MutableInteractionSource() }
    val gaugeFocused by gaugeInteractionSource.collectIsFocusedAsState()
    val gaugeFocusScale = if (isTv && gaugeFocused && !isRunning) 1.05f else 1f
    val focusBorderColor = if (isDarkTheme) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .scale((if (isRunning) pulse else 1f) * gaugeFocusScale)
            .then(
                if (isTv && gaugeFocused && !isRunning) {
                    Modifier
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = focusBorderColor.copy(alpha = 0.4f),
                            spotColor = focusBorderColor.copy(alpha = 0.4f)
                        )
                        .border(
                            width = 4.dp,
                            color = focusBorderColor,
                            shape = CircleShape
                        )
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = gaugeInteractionSource,
                indication = LocalIndication.current,
                enabled = !isRunning,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Background arc
            drawArc(
                color = surfaceVariant,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc with gradient
            if (animatedProgress > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = gradientColors,
                        center = center
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = Triple(fastestLatency, isRunning, hasResults),
                label = "gaugeContent"
            ) { (latency, running, results) ->
                if (latency != null && !running) {
                    // Show fastest latency when done
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = latency.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = fastestColor
                        )
                        Text(
                            text = "ms",
                            style = MaterialTheme.typography.titleMedium,
                            color = onSurfaceVariant
                        )
                        Text(
                            text = "Fastest",
                            style = MaterialTheme.typography.labelSmall,
                            color = fastestColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (running) {
                    // Show percentage and current server while running
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        currentServerName?.let { name ->
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                color = onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                } else {
                    // Initial state - tap to test - big and bold
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "START TEST",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedTestResultItem(
    result: SpeedTestResult,
    rank: Int,
    isFastest: Boolean,
    isNew: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isAppInDarkTheme()
    val isTv = isAndroidTv()

    // TV Focus handling
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // High-visibility focus color
    val focusBorderColor = if (isDarkTheme) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.primary
    val focusScale = if (isTv && isFocused) 1.02f else 1f
    val cardShape = if (isFastest || (isTv && isFocused)) RoundedCornerShape(16.dp) else RoundedCornerShape(12.dp)

    // Get category color for icon
    val categoryColor = remember(result.server.category, isDarkTheme) {
        CategoryColors.forCategory(result.server.category, isDarkTheme)
    }

    // Fixed semantic rating colors (green=excellent, blue=good, orange=fair, red=poor)
    val ratingColor = remember(result.rating, isDarkTheme) {
        RatingColors.forRating(result.rating, isDarkTheme)
    }

    // Material 3 compliant list item - 72dp height for 2-line content
    // #1 gets a subtle highlight
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(focusScale)
            .then(
                if (isTv && isFocused) {
                    Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = cardShape,
                            ambientColor = focusBorderColor.copy(alpha = 0.3f),
                            spotColor = focusBorderColor.copy(alpha = 0.3f)
                        )
                        .border(
                            width = 3.dp,
                            color = focusBorderColor,
                            shape = cardShape
                        )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        shape = cardShape,
        color = when {
            isTv && isFocused -> MaterialTheme.colorScheme.surfaceContainerHighest
            isFastest -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simple rank number
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFastest) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Category Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = result.server.category.icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Server name with optional Top 3 badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = result.server.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Top 3 badge
                    if (rank <= 3) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "TOP ${rank}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Rating badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ratingColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = result.rating.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = ratingColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Latency
            Text(
                text = "${result.latencyMs} ms",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (isFastest) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LockedTop3ResultsCard(
    top3Results: List<SpeedTestResult>,
    onUnlockClick: () -> Unit,
    isRunning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isAppInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = DnsShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
        ),
        // Subtle border only in light mode
        border = if (!isDarkTheme) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Blurred result rows with animation
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(10.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Animate each position when the server at that position changes
                top3Results.forEachIndexed { index, result ->
                    AnimatedContent(
                        targetState = result,
                        transitionSpec = {
                            // New item slides in from bottom, old slides out to top
                            (slideInVertically { height -> height } + fadeIn(tween(200))) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut(tween(150)))
                        },
                        label = "blurredItem$index"
                    ) { animatedResult ->
                        val ratingColor = RatingColors.forRating(animatedResult.rating, isDarkTheme)
                        val categoryColor = CategoryColors.forCategory(animatedResult.server.category, isDarkTheme)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank number
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(28.dp)
                            )

                            // Category Icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(categoryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = animatedResult.server.category.icon,
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Server name and rating
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = animatedResult.server.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = animatedResult.rating.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = ratingColor
                                )
                            }

                            // Latency
                            Text(
                                text = "${animatedResult.latencyMs} ms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Overlay - reduced alpha to show more of the blurred content underneath
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.55f else 0.60f),
                                MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.70f else 0.75f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Lock icon
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = "Top 3 Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = "See your fastest DNS servers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Unlock button with TV focus support
                    val isTv = isAndroidTv()
                    val unlockInteractionSource = remember { MutableInteractionSource() }
                    val unlockFocused by unlockInteractionSource.collectIsFocusedAsState()
                    val unlockFocusScale = if (isTv && unlockFocused) 1.05f else 1f
                    val buttonShape = RoundedCornerShape(28.dp)
                    val focusBorderColor = MaterialTheme.colorScheme.inverseSurface

                    Button(
                        onClick = onUnlockClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = buttonShape,
                        interactionSource = unlockInteractionSource,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(52.dp)
                            .scale(unlockFocusScale)
                            .then(
                                if (isTv && unlockFocused) {
                                    Modifier
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = buttonShape,
                                            ambientColor = focusBorderColor.copy(alpha = 0.4f),
                                            spotColor = focusBorderColor.copy(alpha = 0.4f)
                                        )
                                        .border(
                                            width = 3.dp,
                                            color = focusBorderColor,
                                            shape = buttonShape
                                        )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Unlock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


