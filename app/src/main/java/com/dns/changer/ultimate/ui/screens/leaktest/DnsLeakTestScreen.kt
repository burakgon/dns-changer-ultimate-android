package com.dns.changer.ultimate.ui.screens.leaktest

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Button
import androidx.hilt.navigation.compose.hiltViewModel
import com.dns.changer.ultimate.data.model.ConnectionState
import com.dns.changer.ultimate.data.model.DnsServer
import com.dns.changer.ultimate.ui.viewmodel.MainViewModel
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.ui.screens.connect.isAppInDarkTheme
import com.dns.changer.ultimate.ui.theme.AdaptiveLayoutConfig
import com.dns.changer.ultimate.ui.theme.WindowSize
import com.dns.changer.ultimate.ui.theme.rememberSemanticColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin

data class DnsLeakResult(
    val ip: String,
    val hostname: String?,
    val isp: String?,
    val country: String?,
    val countryCode: String?
)

data class DnsLeakTestResult(
    val dnsServers: List<DnsLeakResult>,
    val userPublicIp: String?
)

enum class LeakTestStatus {
    IDLE,
    RUNNING,
    COMPLETED,              // Neutral - just shows results without judgment
    COMPLETED_SECURE,       // Connected to VPN and secure
    COMPLETED_NOT_PROTECTED,
    COMPLETED_LEAK_DETECTED
}

@Composable
fun DnsLeakTestScreen(
    adaptiveConfig: AdaptiveLayoutConfig,
    viewModel: MainViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var testStatus by remember { mutableStateOf(LeakTestStatus.IDLE) }
    val results = remember { mutableStateListOf<DnsLeakResult>() }
    var userPublicIp by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }

    // Get connection state to determine if user is protected
    val connectionState by viewModel.connectionState.collectAsState()
    val isConnectedToVpn = connectionState is ConnectionState.Connected
    val connectedServer = (connectionState as? ConnectionState.Connected)?.server

    val onStartTest: () -> Unit = {
        scope.launch {
            testStatus = LeakTestStatus.RUNNING
            results.clear()
            userPublicIp = null
            progress = 0f

            val testResult = performDnsLeakTest { prog ->
                progress = prog
            }

            results.addAll(testResult.dnsServers)
            userPublicIp = testResult.userPublicIp

            // Determine status based on connection state
            // If connected to VPN, show secure. If not, show unprotected (yellow/warning)
            testStatus = if (isConnectedToVpn) {
                LeakTestStatus.COMPLETED_SECURE
            } else {
                LeakTestStatus.COMPLETED_NOT_PROTECTED  // Yellow warning - unprotected
            }
        }
    }

    // Size based on window size
    val gaugeSize = when (adaptiveConfig.windowSize) {
        WindowSize.COMPACT -> 180.dp
        WindowSize.MEDIUM -> 200.dp
        WindowSize.EXPANDED -> 220.dp
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Use different layout for expanded screens (tablets)
        if (adaptiveConfig.windowSize == WindowSize.EXPANDED) {
            // Two-pane horizontal layout for tablets
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = adaptiveConfig.horizontalPadding, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left pane: Test gauge and button
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LeakTestGauge(
                        status = testStatus,
                        progress = progress,
                        gaugeSize = gaugeSize,
                        onStartTest = onStartTest
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    StatusText(status = testStatus)

                    Spacer(modifier = Modifier.height(20.dp))

                    ActionButton(
                        status = testStatus,
                        onStartTest = onStartTest,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                }

                // Right pane: Results and info
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Results section
                    AnimatedVisibility(
                        visible = testStatus in listOf(LeakTestStatus.COMPLETED, LeakTestStatus.COMPLETED_SECURE, LeakTestStatus.COMPLETED_NOT_PROTECTED, LeakTestStatus.COMPLETED_LEAK_DETECTED),
                        enter = fadeIn() + slideInVertically { -20 }
                    ) {
                        Column {
                            ResultsSection(
                                results = results,
                                testStatus = testStatus,
                                connectedServer = connectedServer,
                                userPublicIp = userPublicIp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Info section
                    InfoSection()
                }
            }
        } else {
            // Vertical layout for compact/medium screens
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = adaptiveConfig.horizontalPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Test gauge
                LeakTestGauge(
                    status = testStatus,
                    progress = progress,
                    gaugeSize = gaugeSize,
                    onStartTest = onStartTest
                )

                Spacer(modifier = Modifier.height(20.dp))

                StatusText(status = testStatus)

                Spacer(modifier = Modifier.height(16.dp))

                // Action button
                ActionButton(
                    status = testStatus,
                    onStartTest = onStartTest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = adaptiveConfig.contentMaxWidth)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Results section
                AnimatedVisibility(
                    visible = testStatus in listOf(LeakTestStatus.COMPLETED, LeakTestStatus.COMPLETED_SECURE, LeakTestStatus.COMPLETED_NOT_PROTECTED, LeakTestStatus.COMPLETED_LEAK_DETECTED),
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) { 50 }
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = adaptiveConfig.contentMaxWidth)
                    ) {
                        ResultsSection(
                            results = results,
                            testStatus = testStatus,
                            connectedServer = connectedServer,
                            userPublicIp = userPublicIp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // Info section
                InfoSection(
                    modifier = Modifier.widthIn(max = adaptiveConfig.contentMaxWidth)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LeakTestGauge(
    status: LeakTestStatus,
    progress: Float,
    gaugeSize: Dp,
    onStartTest: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gauge")

    // Rotating outer ring animation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    // Inner ring counter rotation
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    // Pulse animation for idle state
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Orbiting particles
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val semanticColors = rememberSemanticColors()
    val successColor = semanticColors.success
    val warningColor = semanticColors.warning
    val errorColor = MaterialTheme.colorScheme.error

    val statusColor = when (status) {
        LeakTestStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        LeakTestStatus.COMPLETED_SECURE -> successColor
        LeakTestStatus.COMPLETED_NOT_PROTECTED -> warningColor
        LeakTestStatus.COMPLETED_LEAK_DETECTED -> errorColor
        else -> primaryColor
    }

    Box(
        modifier = Modifier.size(gaugeSize + 40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow background
        if (status == LeakTestStatus.IDLE || status == LeakTestStatus.RUNNING) {
            Canvas(
                modifier = Modifier
                    .size(gaugeSize + 30.dp)
                    .alpha(glowAlpha * 0.5f)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.4f),
                            tertiaryColor.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2
                )
            }
        }

        // Outer decorative ring
        Canvas(
            modifier = Modifier
                .size(gaugeSize + 20.dp)
                .rotate(if (status == LeakTestStatus.RUNNING) outerRotation else 0f)
        ) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Dashed segments
            for (i in 0 until 16) {
                val startAngle = i * 22.5f
                drawArc(
                    color = if (status == LeakTestStatus.RUNNING) primaryColor.copy(alpha = 0.6f)
                    else surfaceVariant.copy(alpha = 0.5f),
                    startAngle = startAngle,
                    sweepAngle = 15f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Middle gradient ring
        Canvas(
            modifier = Modifier
                .size(gaugeSize + 5.dp)
                .rotate(if (status == LeakTestStatus.RUNNING) innerRotation else 0f)
        ) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            val ringColor = when (status) {
                LeakTestStatus.COMPLETED_SECURE -> successColor
                LeakTestStatus.COMPLETED_LEAK_DETECTED -> errorColor
                else -> null
            }

            if (ringColor != null) {
                drawArc(
                    color = ringColor.copy(alpha = 0.4f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.8f),
                            secondaryColor.copy(alpha = 0.5f),
                            tertiaryColor.copy(alpha = 0.8f),
                            secondaryColor.copy(alpha = 0.5f),
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
        }

        // Orbiting particles (only when running)
        if (status == LeakTestStatus.RUNNING) {
            Canvas(modifier = Modifier.size(gaugeSize + 15.dp)) {
                val orbitRadius = size.minDimension / 2 - 5.dp.toPx()
                val center = Offset(size.width / 2, size.height / 2)

                val colors = listOf(primaryColor, secondaryColor, tertiaryColor)
                for (i in 0 until 3) {
                    val angle = Math.toRadians((particlePhase + i * 120).toDouble())
                    val x = center.x + orbitRadius * cos(angle).toFloat()
                    val y = center.y + orbitRadius * sin(angle).toFloat()

                    drawCircle(
                        color = colors[i],
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Progress arc (when running)
        if (status == LeakTestStatus.RUNNING && animatedProgress > 0) {
            Canvas(modifier = Modifier.size(gaugeSize - 10.dp)) {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background track
                drawArc(
                    color = surfaceVariant,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, tertiaryColor, primaryColor),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center button
        Surface(
            modifier = Modifier
                .size(gaugeSize - 30.dp)
                .scale(if (status == LeakTestStatus.IDLE) pulse else 1f),
            shape = CircleShape,
            color = when (status) {
                LeakTestStatus.COMPLETED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                LeakTestStatus.COMPLETED_SECURE -> successColor.copy(alpha = 0.12f)
                LeakTestStatus.COMPLETED_NOT_PROTECTED -> warningColor.copy(alpha = 0.12f)
                LeakTestStatus.COMPLETED_LEAK_DETECTED -> errorColor.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.primaryContainer
            },
            shadowElevation = if (status == LeakTestStatus.IDLE) 8.dp else 4.dp,
            onClick = if (status != LeakTestStatus.RUNNING) onStartTest else ({})
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = status,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f))
                            .togetherWith(fadeOut(animationSpec = tween(200)))
                    },
                    label = "centerContent"
                ) { currentStatus ->
                    when (currentStatus) {
                        LeakTestStatus.IDLE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "START",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                        LeakTestStatus.RUNNING -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                                Text(
                                    text = stringResource(R.string.testing),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LeakTestStatus.COMPLETED -> {
                            // Neutral state - just show results without judgment
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.done),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        LeakTestStatus.COMPLETED_SECURE -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = successColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.leak_test_no_leak),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = successColor
                                )
                            }
                        }
                        LeakTestStatus.COMPLETED_NOT_PROTECTED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = warningColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.leak_test_not_protected),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = warningColor
                                )
                            }
                        }
                        LeakTestStatus.COMPLETED_LEAK_DETECTED -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = errorColor,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.leak_test_leak),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = errorColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusText(status: LeakTestStatus) {
    val semanticColors = rememberSemanticColors()
    val successColor = semanticColors.success
    val warningColor = semanticColors.warning
    val errorColor = MaterialTheme.colorScheme.error

    AnimatedContent(
        targetState = status,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)).togetherWith(fadeOut(animationSpec = tween(200)))
        },
        label = "statusText"
    ) { currentStatus ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (currentStatus) {
                    LeakTestStatus.IDLE -> stringResource(R.string.leak_test_title)
                    LeakTestStatus.RUNNING -> stringResource(R.string.leak_test_running)
                    LeakTestStatus.COMPLETED -> stringResource(R.string.leak_test_dns_servers)
                    LeakTestStatus.COMPLETED_SECURE -> stringResource(R.string.leak_test_secure)
                    LeakTestStatus.COMPLETED_NOT_PROTECTED -> stringResource(R.string.leak_test_not_protected_title)
                    LeakTestStatus.COMPLETED_LEAK_DETECTED -> stringResource(R.string.leak_test_leak_detected)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when (currentStatus) {
                    LeakTestStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    LeakTestStatus.COMPLETED_SECURE -> successColor
                    LeakTestStatus.COMPLETED_NOT_PROTECTED -> warningColor
                    LeakTestStatus.COMPLETED_LEAK_DETECTED -> errorColor
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (currentStatus) {
                    LeakTestStatus.IDLE -> stringResource(R.string.leak_test_subtitle)
                    LeakTestStatus.RUNNING -> stringResource(R.string.leak_test_checking)
                    LeakTestStatus.COMPLETED -> stringResource(R.string.leak_test_servers_single_desc)
                    LeakTestStatus.COMPLETED_SECURE -> stringResource(R.string.leak_test_secure_desc)
                    LeakTestStatus.COMPLETED_NOT_PROTECTED -> stringResource(R.string.leak_test_not_protected_desc)
                    LeakTestStatus.COMPLETED_LEAK_DETECTED -> stringResource(R.string.leak_test_leak_desc)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ActionButton(
    status: LeakTestStatus,
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticColors = rememberSemanticColors()
    val successColor = semanticColors.success
    val warningColor = semanticColors.warning
    val errorColor = MaterialTheme.colorScheme.error

    Button(
        onClick = onStartTest,
        enabled = status != LeakTestStatus.RUNNING,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (status) {
                LeakTestStatus.COMPLETED_SECURE -> successColor
                LeakTestStatus.COMPLETED_NOT_PROTECTED -> warningColor
                LeakTestStatus.COMPLETED_LEAK_DETECTED -> errorColor
                else -> MaterialTheme.colorScheme.primary
            }
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        if (status == LeakTestStatus.RUNNING) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.testing),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Icon(
                imageVector = if (status == LeakTestStatus.IDLE) Icons.Rounded.PlayArrow else Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (status == LeakTestStatus.IDLE) stringResource(R.string.start_test) else stringResource(R.string.test_again),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ConnectedDnsCard(server: DnsServer) {
    val semanticColors = rememberSemanticColors()
    val successColor = semanticColors.success

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = successColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = successColor.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = successColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.leak_test_protected_by),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (server.isDoH && !server.dohUrl.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.doh_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.doh_encrypted),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "${server.primaryDns} • ${server.secondaryDns}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun YourIpCard(ip: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.leak_test_your_ip),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ip,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ResultsSection(
    results: List<DnsLeakResult>,
    testStatus: LeakTestStatus,
    connectedServer: DnsServer?,
    userPublicIp: String?
) {
    val semanticColors = rememberSemanticColors()
    val successColor = semanticColors.success
    val warningColor = semanticColors.warning
    val errorColor = MaterialTheme.colorScheme.error

    val isCompleted = testStatus == LeakTestStatus.COMPLETED
    val isSecure = testStatus == LeakTestStatus.COMPLETED_SECURE
    val isNotProtected = testStatus == LeakTestStatus.COMPLETED_NOT_PROTECTED
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Your IP Card
        if (!userPublicIp.isNullOrBlank()) {
            YourIpCard(ip = userPublicIp)
        }

        // Connected DNS Card (when secure)
        if (isSecure && connectedServer != null) {
            ConnectedDnsCard(server = connectedServer)
        }

        // Summary Card
        TestSummaryCard(
            results = results,
            testStatus = testStatus
        )

        // DNS Servers Card
        val cardColor = when {
            isCompleted -> primaryColor
            isSecure -> successColor
            isNotProtected -> warningColor
            else -> errorColor
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardColor.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = cardColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = when {
                                    isCompleted -> Icons.Filled.Dns
                                    isSecure -> Icons.Filled.CheckCircle
                                    isNotProtected -> Icons.Outlined.Info
                                    else -> Icons.Filled.Warning
                                },
                                contentDescription = null,
                                tint = cardColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.leak_test_dns_servers),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                isCompleted -> if (results.size == 1) stringResource(R.string.leak_test_servers_single_desc) else stringResource(R.string.leak_test_servers_multiple_desc)
                                isSecure -> stringResource(R.string.leak_test_servers_secure_desc)
                                isNotProtected -> if (results.size == 1) stringResource(R.string.leak_test_servers_single_desc) else stringResource(R.string.leak_test_servers_multiple_desc)
                                else -> stringResource(R.string.leak_test_servers_leak_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardColor
                    ) {
                        Text(
                            text = "${results.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Results list
                results.forEachIndexed { index, result ->
                    DnsServerResultItem(
                        result = result,
                        index = index + 1,
                        animationDelay = index * 100
                    )
                    if (index < results.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Recommendations Card
        RecommendationsCard(testStatus = testStatus)
    }
}

@Composable
private fun TestSummaryCard(
    results: List<DnsLeakResult>,
    testStatus: LeakTestStatus
) {
    val semanticColors = rememberSemanticColors()
    val successColor = semanticColors.success
    val warningColor = semanticColors.warning
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary

    val isCompleted = testStatus == LeakTestStatus.COMPLETED
    val isSecure = testStatus == LeakTestStatus.COMPLETED_SECURE
    val isNotProtected = testStatus == LeakTestStatus.COMPLETED_NOT_PROTECTED

    val statusColor = when {
        isCompleted -> primaryColor
        isSecure -> successColor
        isNotProtected -> warningColor
        else -> errorColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.leak_test_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.leak_test_status),
                    value = when {
                        isCompleted -> stringResource(R.string.leak_test_status_complete)
                        isSecure -> stringResource(R.string.leak_test_status_secure)
                        isNotProtected -> stringResource(R.string.leak_test_status_not_protected)
                        else -> stringResource(R.string.leak_test_status_leak)
                    },
                    valueColor = statusColor,
                    icon = when {
                        isCompleted -> Icons.Filled.Dns
                        isSecure -> Icons.Filled.Shield
                        isNotProtected -> Icons.Outlined.Shield
                        else -> Icons.Filled.Warning
                    }
                )

                // Servers Found
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.leak_test_servers_found),
                    value = "${results.size}",
                    valueColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Public
                )
            }


            // IP addresses list
            if (results.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.leak_test_detected_ips),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        results.forEach { result ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            color = statusColor,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = result.ip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!result.countryCode.isNullOrBlank()) {
                                    Text(
                                        text = result.countryCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subValue: String? = null,
    valueColor: Color,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = valueColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            if (!subValue.isNullOrBlank()) {
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecommendationsCard(testStatus: LeakTestStatus) {
    val semanticColors = rememberSemanticColors()
    val successColor = semanticColors.success
    val warningColor = semanticColors.warning
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary

    val isCompleted = testStatus == LeakTestStatus.COMPLETED
    val isSecure = testStatus == LeakTestStatus.COMPLETED_SECURE
    val isNotProtected = testStatus == LeakTestStatus.COMPLETED_NOT_PROTECTED

    val cardColor = when {
        isCompleted -> primaryColor
        isSecure -> successColor
        isNotProtected -> warningColor
        else -> errorColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.08f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = cardColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (isSecure) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                            contentDescription = null,
                            tint = cardColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.leak_test_recommendation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = when {
                    isCompleted -> stringResource(R.string.leak_test_recommendation_complete)
                    isSecure -> stringResource(R.string.leak_test_recommendation_secure)
                    isNotProtected -> stringResource(R.string.leak_test_recommendation_not_protected)
                    else -> stringResource(R.string.leak_test_recommendation_leak)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            if (!isSecure && !isCompleted) {
                Spacer(modifier = Modifier.height(14.dp))

                // Tips for fixing
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecommendationTip(
                        number = "1",
                        text = stringResource(R.string.leak_test_tip_1)
                    )
                    RecommendationTip(
                        number = "2",
                        text = stringResource(R.string.leak_test_tip_2)
                    )
                    RecommendationTip(
                        number = "3",
                        text = stringResource(R.string.leak_test_tip_3)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationTip(
    number: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DnsServerResultItem(
    result: DnsLeakResult,
    index: Int,
    animationDelay: Int
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Server info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.ip,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!result.isp.isNullOrBlank()) {
                    Text(
                        text = result.isp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Country badge
            if (!result.countryCode.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = result.countryCode,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.leak_test_info_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        InfoCard(
            icon = Icons.Outlined.Info,
            iconColor = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.leak_test_what_is_title),
            description = stringResource(R.string.leak_test_what_is_desc),
            animationDelay = 0
        )

        InfoCard(
            icon = Icons.Filled.VisibilityOff,
            iconColor = MaterialTheme.colorScheme.secondary,
            title = stringResource(R.string.leak_test_why_matters_title),
            description = stringResource(R.string.leak_test_why_matters_desc),
            animationDelay = 100
        )

        InfoCard(
            icon = Icons.Outlined.Shield,
            iconColor = MaterialTheme.colorScheme.tertiary,
            title = stringResource(R.string.leak_test_how_fix_title),
            description = stringResource(R.string.leak_test_how_fix_desc),
            animationDelay = 200
        )
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    animationDelay: Int
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "offsetY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(translationY = offsetY, alpha = alpha),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private suspend fun performDnsLeakTest(
    onProgress: (Float) -> Unit
): DnsLeakTestResult = withContext(Dispatchers.IO) {
    val results = mutableListOf<DnsLeakResult>()
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    onProgress(0.1f)

    // Method 1: Use whoami.akamai.net - this returns the IP of the DNS RESOLVER
    // that contacted Akamai's nameserver (the actual DNS server handling your queries)
    try {
        val testId = System.currentTimeMillis().toString(36) + (0..999999).random().toString(36)
        android.util.Log.d("DnsLeakTest", "Starting DNS leak test with ID: $testId")

        // whoami.akamai.net returns the IP of the DNS resolver that made the query
        // This is the REAL test - it shows which DNS server is actually handling your queries
        try {
            val addresses = java.net.InetAddress.getAllByName("whoami.akamai.net")
            addresses.forEach { addr ->
                val ip = addr.hostAddress ?: ""
                android.util.Log.d("DnsLeakTest", "whoami.akamai.net -> $ip (THIS IS YOUR DNS RESOLVER)")
                if (ip.isNotBlank() && results.none { it.ip == ip }) {
                    results.add(DnsLeakResult(
                        ip = ip,
                        hostname = null,
                        isp = null,
                        country = null,
                        countryCode = null
                    ))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("DnsLeakTest", "whoami.akamai.net failed: ${e.message}")
        }

        // Also try ns1.google.com which returns the resolver IP
        try {
            val addresses = java.net.InetAddress.getAllByName("ns1.google.com")
            // This just confirms DNS is working, doesn't reveal resolver
        } catch (e: Exception) {
            // Ignore
        }
    } catch (e: Exception) {
        android.util.Log.e("DnsLeakTest", "DNS resolution test failed: ${e.message}")
    }

    onProgress(0.4f)

    // Method 2: Use ipleak.net to get public IP (for reference only, not added to DNS results)
    var userPublicIp: String? = null
    try {
        val request = okhttp3.Request.Builder()
            .url("https://ipv4.ipleak.net/json/")
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                android.util.Log.d("DnsLeakTest", "ipleak.net response: $body")
                val json = JSONObject(body)
                userPublicIp = json.optString("ip", "")
                android.util.Log.d("DnsLeakTest", "User's public IP: $userPublicIp (not a DNS server)")
                // Don't add public IP to results - it's not a DNS server
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("DnsLeakTest", "ipleak.net failed: ${e.message}")
    }

    onProgress(0.6f)

    // Method 3: Check dnsleaktest.com DNS servers API
    try {
        // Generate random subdomain for leak detection
        val randomId = (100000..999999).random()
        val testDomain = "$randomId.test.dnsleaktest.com"

        android.util.Log.d("DnsLeakTest", "Testing with domain: $testDomain")

        // Resolve the test domain - this will hit dnsleaktest.com's nameserver
        try {
            java.net.InetAddress.getByName(testDomain)
        } catch (e: Exception) {
            // Expected to fail, but the DNS query was still made
        }

        // Small delay to let the test register
        kotlinx.coroutines.delay(500)

        // Now check what DNS servers were detected
        val request = okhttp3.Request.Builder()
            .url("https://bash.ws/dnsleak/test/$randomId?json")
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                android.util.Log.d("DnsLeakTest", "dnsleaktest.com response: $body")

                try {
                    val jsonArray = JSONArray(body)
                    for (i in 0 until jsonArray.length()) {
                        val server = jsonArray.getJSONObject(i)
                        val ip = server.optString("ip", "")
                        val countryName = server.optString("country_name", "")
                        val asn = server.optString("asn", "")
                        val asnName = server.optString("asn_name", "")
                        val serverType = server.optString("type", "")

                        if (ip.isNotBlank() && results.none { it.ip == ip }) {
                            results.add(DnsLeakResult(
                                ip = ip,
                                hostname = null,
                                isp = null,
                                country = countryName.ifBlank { null },
                                countryCode = null
                            ))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("DnsLeakTest", "Failed to parse dnsleaktest response: ${e.message}")
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("DnsLeakTest", "dnsleaktest.com failed: ${e.message}")
    }

    onProgress(0.85f)

    // Method 4: Direct check of current DNS configuration
    // Resolve myip.opendns.com which returns YOUR IP as seen by OpenDNS
    try {
        val addresses = java.net.InetAddress.getAllByName("myip.opendns.com")
        addresses.forEach { addr ->
            val ip = addr.hostAddress ?: ""
            android.util.Log.d("DnsLeakTest", "myip.opendns.com -> $ip (your IP seen by OpenDNS)")
            // This is your public IP, not a DNS server - skip adding to results
        }
    } catch (e: Exception) {
        android.util.Log.w("DnsLeakTest", "myip.opendns.com failed: ${e.message}")
    }

    onProgress(1.0f)

    // Filter out the user's public IP from results (it's not a DNS server)
    val filteredResults = if (userPublicIp != null) {
        results.filter { it.ip != userPublicIp }
    } else {
        results.toList()
    }

    android.util.Log.d("DnsLeakTest", "Test complete. Found ${filteredResults.size} DNS servers (filtered from ${results.size})")
    filteredResults.forEach {
        android.util.Log.d("DnsLeakTest", "  - ${it.ip} (${it.isp})")
    }

    DnsLeakTestResult(
        dnsServers = filteredResults,
        userPublicIp = userPublicIp
    )
}

/**
 * Identify if an IP belongs to a known DNS provider
 */
private fun identifyDnsProvider(ip: String): Pair<String, Boolean> {
    return when {
        // OpenDNS (main IPs and anycast resolver network)
        ip.startsWith("208.67.222.") || ip.startsWith("208.67.220.") -> "OpenDNS" to true
        ip == "208.67.222.222" || ip == "208.67.220.220" -> "OpenDNS" to true
        ip.startsWith("146.112.") -> "OpenDNS" to true  // OpenDNS resolver anycast network
        ip.startsWith("67.215.") -> "OpenDNS" to true   // OpenDNS alternate range

        // Cloudflare
        ip.startsWith("1.1.1.") || ip.startsWith("1.0.0.") -> "Cloudflare DNS" to true
        ip == "1.1.1.1" || ip == "1.0.0.1" -> "Cloudflare DNS" to true
        ip.startsWith("172.64.") || ip.startsWith("104.16.") -> "Cloudflare DNS" to true

        // Google DNS
        ip.startsWith("8.8.8.") || ip.startsWith("8.8.4.") -> "Google DNS" to true
        ip == "8.8.8.8" || ip == "8.8.4.4" -> "Google DNS" to true
        ip.startsWith("216.239.") -> "Google DNS" to true  // Google resolver network

        // Quad9
        ip.startsWith("9.9.9.") || ip.startsWith("149.112.112.") -> "Quad9 DNS" to true
        ip == "9.9.9.9" || ip == "149.112.112.112" -> "Quad9 DNS" to true

        // AdGuard DNS
        ip.startsWith("94.140.14.") || ip.startsWith("94.140.15.") -> "AdGuard DNS" to true
        ip == "94.140.14.14" || ip == "94.140.15.15" -> "AdGuard DNS" to true

        // NextDNS (main IPs and anycast network)
        ip.startsWith("45.90.28.") || ip.startsWith("45.90.30.") -> "NextDNS" to true
        ip.startsWith("45.90.") -> "NextDNS" to true  // NextDNS anycast range
        ip.startsWith("185.228.136.") || ip.startsWith("185.228.137.") -> "NextDNS" to true

        // Comodo Secure DNS
        ip.startsWith("8.26.56.") || ip.startsWith("8.20.247.") -> "Comodo DNS" to true

        // Level3/CenturyLink
        ip.startsWith("4.2.2.") -> "Level3 DNS" to true

        // Verisign
        ip.startsWith("64.6.64.") || ip.startsWith("64.6.65.") -> "Verisign DNS" to true

        // CleanBrowsing
        ip.startsWith("185.228.168.") || ip.startsWith("185.228.169.") -> "CleanBrowsing DNS" to true

        else -> "Unknown Provider" to false
    }
}

// Keep the old method signature for compatibility but it's no longer used
private suspend fun performDnsLeakTestOld(
    onProgress: (Float) -> Unit
): List<DnsLeakResult> = withContext(Dispatchers.IO) {
    val results = mutableListOf<DnsLeakResult>()
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    onProgress(0.1f)

    // Method 1: Use mullvad DNS leak test API (most reliable)
    try {
        val request = okhttp3.Request.Builder()
            .url("https://am.i.mullvad.net/json")
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)

                // Get IP info
                val ip = json.optString("ip", "")
                val city = json.optString("city", "")
                val country = json.optString("country", "")
                val org = json.optString("organization", "")
                val mullvadExit = json.optBoolean("mullvad_exit_ip", false)

                if (ip.isNotBlank()) {
                    results.add(DnsLeakResult(
                        ip = ip,
                        hostname = if (mullvadExit) "Mullvad VPN" else null,
                        isp = org.ifBlank { null },
                        country = country.ifBlank { null },
                        countryCode = null
                    ))
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("DnsLeakTest", "mullvad failed: ${e.message}")
    }

    onProgress(1.0f)
    results
}
