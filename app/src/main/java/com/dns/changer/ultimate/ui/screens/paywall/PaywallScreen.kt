package com.dns.changer.ultimate.ui.screens.paywall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Https
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import com.revenuecat.purchases.models.StoreProduct
import com.dns.changer.ultimate.ui.viewmodel.PremiumViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class SubscriptionPlan {
    MONTHLY,
    YEARLY,
    YEARLY_TRIAL
}

data class PlanDetails(
    val id: SubscriptionPlan,
    val title: String,
    val period: String,
    val price: String,
    val originalPrice: String?,
    val pricePerMonth: String?,
    val savings: Int?,
    val isBestValue: Boolean = false,
    val hasFreeTrial: Boolean = false,
    val trialDays: Int = 0,
    val storeProduct: StoreProduct? = null
)

// Premium colors - theme-aware
private object PremiumColors {
    // Gold palette
    val GoldLight = Color(0xFFFFD54F)
    val GoldPrimary = Color(0xFFFFB300)
    val GoldDeep = Color(0xFFF57C00)
    val GoldDark = Color(0xFFE65100)

    // Button colors
    val ButtonStart = Color(0xFFFF8F00)
    val ButtonEnd = Color(0xFFE65100)

    // Accent colors
    val SuccessGreen = Color(0xFF00C853)
    val SuccessGreenLight = Color(0xFF4CAF50)
    val PremiumPurple = Color(0xFF7C4DFF)
    val PremiumPurpleLight = Color(0xFF9575CD)
    val PremiumCyan = Color(0xFF00BCD4)
    val PremiumCyanLight = Color(0xFF4DD0E1)

    // Light mode specific - darker/more saturated for contrast
    val GoldLightMode = Color(0xFFFF9800)
    val GoldDeepLightMode = Color(0xFFE65100)
}

// Screen size classification for adaptive layouts
private enum class ScreenSizeClass {
    COMPACT,    // Phone portrait, folded foldable
    MEDIUM,     // Phone landscape, small tablet
    EXPANDED    // Tablet, unfolded foldable
}

@Composable
fun PaywallScreen(
    products: Map<String, StoreProduct>,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.YEARLY_TRIAL) }
    var animationStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(100)
        animationStep = 1
        delay(200)
        animationStep = 2
        delay(150)
        animationStep = 3
        delay(200)
        animationStep = 4
    }

    val plans = remember(products) { buildPlansList(products) }
    val selectedPlanDetails = plans.find { it.id == selectedPlan }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumBackground()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val isLandscape = screenWidth > screenHeight

            // Determine screen size class
            val sizeClass = when {
                screenWidth < 600.dp -> ScreenSizeClass.COMPACT
                screenWidth < 840.dp -> ScreenSizeClass.MEDIUM
                else -> ScreenSizeClass.EXPANDED
            }

            // Use different layouts based on screen size and orientation
            when {
                // Landscape or wide screen - use horizontal layout
                isLandscape || sizeClass == ScreenSizeClass.EXPANDED -> {
                    LandscapePaywallLayout(
                        animationStep = animationStep,
                        plans = plans,
                        selectedPlan = selectedPlan,
                        onPlanSelected = { selectedPlan = it },
                        selectedPlanDetails = selectedPlanDetails,
                        isLoading = isLoading,
                        onPurchase = onPurchase,
                        onRestore = onRestore,
                        onDismiss = onDismiss,
                        isCompactHeight = screenHeight < 500.dp
                    )
                }
                // Portrait - use vertical scrollable layout
                else -> {
                    PortraitPaywallLayout(
                        animationStep = animationStep,
                        plans = plans,
                        selectedPlan = selectedPlan,
                        onPlanSelected = { selectedPlan = it },
                        selectedPlanDetails = selectedPlanDetails,
                        isLoading = isLoading,
                        onPurchase = onPurchase,
                        onRestore = onRestore,
                        onDismiss = onDismiss,
                        isCompactHeight = screenHeight < 700.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitPaywallLayout(
    animationStep: Int,
    plans: List<PlanDetails>,
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    selectedPlanDetails: PlanDetails?,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    isCompactHeight: Boolean
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Close button - always visible at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .alpha(0.7f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // HERO
            AnimatedVisibility(
                visible = animationStep >= 1,
                enter = fadeIn(tween(500)) + scaleIn(tween(600, easing = EaseOutBack), initialScale = 0.6f)
            ) {
                HeroSection(isCompact = isCompactHeight)
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

            // SOCIAL PROOF
            AnimatedVisibility(
                visible = animationStep >= 2,
                enter = fadeIn(tween(400, delayMillis = 100)) +
                        slideInVertically(tween(400)) { it / 3 }
            ) {
                SocialProofBanner()
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 16.dp))

            // FEATURES
            AnimatedVisibility(
                visible = animationStep >= 3,
                enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 2 }
            ) {
                BenefitsSection(isCompact = isCompactHeight)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // BOTTOM SECTION - Fixed at bottom
        AnimatedVisibility(
            visible = animationStep >= 4,
            enter = slideInVertically(tween(500, easing = EaseOutCubic)) { it }
        ) {
            BottomPurchaseSection(
                plans = plans,
                selectedPlan = selectedPlan,
                onPlanSelected = onPlanSelected,
                selectedPlanDetails = selectedPlanDetails,
                isLoading = isLoading,
                onPurchase = onPurchase,
                onRestore = onRestore,
                isCompact = isCompactHeight
            )
        }
    }
}

@Composable
private fun LandscapePaywallLayout(
    animationStep: Int,
    plans: List<PlanDetails>,
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    selectedPlanDetails: PlanDetails?,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    isCompactHeight: Boolean
) {
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            // Left side - Hero + Benefits (scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(leftScrollState)
                    .padding(start = 16.dp, end = 8.dp, top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HERO - Smaller in landscape
                AnimatedVisibility(
                    visible = animationStep >= 1,
                    enter = fadeIn(tween(500)) + scaleIn(tween(600, easing = EaseOutBack), initialScale = 0.6f)
                ) {
                    HeroSection(isCompact = true)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SOCIAL PROOF
                AnimatedVisibility(
                    visible = animationStep >= 2,
                    enter = fadeIn(tween(400, delayMillis = 100)) +
                            slideInVertically(tween(400)) { it / 3 }
                ) {
                    SocialProofBannerCompact()
                }

                Spacer(modifier = Modifier.height(12.dp))

                // FEATURES - Horizontal layout for landscape
                AnimatedVisibility(
                    visible = animationStep >= 3,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 2 }
                ) {
                    BenefitsSectionLandscape()
                }
            }

            // Right side - Plans + CTA (scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rightScrollState)
                    .padding(start = 8.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = animationStep >= 4,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(500)) { it / 2 }
                ) {
                    LandscapePurchaseSection(
                        plans = plans,
                        selectedPlan = selectedPlan,
                        onPlanSelected = onPlanSelected,
                        selectedPlanDetails = selectedPlanDetails,
                        isLoading = isLoading,
                        onPurchase = onPurchase,
                        onRestore = onRestore,
                        isCompactHeight = isCompactHeight
                    )
                }
            }
        }

        // Close button - top right (placed after Row so it's on top and receives touches)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .alpha(0.7f)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumBackground() {
    // Use background luminance to detect actual theme (works with manual switch)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "o1"
    )
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "o2"
    )

    // Adjust alpha values for light/dark mode
    val goldAlpha = if (isDark) 0.18f else 0.12f
    val goldSecondaryAlpha = if (isDark) 0.05f else 0.04f
    val purpleAlpha = if (isDark) 0.1f else 0.08f
    val bottomGlowAlpha = if (isDark) 0.1f else 0.06f

    // Use theme-appropriate colors
    val goldColor = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val goldDeepColor = if (isDark) PremiumColors.GoldDeep else PremiumColors.GoldDeepLightMode
    val purpleColor = if (isDark) PremiumColors.PremiumPurple else PremiumColors.PremiumPurpleLight

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Top golden glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    goldColor.copy(alpha = goldAlpha),
                    goldDeepColor.copy(alpha = goldSecondaryAlpha),
                    Color.Transparent
                ),
                center = Offset(size.width * (0.2f + offset1 * 0.3f), size.height * 0.06f),
                radius = size.width * 0.8f
            ),
            center = Offset(size.width * (0.2f + offset1 * 0.3f), size.height * 0.06f),
            radius = size.width * 0.8f
        )
        // Purple accent
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(purpleColor.copy(alpha = purpleAlpha), Color.Transparent),
                center = Offset(size.width * (0.85f - offset2 * 0.15f), size.height * 0.25f),
                radius = size.width * 0.35f
            ),
            center = Offset(size.width * (0.85f - offset2 * 0.15f), size.height * 0.25f),
            radius = size.width * 0.35f
        )
        // Bottom warm glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(goldDeepColor.copy(alpha = bottomGlowAlpha), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.98f),
                radius = size.width * 0.7f
            ),
            center = Offset(size.width * 0.5f, size.height * 0.98f),
            radius = size.width * 0.7f
        )
    }
}

@Composable
private fun HeroSection(isCompact: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Epic animated badge - smaller for compact
        EpicPremiumBadge(isCompact = isCompact)

        Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 16.dp))

        Text(
            text = "Unlock Premium",
            style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ultimate DNS Protection",
            style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EpicPremiumBadge(isCompact: Boolean = false) {
    // Use background luminance to detect actual theme (works with manual switch)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "epic")

    // Scale factor for compact mode
    val scaleFactor = if (isCompact) 0.7f else 1f
    val badgeSize = if (isCompact) 98.dp else 140.dp

    // Multiple rotation speeds for layered effect
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot1"
    )
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot2"
    )
    val rotation3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot3"
    )

    // Pulsing effects
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )
    val innerPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "innerPulse"
    )

    // Glow intensity - stronger in dark mode
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = if (isDark) 0.4f else 0.25f,
        targetValue = if (isDark) 0.9f else 0.5f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )

    // Particle orbit
    val particleAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "particle"
    )

    // Crown float animation
    val crownFloat by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "float"
    )

    // Sparkle animations
    val sparkle1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "sparkle1"
    )
    val sparkle2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "sparkle2"
    )
    val sparkle3 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse),
        label = "sparkle3"
    )

    // Shimmer for crown
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    // Theme-aware colors
    val goldLight = PremiumColors.GoldLight
    val goldPrimary = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val goldDeep = if (isDark) PremiumColors.GoldDeep else PremiumColors.GoldDeepLightMode
    val goldDark = PremiumColors.GoldDark
    val premiumCyan = if (isDark) PremiumColors.PremiumCyan else PremiumColors.PremiumCyanLight
    val premiumPurple = if (isDark) PremiumColors.PremiumPurple else PremiumColors.PremiumPurpleLight

    // Jewel colors - slightly adjusted for light mode contrast
    val RubyRed = if (isDark) Color(0xFFE91E63) else Color(0xFFD81B60)
    val SapphireBlue = if (isDark) Color(0xFF2196F3) else Color(0xFF1976D2)
    val EmeraldGreen = if (isDark) Color(0xFF4CAF50) else Color(0xFF388E3C)

    // Sparkle color for light mode
    val sparkleColor = if (isDark) Color.White else goldPrimary

    Box(
        modifier = Modifier.size(badgeSize),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Outer pulsing glow
        Canvas(modifier = Modifier.size(badgeSize).scale(pulse)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldPrimary.copy(alpha = glowIntensity * 0.5f),
                        goldDeep.copy(alpha = glowIntensity * 0.25f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2
            )
        }

        // Layer 2: Outer spinning dashed ring
        Canvas(modifier = Modifier.size(badgeSize * 0.89f).rotate(rotation1)) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val ringAlpha = if (isDark) 0.6f else 0.8f
            for (i in 0 until 12) {
                val startAngle = i * 30f
                drawArc(
                    color = goldLight.copy(alpha = ringAlpha),
                    startAngle = startAngle,
                    sweepAngle = 15f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }

        // Layer 3: Middle gradient ring (opposite direction)
        Canvas(modifier = Modifier.size(badgeSize * 0.79f).rotate(rotation2)) {
            val ringAlpha = if (isDark) 0.8f else 0.9f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        premiumCyan.copy(alpha = ringAlpha),
                        premiumPurple.copy(alpha = ringAlpha),
                        goldPrimary.copy(alpha = ringAlpha),
                        Color.Transparent
                    )
                ),
                startAngle = 0f,
                sweepAngle = 280f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Layer 4: Inner fast ring
        Canvas(modifier = Modifier.size(badgeSize * 0.68f).rotate(rotation3)) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        goldLight,
                        goldPrimary,
                        goldDeep,
                        Color.Transparent
                    )
                ),
                startAngle = 0f,
                sweepAngle = 200f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Layer 5: Orbiting particles
        Canvas(modifier = Modifier.size(badgeSize * 0.86f)) {
            val center = Offset(size.width / 2, size.height / 2)
            val orbitRadius = size.width / 2 - 8.dp.toPx()
            val particleCore = if (isDark) Color.White else goldLight

            for (i in 0 until 3) {
                val angle = Math.toRadians((particleAngle + i * 120).toDouble())
                val x = center.x + orbitRadius * cos(angle).toFloat()
                val y = center.y + orbitRadius * sin(angle).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(particleCore, goldLight.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(x, y),
                        radius = 6.dp.toPx()
                    ),
                    center = Offset(x, y),
                    radius = 4.dp.toPx()
                )
            }
        }

        // Layer 6: Epic Crown with floating animation
        Canvas(
            modifier = Modifier
                .size(badgeSize * 0.51f)
                .scale(innerPulse)
                .graphicsLayer { translationY = crownFloat * scaleFactor }
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2

            // Crown base measurements
            val crownBottom = h * 0.85f
            val crownTop = h * 0.15f
            val baseHeight = h * 0.12f
            val crownWidth = w * 0.9f
            val leftEdge = (w - crownWidth) / 2
            val rightEdge = leftEdge + crownWidth

            // Draw crown glow/shadow - adjusted for light mode
            val glowAlpha = if (isDark) 0.4f else 0.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldPrimary.copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(centerX, h * 0.5f),
                    radius = w * 0.6f
                ),
                center = Offset(centerX, h * 0.5f),
                radius = w * 0.6f
            )

            // Crown path
            val crownPath = androidx.compose.ui.graphics.Path().apply {
                // Start from bottom left
                moveTo(leftEdge, crownBottom)

                // Left edge up
                lineTo(leftEdge, crownBottom - baseHeight)

                // Crown body with 5 points
                val pointWidth = crownWidth / 5

                // First valley (left)
                lineTo(leftEdge + pointWidth * 0.5f, h * 0.55f)
                // First peak (left)
                lineTo(leftEdge + pointWidth * 1f, crownTop + h * 0.15f)
                // Second valley
                lineTo(leftEdge + pointWidth * 1.5f, h * 0.45f)
                // Second peak (left-center)
                lineTo(leftEdge + pointWidth * 2f, crownTop + h * 0.08f)
                // Middle valley
                lineTo(centerX, h * 0.38f)
                // Center peak (tallest)
                lineTo(centerX, crownTop)
                // Middle valley (right side)
                lineTo(centerX, h * 0.38f)
                // Third peak (right-center)
                lineTo(leftEdge + pointWidth * 3f, crownTop + h * 0.08f)
                // Third valley
                lineTo(leftEdge + pointWidth * 3.5f, h * 0.45f)
                // Fourth peak (right)
                lineTo(leftEdge + pointWidth * 4f, crownTop + h * 0.15f)
                // Fourth valley (right)
                lineTo(leftEdge + pointWidth * 4.5f, h * 0.55f)

                // Right edge down
                lineTo(rightEdge, crownBottom - baseHeight)
                lineTo(rightEdge, crownBottom)

                // Bottom edge
                close()
            }

            // Draw crown shadow
            drawPath(
                path = crownPath,
                brush = Brush.verticalGradient(
                    colors = listOf(goldDark.copy(alpha = 0.5f), Color.Transparent),
                    startY = crownBottom,
                    endY = crownBottom + 8.dp.toPx()
                )
            )

            // Draw crown body with rich gradient
            drawPath(
                path = crownPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        goldLight,
                        goldPrimary,
                        goldDeep,
                        goldDark,
                        goldDeep
                    ),
                    start = Offset(leftEdge, crownTop),
                    end = Offset(rightEdge, crownBottom)
                )
            )

            // Crown inner shine
            drawPath(
                path = crownPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset(leftEdge, crownTop),
                    end = Offset(centerX, h * 0.6f)
                )
            )

            // Shimmer effect across crown
            val shimmerStart = w * shimmer
            val shimmerWidth = w * 0.3f
            drawPath(
                path = crownPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    start = Offset(shimmerStart - shimmerWidth, 0f),
                    end = Offset(shimmerStart + shimmerWidth, h)
                )
            )

            // Crown base band (darker)
            val bandTop = crownBottom - baseHeight
            val bandPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(leftEdge, crownBottom)
                lineTo(leftEdge, bandTop)
                lineTo(rightEdge, bandTop)
                lineTo(rightEdge, crownBottom)
                close()
            }
            drawPath(
                path = bandPath,
                brush = Brush.verticalGradient(
                    colors = listOf(goldDeep, goldDark),
                    startY = bandTop,
                    endY = crownBottom
                )
            )

            // Center jewel (large ruby)
            val centerJewelY = h * 0.52f
            val jewelRadius = 6.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        RubyRed,
                        RubyRed.copy(alpha = 0.8f)
                    ),
                    center = Offset(centerX - jewelRadius * 0.3f, centerJewelY - jewelRadius * 0.3f),
                    radius = jewelRadius
                ),
                center = Offset(centerX, centerJewelY),
                radius = jewelRadius
            )
            // Jewel highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                center = Offset(centerX - jewelRadius * 0.3f, centerJewelY - jewelRadius * 0.3f),
                radius = jewelRadius * 0.25f
            )

            // Left jewel (sapphire)
            val leftJewelX = leftEdge + crownWidth * 0.22f
            val leftJewelY = h * 0.58f
            val smallJewelRadius = 4.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        SapphireBlue,
                        SapphireBlue.copy(alpha = 0.8f)
                    ),
                    center = Offset(leftJewelX - smallJewelRadius * 0.3f, leftJewelY - smallJewelRadius * 0.3f),
                    radius = smallJewelRadius
                ),
                center = Offset(leftJewelX, leftJewelY),
                radius = smallJewelRadius
            )

            // Right jewel (emerald)
            val rightJewelX = leftEdge + crownWidth * 0.78f
            val rightJewelY = h * 0.58f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        EmeraldGreen,
                        EmeraldGreen.copy(alpha = 0.8f)
                    ),
                    center = Offset(rightJewelX - smallJewelRadius * 0.3f, rightJewelY - smallJewelRadius * 0.3f),
                    radius = smallJewelRadius
                ),
                center = Offset(rightJewelX, rightJewelY),
                radius = smallJewelRadius
            )

            // Top crown tips - small gold balls
            val tipRadius = 3.dp.toPx()
            val tipPositions = listOf(
                Offset(leftEdge + crownWidth / 5, crownTop + h * 0.15f),
                Offset(leftEdge + crownWidth * 2 / 5, crownTop + h * 0.08f),
                Offset(centerX, crownTop),
                Offset(leftEdge + crownWidth * 3 / 5, crownTop + h * 0.08f),
                Offset(leftEdge + crownWidth * 4 / 5, crownTop + h * 0.15f)
            )
            tipPositions.forEach { pos ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(goldLight, goldPrimary, goldDeep),
                        center = Offset(pos.x - tipRadius * 0.3f, pos.y - tipRadius * 0.3f),
                        radius = tipRadius
                    ),
                    center = pos,
                    radius = tipRadius
                )
                // Tiny highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    center = Offset(pos.x - tipRadius * 0.3f, pos.y - tipRadius * 0.4f),
                    radius = tipRadius * 0.3f
                )
            }
        }

        // Layer 7: Floating sparkles around crown
        Canvas(modifier = Modifier.size(badgeSize * 0.86f)) {
            val sparklePositions = listOf(
                Triple(Offset(size.width * 0.15f, size.height * 0.25f), sparkle1, 4.dp.toPx()),
                Triple(Offset(size.width * 0.85f, size.height * 0.3f), sparkle2, 3.dp.toPx()),
                Triple(Offset(size.width * 0.2f, size.height * 0.7f), sparkle3, 3.5.dp.toPx()),
                Triple(Offset(size.width * 0.8f, size.height * 0.65f), sparkle1, 2.5.dp.toPx()),
                Triple(Offset(size.width * 0.5f, size.height * 0.1f), sparkle2, 3.dp.toPx())
            )

            sparklePositions.forEach { (pos, alpha, sparkleSize) ->
                // Draw 4-point star sparkle
                val starPath = androidx.compose.ui.graphics.Path().apply {
                    // Vertical line
                    moveTo(pos.x, pos.y - sparkleSize)
                    lineTo(pos.x, pos.y + sparkleSize)
                    moveTo(pos.x - sparkleSize, pos.y)
                    lineTo(pos.x + sparkleSize, pos.y)
                }
                drawPath(
                    path = starPath,
                    color = sparkleColor.copy(alpha = alpha * 0.9f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                )
                // Center glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            sparkleColor.copy(alpha = alpha),
                            goldLight.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        ),
                        center = pos,
                        radius = sparkleSize * 0.8f
                    ),
                    center = pos,
                    radius = sparkleSize * 0.8f
                )
            }
        }
    }
}

@Composable
private fun SocialProofBanner() {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldColor = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val greenColor = if (isDark) PremiumColors.SuccessGreen else PremiumColors.SuccessGreenLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.5f else 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isDark) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rating - Real: 4.7
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = goldColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "4.7",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "rating",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Downloads - Real: 10M+
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = greenColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "10M+",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "users",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Trust
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Secure",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SocialProofBannerCompact() {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldColor = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val greenColor = if (isDark) PremiumColors.SuccessGreen else PremiumColors.SuccessGreenLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.5f else 0.7f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isDark) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rating
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = goldColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "4.7",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Downloads
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = greenColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "10M+",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Secure
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "Secure",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BenefitsSection(isCompact: Boolean = false) {
    val benefits = listOf(
        Triple(Icons.Default.Https, "DNS over HTTPS", "Encrypted & Private"),
        Triple(Icons.Default.PowerSettingsNew, "Auto-Connect", "Start on Boot"),
        Triple(Icons.Default.Widgets, "Quick Toggle", "One-Tap Access"),
        Triple(Icons.Default.Star, "Home Widget", "Quick Access"),
        Triple(Icons.Default.Add, "Unlimited DNS", "Custom Servers"),
        Triple(Icons.Default.Speed, "Speed Tests", "Find Fastest DNS"),
        Triple(Icons.Default.Lightbulb, "Feature Requests", "Shape the App"),
        Triple(Icons.Default.Block, "Zero Ads", "100% Ad-Free")
    )

    Column(
        modifier = Modifier.padding(horizontal = if (isCompact) 12.dp else 20.dp),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
    ) {
        benefits.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
            ) {
                rowItems.forEach { (icon, title, subtitle) ->
                    BenefitItem(
                        icon = icon,
                        title = title,
                        subtitle = subtitle,
                        modifier = Modifier.weight(1f),
                        isCompact = isCompact
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitsSectionLandscape() {
    val benefits = listOf(
        Triple(Icons.Default.Https, "DNS over HTTPS", "Encrypted"),
        Triple(Icons.Default.PowerSettingsNew, "Auto-Connect", "Boot"),
        Triple(Icons.Default.Widgets, "Quick Toggle", "Access"),
        Triple(Icons.Default.Star, "Home Widget", "Quick"),
        Triple(Icons.Default.Add, "Unlimited DNS", "Custom"),
        Triple(Icons.Default.Speed, "Speed Tests", "Fast"),
        Triple(Icons.Default.Lightbulb, "Feature Requests", "Ideas"),
        Triple(Icons.Default.Block, "Zero Ads", "Ad-Free")
    )

    // 3 rows of 2 items each for landscape
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        benefits.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { (icon, title, subtitle) ->
                    BenefitItemCompact(
                        icon = icon,
                        title = title,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.4f else 0.6f),
                shape = RoundedCornerShape(if (isCompact) 8.dp else 10.dp)
            )
            .then(
                if (!isDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(if (isCompact) 8.dp else 10.dp)
                    )
                } else Modifier
            )
            .padding(if (isCompact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 30.dp else 36.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.3f else 0.5f),
                    shape = RoundedCornerShape(if (isCompact) 8.dp else 10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
            )
        }
        Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))
        Column {
            Text(
                text = title,
                style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (isCompact) 10.sp else 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BenefitItemCompact(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.4f else 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .then(
                if (!isDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun BottomPurchaseSection(
    plans: List<PlanDetails>,
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    selectedPlanDetails: PlanDetails?,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    onRestore: () -> Unit,
    isCompact: Boolean = false
) {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = if (isCompact) 24.dp else 32.dp, topEnd = if (isCompact) 24.dp else 32.dp),
        shadowElevation = if (isDark) 24.dp else 12.dp,
        tonalElevation = if (isDark) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 12.dp else 20.dp, vertical = if (isCompact) 12.dp else 20.dp)
        ) {
            // Plan selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
            ) {
                plans.forEach { plan ->
                    PlanOptionCard(
                        plan = plan,
                        isSelected = selectedPlan == plan.id,
                        onClick = { onPlanSelected(plan.id) },
                        modifier = Modifier.weight(1f),
                        isCompact = isCompact
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))

            // Main CTA
            PurchaseCTAButton(
                selectedPlanDetails = selectedPlanDetails,
                isLoading = isLoading,
                onPurchase = onPurchase,
                isCompact = isCompact
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 10.dp))

            // Transparency text
            Text(
                text = if (selectedPlanDetails?.hasFreeTrial == true) {
                    "${selectedPlanDetails.trialDays}-day free trial, then ${selectedPlanDetails.price}/year. Cancel anytime."
                } else {
                    "Cancel anytime. Instant access to all features."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " Secure payment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "  •  ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Restore Purchases",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onRestore() }
                )
            }
        }
    }
}

@Composable
private fun LandscapePurchaseSection(
    plans: List<PlanDetails>,
    selectedPlan: SubscriptionPlan,
    onPlanSelected: (SubscriptionPlan) -> Unit,
    selectedPlanDetails: PlanDetails?,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    onRestore: () -> Unit,
    isCompactHeight: Boolean
) {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = if (isDark) 16.dp else 8.dp,
        tonalElevation = if (isDark) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 12.dp))

            // Plan selector - vertical for landscape
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                plans.forEach { plan ->
                    PlanOptionCardHorizontal(
                        plan = plan,
                        isSelected = selectedPlan == plan.id,
                        onClick = { onPlanSelected(plan.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

            // Main CTA
            PurchaseCTAButton(
                selectedPlanDetails = selectedPlanDetails,
                isLoading = isLoading,
                onPurchase = onPurchase,
                isCompact = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transparency text
            Text(
                text = if (selectedPlanDetails?.hasFreeTrial == true) {
                    "${selectedPlanDetails.trialDays}-day free trial, then ${selectedPlanDetails.price}/year"
                } else {
                    "Cancel anytime"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Footer
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = " Secure",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " • ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                Text(
                    text = "Restore",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onRestore() }
                )
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    plan: PlanDetails,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldPrimary = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val goldDeep = if (isDark) PremiumColors.GoldDeep else PremiumColors.GoldDeepLightMode
    val goldDark = PremiumColors.GoldDark

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    // Solid backgrounds for better appearance
    val backgroundColor = when {
        plan.isBestValue && isSelected -> if (isDark) goldPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest
        plan.isBestValue -> if (isDark) goldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHigh
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val borderColor = when {
        plan.isBestValue && isSelected -> goldDeep
        plan.isBestValue -> goldPrimary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = when {
        plan.isBestValue && isSelected -> 2.5.dp
        plan.isBestValue -> 2.dp
        isSelected -> 2.dp
        else -> 1.dp
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(backgroundColor)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (plan.isBestValue) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(listOf(goldPrimary, goldDeep)),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "MOST POPULAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = plan.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = plan.price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (plan.isBestValue) goldDark else MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "/${plan.period}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val successGreen = if (isDark) PremiumColors.SuccessGreen else PremiumColors.SuccessGreenLight

            if (plan.hasFreeTrial) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = successGreen.copy(alpha = if (isDark) 0.15f else 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${plan.trialDays} DAYS FREE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = successGreen,
                        letterSpacing = 0.3.sp
                    )
                }
            } else if (plan.savings != null && plan.savings > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Save ${plan.savings}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) {
                            if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary
                        } else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanOptionCardHorizontal(
    plan: PlanDetails,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldPrimary = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val goldDeep = if (isDark) PremiumColors.GoldDeep else PremiumColors.GoldDeepLightMode
    val goldDark = PremiumColors.GoldDark
    val successGreen = if (isDark) PremiumColors.SuccessGreen else PremiumColors.SuccessGreenLight

    val backgroundColor = when {
        plan.isBestValue && isSelected -> if (isDark) goldPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest
        plan.isBestValue -> if (isDark) goldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceContainerHigh
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val borderColor = when {
        plan.isBestValue && isSelected -> goldDeep
        plan.isBestValue -> goldPrimary
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Radio button
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(
                    width = 2.dp,
                    color = if (isSelected) {
                        if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary
                    } else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Plan info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (plan.isBestValue) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(listOf(goldPrimary, goldDeep)),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "BEST",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            if (plan.hasFreeTrial) {
                Text(
                    text = "${plan.trialDays}-day free trial",
                    style = MaterialTheme.typography.labelSmall,
                    color = successGreen,
                    fontWeight = FontWeight.Medium
                )
            } else if (plan.savings != null && plan.savings > 0) {
                Text(
                    text = "Save ${plan.savings}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Price
        Text(
            text = plan.price,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (plan.isBestValue) goldDark else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "/${plan.period.take(2)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PurchaseCTAButton(
    selectedPlanDetails: PlanDetails?,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    isCompact: Boolean = false
) {
    // Use background luminance to detect actual theme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "cta")

    val shimmer by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isDark) 0.4f else 0.25f,
        targetValue = if (isDark) 0.7f else 0.45f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )

    Button(
        onClick = { selectedPlanDetails?.storeProduct?.let { onPurchase(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 48.dp else 58.dp)
            .scale(pulse)
            .shadow(
                elevation = if (isDark) 16.dp else 8.dp,
                shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp),
                ambientColor = PremiumColors.ButtonEnd.copy(alpha = glowAlpha),
                spotColor = PremiumColors.ButtonEnd.copy(alpha = glowAlpha)
            ),
        enabled = !isLoading && selectedPlanDetails?.storeProduct != null,
        shape = RoundedCornerShape(if (isCompact) 12.dp else 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PremiumColors.ButtonStart, PremiumColors.ButtonEnd)
                    )
                )
                .drawBehind {
                    val shimmerWidth = size.width * 0.4f
                    val start = size.width * shimmer
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            start = Offset(start - shimmerWidth, 0f),
                            end = Offset(start + shimmerWidth, size.height)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Rocket,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (selectedPlanDetails?.hasFreeTrial == true)
                            "Start Free Trial" else "Get Premium Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

private fun buildPlansList(products: Map<String, StoreProduct>): List<PlanDetails> {
    val monthlyProduct = products[PremiumViewModel.PACKAGE_ID_MONTHLY]
    val yearlyProduct = products[PremiumViewModel.PACKAGE_ID_YEARLY]
    val yearlyTrialProduct = products[PremiumViewModel.PACKAGE_ID_YEARLY_TRIAL]

    // Get formatted prices from RevenueCat (already localized)
    val monthlyPrice = monthlyProduct?.price?.formatted ?: "---"
    val yearlyPrice = yearlyProduct?.price?.formatted ?: "---"
    val yearlyTrialPrice = yearlyTrialProduct?.price?.formatted ?: "---"

    // Get actual amounts for calculations (in micros, divide by 1,000,000)
    val monthlyAmountMicros = monthlyProduct?.price?.amountMicros ?: 0L
    val yearlyAmountMicros = yearlyProduct?.price?.amountMicros ?: 0L
    val yearlyTrialAmountMicros = yearlyTrialProduct?.price?.amountMicros ?: 0L

    val monthlyAmount = monthlyAmountMicros / 1_000_000.0
    val yearlyAmount = yearlyAmountMicros / 1_000_000.0

    // Calculate savings dynamically
    val savingsPercent = if (monthlyAmount > 0 && yearlyAmount > 0) {
        ((monthlyAmount * 12 - yearlyAmount) / (monthlyAmount * 12) * 100).toInt()
    } else {
        0
    }

    // Calculate price per month for yearly plans
    val yearlyPerMonth = if (yearlyAmount > 0) {
        val perMonth = yearlyAmount / 12
        // Format with currency symbol from the product
        val currencySymbol = yearlyProduct?.price?.formatted?.firstOrNull { !it.isDigit() && it != '.' && it != ',' } ?: ""
        "$currencySymbol${String.format("%.2f", perMonth)}"
    } else null

    val yearlyTrialPerMonth = if (yearlyTrialAmountMicros > 0) {
        val perMonth = (yearlyTrialAmountMicros / 1_000_000.0) / 12
        val currencySymbol = yearlyTrialProduct?.price?.formatted?.firstOrNull { !it.isDigit() && it != '.' && it != ',' } ?: ""
        "$currencySymbol${String.format("%.2f", perMonth)}"
    } else null

    return listOf(
        PlanDetails(
            id = SubscriptionPlan.MONTHLY,
            title = "Monthly",
            period = "month",
            price = monthlyPrice,
            originalPrice = null,
            pricePerMonth = null,
            savings = null,
            storeProduct = monthlyProduct
        ),
        PlanDetails(
            id = SubscriptionPlan.YEARLY_TRIAL,
            title = "Yearly",
            period = "year",
            price = yearlyTrialPrice,
            originalPrice = null,
            pricePerMonth = yearlyTrialPerMonth,
            savings = if (savingsPercent > 0) savingsPercent else null,
            isBestValue = true,
            hasFreeTrial = true,
            trialDays = 3,
            storeProduct = yearlyTrialProduct
        ),
        PlanDetails(
            id = SubscriptionPlan.YEARLY,
            title = "Yearly",
            period = "year",
            price = yearlyPrice,
            originalPrice = null,
            pricePerMonth = yearlyPerMonth,
            savings = if (savingsPercent > 0) savingsPercent else null,
            storeProduct = yearlyProduct
        )
    )
}
