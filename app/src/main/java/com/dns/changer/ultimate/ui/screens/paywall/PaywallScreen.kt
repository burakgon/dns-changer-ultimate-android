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
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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

// Screen size classification for adaptive layouts - based on HEIGHT
private enum class HeightSizeClass {
    VERY_COMPACT,  // < 580dp - small phones, landscape
    COMPACT,       // 580-680dp - normal phones
    MEDIUM,        // 680-800dp - large phones
    EXPANDED       // > 800dp - tablets, foldables unfolded
}

// Width size class for scaling on wider screens
private enum class WidthSizeClass {
    COMPACT,       // < 360dp - narrow phones
    MEDIUM,        // 360-600dp - normal phones
    EXPANDED,      // 600-840dp - large phones, small tablets
    LARGE          // > 840dp - tablets, foldables unfolded
}

// Data class to hold layout configuration
private data class PaywallLayoutConfig(
    val heightClass: HeightSizeClass,
    val widthClass: WidthSizeClass,
    val crownSize: Dp,
    val showAllBenefits: Boolean,
    val benefitCount: Int,
    val titleStyle: androidx.compose.ui.text.TextStyle,
    val verticalSpacing: Dp,
    val horizontalPadding: Dp,
    val planCardHeight: Dp,
    val ctaHeight: Dp,
    val maxContentWidth: Dp,
    val planCardMinWidth: Dp,
    val priceTextStyle: androidx.compose.ui.text.TextStyle
)

@Composable
fun PaywallScreen(
    products: Map<String, StoreProduct>,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    onClearError: () -> Unit = {}
) {
    var selectedPlan by remember { mutableStateOf(SubscriptionPlan.YEARLY_TRIAL) }
    var animationStep by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    // Staggered entrance animations
    LaunchedEffect(Unit) {
        delay(50)
        animationStep = 1
        delay(120)
        animationStep = 2
        delay(100)
        animationStep = 3
        delay(120)
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

            // Determine height size class
            val heightClass = when {
                screenHeight < 580.dp -> HeightSizeClass.VERY_COMPACT
                screenHeight < 680.dp -> HeightSizeClass.COMPACT
                screenHeight < 800.dp -> HeightSizeClass.MEDIUM
                else -> HeightSizeClass.EXPANDED
            }

            // Determine width size class
            val widthClass = when {
                screenWidth < 360.dp -> WidthSizeClass.COMPACT
                screenWidth < 600.dp -> WidthSizeClass.MEDIUM
                screenWidth < 840.dp -> WidthSizeClass.EXPANDED
                else -> WidthSizeClass.LARGE
            }

            // Read typography values before remember block (composable context)
            val titleMediumStyle = MaterialTheme.typography.titleMedium
            val titleLargeStyle = MaterialTheme.typography.titleLarge
            val headlineSmallStyle = MaterialTheme.typography.headlineSmall
            val headlineMediumStyle = MaterialTheme.typography.headlineMedium
            val bodyLargeStyle = MaterialTheme.typography.bodyLarge
            val bodyMediumStyle = MaterialTheme.typography.bodyMedium

            // Calculate max content width based on screen size
            val maxContentWidth = when (widthClass) {
                WidthSizeClass.COMPACT -> screenWidth
                WidthSizeClass.MEDIUM -> screenWidth
                WidthSizeClass.EXPANDED -> 540.dp
                WidthSizeClass.LARGE -> 600.dp
            }

            // Calculate plan card min width to prevent text wrapping
            val planCardMinWidth = when (widthClass) {
                WidthSizeClass.COMPACT -> 90.dp
                WidthSizeClass.MEDIUM -> 105.dp
                WidthSizeClass.EXPANDED -> 140.dp
                WidthSizeClass.LARGE -> 160.dp
            }

            // Create layout configuration based on size class
            val layoutConfig = remember(heightClass, widthClass, titleMediumStyle, titleLargeStyle, headlineSmallStyle, headlineMediumStyle, bodyLargeStyle, bodyMediumStyle, maxContentWidth, planCardMinWidth) {
                when (heightClass) {
                    HeightSizeClass.VERY_COMPACT -> PaywallLayoutConfig(
                        heightClass = heightClass,
                        widthClass = widthClass,
                        crownSize = 70.dp,
                        showAllBenefits = false,
                        benefitCount = 4,
                        titleStyle = titleMediumStyle,
                        verticalSpacing = 6.dp,
                        horizontalPadding = 12.dp,
                        planCardHeight = 100.dp,
                        ctaHeight = 44.dp,
                        maxContentWidth = maxContentWidth,
                        planCardMinWidth = planCardMinWidth,
                        priceTextStyle = bodyMediumStyle
                    )
                    HeightSizeClass.COMPACT -> PaywallLayoutConfig(
                        heightClass = heightClass,
                        widthClass = widthClass,
                        crownSize = 90.dp,
                        showAllBenefits = false,
                        benefitCount = 6,
                        titleStyle = titleLargeStyle,
                        verticalSpacing = 8.dp,
                        horizontalPadding = 14.dp,
                        planCardHeight = 110.dp,
                        ctaHeight = 48.dp,
                        maxContentWidth = maxContentWidth,
                        planCardMinWidth = planCardMinWidth,
                        priceTextStyle = bodyLargeStyle
                    )
                    HeightSizeClass.MEDIUM -> PaywallLayoutConfig(
                        heightClass = heightClass,
                        widthClass = widthClass,
                        crownSize = 110.dp,
                        showAllBenefits = true,
                        benefitCount = 8,
                        titleStyle = headlineSmallStyle,
                        verticalSpacing = 12.dp,
                        horizontalPadding = 18.dp,
                        planCardHeight = 130.dp,
                        ctaHeight = 52.dp,
                        maxContentWidth = maxContentWidth,
                        planCardMinWidth = planCardMinWidth,
                        priceTextStyle = titleMediumStyle
                    )
                    HeightSizeClass.EXPANDED -> PaywallLayoutConfig(
                        heightClass = heightClass,
                        widthClass = widthClass,
                        crownSize = 140.dp,
                        showAllBenefits = true,
                        benefitCount = 10,
                        titleStyle = headlineMediumStyle,
                        verticalSpacing = 16.dp,
                        horizontalPadding = 24.dp,
                        planCardHeight = 150.dp,
                        ctaHeight = 58.dp,
                        maxContentWidth = maxContentWidth,
                        planCardMinWidth = planCardMinWidth,
                        priceTextStyle = titleLargeStyle
                    )
                }
            }

            // Use different layouts based on screen size and orientation
            when {
                // Landscape or very wide screen - use horizontal layout
                isLandscape || screenWidth > 840.dp -> {
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
                        config = layoutConfig
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
                        config = layoutConfig
                    )
                }
            }
        }

        // Snackbar for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
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
    config: PaywallLayoutConfig
) {
    val scrollState = rememberScrollState()
    val isVeryCompact = config.heightClass == HeightSizeClass.VERY_COMPACT
    val isCompact = config.heightClass == HeightSizeClass.COMPACT || isVeryCompact
    val isWideScreen = config.widthClass == WidthSizeClass.EXPANDED || config.widthClass == WidthSizeClass.LARGE

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Close button - always visible at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = config.maxContentWidth)
                .padding(4.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .alpha(0.7f)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = config.maxContentWidth)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HERO
            AnimatedVisibility(
                visible = animationStep >= 1,
                enter = fadeIn(tween(400)) + scaleIn(tween(500, easing = EaseOutBack), initialScale = 0.5f)
            ) {
                HeroSection(config = config)
            }

            Spacer(modifier = Modifier.height(config.verticalSpacing))

            // SOCIAL PROOF
            AnimatedVisibility(
                visible = animationStep >= 2,
                enter = fadeIn(tween(300, delayMillis = 50)) +
                        slideInVertically(tween(350)) { it / 4 }
            ) {
                SocialProofBanner(isCompact = isCompact, isWideScreen = isWideScreen)
            }

            Spacer(modifier = Modifier.height(config.verticalSpacing))

            // FEATURES - Auto-scrolling infinite loop
            AnimatedVisibility(
                visible = animationStep >= 3,
                enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { it / 3 }
            ) {
                BenefitsSection(config = config)
            }

            Spacer(modifier = Modifier.height(config.verticalSpacing))
        }

        // BOTTOM SECTION - Fixed at bottom, centered on wide screens
        AnimatedVisibility(
            visible = animationStep >= 4,
            enter = slideInVertically(tween(400, easing = EaseOutCubic)) { it }
        ) {
            BottomPurchaseSection(
                plans = plans,
                selectedPlan = selectedPlan,
                onPlanSelected = onPlanSelected,
                selectedPlanDetails = selectedPlanDetails,
                isLoading = isLoading,
                onPurchase = onPurchase,
                onRestore = onRestore,
                config = config
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
    config: PaywallLayoutConfig
) {
    val leftScrollState = rememberScrollState()
    val rightScrollState = rememberScrollState()

    // Determine if this is a large foldable/tablet (wide screen in landscape)
    val isLargeLandscape = config.widthClass == WidthSizeClass.LARGE || config.widthClass == WidthSizeClass.EXPANDED

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isLargeLandscape) 32.dp else 12.dp, vertical = if (isLargeLandscape) 16.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isLargeLandscape) 32.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Hero + Benefits (scrollable, vertically centered)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = if (isLargeLandscape) 400.dp else 320.dp)
                        .verticalScroll(leftScrollState)
                        .padding(vertical = if (isLargeLandscape) 16.dp else 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isLargeLandscape) 16.dp else 8.dp)
                ) {
                    // HERO - Scaled for screen size
                    AnimatedVisibility(
                        visible = animationStep >= 1,
                        enter = fadeIn(tween(400)) + scaleIn(tween(500, easing = EaseOutBack), initialScale = 0.5f)
                    ) {
                        if (isLargeLandscape) {
                            HeroSectionLargeLandscape()
                        } else {
                            HeroSectionLandscape()
                        }
                    }

                    // SOCIAL PROOF
                    AnimatedVisibility(
                        visible = animationStep >= 2,
                        enter = fadeIn(tween(300, delayMillis = 50)) +
                                slideInVertically(tween(350)) { it / 4 }
                    ) {
                        if (isLargeLandscape) {
                            SocialProofBanner(isCompact = false, isWideScreen = false)
                        } else {
                            SocialProofBannerCompact()
                        }
                    }

                    // FEATURES - Scaled for screen size
                    AnimatedVisibility(
                        visible = animationStep >= 3,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { it / 3 }
                    ) {
                        if (isLargeLandscape) {
                            BenefitsSectionLargeLandscape()
                        } else {
                            BenefitsSectionLandscape()
                        }
                    }
                }
            }

            // Right side - Plans + CTA (scrollable, vertically centered)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rightScrollState)
                        .padding(vertical = if (isLargeLandscape) 16.dp else 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = animationStep >= 4,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { it / 3 }
                    ) {
                        LandscapePurchaseSection(
                            plans = plans,
                            selectedPlan = selectedPlan,
                            onPlanSelected = onPlanSelected,
                            selectedPlanDetails = selectedPlanDetails,
                            isLoading = isLoading,
                            onPurchase = onPurchase,
                            onRestore = onRestore,
                            isLarge = isLargeLandscape
                        )
                    }
                }
            }
        }

        // Close button - top right
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (isLargeLandscape) 12.dp else 4.dp)
                .alpha(0.7f)
                .size(if (isLargeLandscape) 44.dp else 36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (isLargeLandscape) 24.dp else 20.dp)
            )
        }
    }
}

@Composable
private fun PremiumBackground() {
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

    val goldAlpha = if (isDark) 0.18f else 0.12f
    val goldSecondaryAlpha = if (isDark) 0.05f else 0.04f
    val purpleAlpha = if (isDark) 0.1f else 0.08f
    val bottomGlowAlpha = if (isDark) 0.1f else 0.06f

    val goldColor = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val goldDeepColor = if (isDark) PremiumColors.GoldDeep else PremiumColors.GoldDeepLightMode
    val purpleColor = if (isDark) PremiumColors.PremiumPurple else PremiumColors.PremiumPurpleLight

    Canvas(modifier = Modifier.fillMaxSize()) {
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
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(purpleColor.copy(alpha = purpleAlpha), Color.Transparent),
                center = Offset(size.width * (0.85f - offset2 * 0.15f), size.height * 0.25f),
                radius = size.width * 0.35f
            ),
            center = Offset(size.width * (0.85f - offset2 * 0.15f), size.height * 0.25f),
            radius = size.width * 0.35f
        )
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
private fun HeroSection(config: PaywallLayoutConfig) {
    val isCompact = config.heightClass == HeightSizeClass.VERY_COMPACT ||
                   config.heightClass == HeightSizeClass.COMPACT

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = config.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EpicPremiumBadge(size = config.crownSize)

        Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

        Text(
            text = "Unlock Premium",
            style = config.titleStyle,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Ultimate DNS Protection",
            style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeroSectionLandscape() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EpicPremiumBadge(size = 70.dp)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Unlock Premium",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Ultimate DNS Protection",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeroSectionLargeLandscape() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EpicPremiumBadge(size = 110.dp)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Unlock Premium",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Ultimate DNS Protection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EpicPremiumBadge(size: Dp) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "epic")

    val scaleFactor = size / 140.dp

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

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )
    val innerPulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "innerPulse"
    )

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = if (isDark) 0.4f else 0.25f,
        targetValue = if (isDark) 0.9f else 0.5f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "glow"
    )

    val particleAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "particle"
    )

    val crownFloat by infiniteTransition.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "float"
    )

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

    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer"
    )

    val goldLight = PremiumColors.GoldLight
    val goldPrimary = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val goldDeep = if (isDark) PremiumColors.GoldDeep else PremiumColors.GoldDeepLightMode
    val goldDark = PremiumColors.GoldDark
    val premiumCyan = if (isDark) PremiumColors.PremiumCyan else PremiumColors.PremiumCyanLight
    val premiumPurple = if (isDark) PremiumColors.PremiumPurple else PremiumColors.PremiumPurpleLight

    val RubyRed = if (isDark) Color(0xFFE91E63) else Color(0xFFD81B60)
    val SapphireBlue = if (isDark) Color(0xFF2196F3) else Color(0xFF1976D2)
    val EmeraldGreen = if (isDark) Color(0xFF4CAF50) else Color(0xFF388E3C)
    val sparkleColor = if (isDark) Color.White else goldPrimary

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Outer pulsing glow
        Canvas(modifier = Modifier.size(size).scale(pulse)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldPrimary.copy(alpha = glowIntensity * 0.5f),
                        goldDeep.copy(alpha = glowIntensity * 0.25f),
                        Color.Transparent
                    )
                ),
                radius = this.size.minDimension / 2
            )
        }

        // Layer 2: Outer spinning dashed ring
        Canvas(modifier = Modifier.size(size * 0.89f).rotate(rotation1)) {
            val strokeWidth = (2.dp * scaleFactor).toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2
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

        // Layer 3: Middle gradient ring
        Canvas(modifier = Modifier.size(size * 0.79f).rotate(rotation2)) {
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
                style = Stroke(width = (3.dp * scaleFactor).toPx(), cap = StrokeCap.Round)
            )
        }

        // Layer 4: Inner fast ring
        Canvas(modifier = Modifier.size(size * 0.68f).rotate(rotation3)) {
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
                style = Stroke(width = (4.dp * scaleFactor).toPx(), cap = StrokeCap.Round)
            )
        }

        // Layer 5: Orbiting particles
        Canvas(modifier = Modifier.size(size * 0.86f)) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val orbitRadius = this.size.width / 2 - (8.dp * scaleFactor).toPx()
            val particleCore = if (isDark) Color.White else goldLight

            for (i in 0 until 3) {
                val angle = Math.toRadians((particleAngle + i * 120).toDouble())
                val x = center.x + orbitRadius * cos(angle).toFloat()
                val y = center.y + orbitRadius * sin(angle).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(particleCore, goldLight.copy(alpha = 0.5f), Color.Transparent),
                        center = Offset(x, y),
                        radius = (6.dp * scaleFactor).toPx()
                    ),
                    center = Offset(x, y),
                    radius = (4.dp * scaleFactor).toPx()
                )
            }
        }

        // Layer 6: Crown with floating animation
        Canvas(
            modifier = Modifier
                .size(size * 0.51f)
                .scale(innerPulse)
                .graphicsLayer { translationY = crownFloat * scaleFactor }
        ) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w / 2

            val crownBottom = h * 0.85f
            val crownTop = h * 0.15f
            val baseHeight = h * 0.12f
            val crownWidth = w * 0.9f
            val leftEdge = (w - crownWidth) / 2
            val rightEdge = leftEdge + crownWidth

            // Crown glow
            val glowAlpha = if (isDark) 0.4f else 0.25f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(goldPrimary.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(centerX, h * 0.5f),
                    radius = w * 0.6f
                ),
                center = Offset(centerX, h * 0.5f),
                radius = w * 0.6f
            )

            // Crown path
            val crownPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(leftEdge, crownBottom)
                lineTo(leftEdge, crownBottom - baseHeight)
                val pointWidth = crownWidth / 5
                lineTo(leftEdge + pointWidth * 0.5f, h * 0.55f)
                lineTo(leftEdge + pointWidth * 1f, crownTop + h * 0.15f)
                lineTo(leftEdge + pointWidth * 1.5f, h * 0.45f)
                lineTo(leftEdge + pointWidth * 2f, crownTop + h * 0.08f)
                lineTo(centerX, h * 0.38f)
                lineTo(centerX, crownTop)
                lineTo(centerX, h * 0.38f)
                lineTo(leftEdge + pointWidth * 3f, crownTop + h * 0.08f)
                lineTo(leftEdge + pointWidth * 3.5f, h * 0.45f)
                lineTo(leftEdge + pointWidth * 4f, crownTop + h * 0.15f)
                lineTo(leftEdge + pointWidth * 4.5f, h * 0.55f)
                lineTo(rightEdge, crownBottom - baseHeight)
                lineTo(rightEdge, crownBottom)
                close()
            }

            // Crown body
            drawPath(
                path = crownPath,
                brush = Brush.linearGradient(
                    colors = listOf(goldLight, goldPrimary, goldDeep, goldDark, goldDeep),
                    start = Offset(leftEdge, crownTop),
                    end = Offset(rightEdge, crownBottom)
                )
            )

            // Crown shine
            drawPath(
                path = crownPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent, Color.Transparent),
                    start = Offset(leftEdge, crownTop),
                    end = Offset(centerX, h * 0.6f)
                )
            )

            // Shimmer
            val shimmerStart = w * shimmer
            val shimmerWidth = w * 0.3f
            drawPath(
                path = crownPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent),
                    start = Offset(shimmerStart - shimmerWidth, 0f),
                    end = Offset(shimmerStart + shimmerWidth, h)
                )
            )

            // Base band
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

            // Center jewel
            val centerJewelY = h * 0.52f
            val jewelRadius = (6.dp * scaleFactor).toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, RubyRed, RubyRed.copy(alpha = 0.8f)),
                    center = Offset(centerX - jewelRadius * 0.3f, centerJewelY - jewelRadius * 0.3f),
                    radius = jewelRadius
                ),
                center = Offset(centerX, centerJewelY),
                radius = jewelRadius
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                center = Offset(centerX - jewelRadius * 0.3f, centerJewelY - jewelRadius * 0.3f),
                radius = jewelRadius * 0.25f
            )

            // Side jewels
            val leftJewelX = leftEdge + crownWidth * 0.22f
            val leftJewelY = h * 0.58f
            val smallJewelRadius = (4.dp * scaleFactor).toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, SapphireBlue, SapphireBlue.copy(alpha = 0.8f)),
                    center = Offset(leftJewelX - smallJewelRadius * 0.3f, leftJewelY - smallJewelRadius * 0.3f),
                    radius = smallJewelRadius
                ),
                center = Offset(leftJewelX, leftJewelY),
                radius = smallJewelRadius
            )

            val rightJewelX = leftEdge + crownWidth * 0.78f
            val rightJewelY = h * 0.58f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, EmeraldGreen, EmeraldGreen.copy(alpha = 0.8f)),
                    center = Offset(rightJewelX - smallJewelRadius * 0.3f, rightJewelY - smallJewelRadius * 0.3f),
                    radius = smallJewelRadius
                ),
                center = Offset(rightJewelX, rightJewelY),
                radius = smallJewelRadius
            )

            // Crown tip balls
            val tipRadius = (3.dp * scaleFactor).toPx()
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
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    center = Offset(pos.x - tipRadius * 0.3f, pos.y - tipRadius * 0.4f),
                    radius = tipRadius * 0.3f
                )
            }
        }

        // Layer 7: Floating sparkles
        Canvas(modifier = Modifier.size(size * 0.86f)) {
            val sparklePositions = listOf(
                Triple(Offset(this.size.width * 0.15f, this.size.height * 0.25f), sparkle1, (4.dp * scaleFactor).toPx()),
                Triple(Offset(this.size.width * 0.85f, this.size.height * 0.3f), sparkle2, (3.dp * scaleFactor).toPx()),
                Triple(Offset(this.size.width * 0.2f, this.size.height * 0.7f), sparkle3, (3.5.dp * scaleFactor).toPx()),
                Triple(Offset(this.size.width * 0.8f, this.size.height * 0.65f), sparkle1, (2.5.dp * scaleFactor).toPx()),
                Triple(Offset(this.size.width * 0.5f, this.size.height * 0.1f), sparkle2, (3.dp * scaleFactor).toPx())
            )

            sparklePositions.forEach { (pos, alpha, sparkleSize) ->
                val starPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(pos.x, pos.y - sparkleSize)
                    lineTo(pos.x, pos.y + sparkleSize)
                    moveTo(pos.x - sparkleSize, pos.y)
                    lineTo(pos.x + sparkleSize, pos.y)
                }
                drawPath(
                    path = starPath,
                    color = sparkleColor.copy(alpha = alpha * 0.9f),
                    style = Stroke(width = (1.5.dp * scaleFactor).toPx(), cap = StrokeCap.Round)
                )
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
private fun SocialProofBanner(isCompact: Boolean = false, isWideScreen: Boolean = false) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldColor = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val greenColor = if (isDark) PremiumColors.SuccessGreen else PremiumColors.SuccessGreenLight

    val horizontalPadding = when {
        isWideScreen -> 24.dp
        isCompact -> 14.dp
        else -> 20.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.5f else 0.7f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = if (isDark) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = if (isCompact) 8.dp else 10.dp, horizontal = if (isCompact) 12.dp else 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconSize = when {
            isWideScreen -> 20.dp
            isCompact -> 16.dp
            else -> 18.dp
        }
        val textStyle = when {
            isWideScreen -> MaterialTheme.typography.titleSmall
            isCompact -> MaterialTheme.typography.labelMedium
            else -> MaterialTheme.typography.titleSmall
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = goldColor, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(3.dp))
            Text("4.7", style = textStyle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(2.dp))
            Text("rating", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(Modifier.width(1.dp).height(if (isCompact) 20.dp else 24.dp).background(MaterialTheme.colorScheme.outlineVariant))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Download, null, tint = greenColor, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(3.dp))
            Text("10M+", style = textStyle, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(2.dp))
            Text("users", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(Modifier.width(1.dp).height(if (isCompact) 20.dp else 24.dp).background(MaterialTheme.colorScheme.outlineVariant))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(3.dp))
            Text("Secure", style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SocialProofBannerCompact() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldColor = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val greenColor = if (isDark) PremiumColors.SuccessGreen else PremiumColors.SuccessGreenLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.5f else 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = goldColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
            Text("4.7", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Download, null, tint = greenColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
            Text("10M+", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(Modifier.width(1.dp).height(14.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(2.dp))
            Text("Secure", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BenefitsSection(config: PaywallLayoutConfig) {
    val allBenefits = listOf(
        Triple(Icons.Default.Https, "DNS over HTTPS", "Encrypted & Private"),
        Triple(Icons.Default.PowerSettingsNew, "Auto-Connect", "Start on Boot"),
        Triple(Icons.Default.Widgets, "Quick Toggle", "One-Tap Access"),
        Triple(Icons.Default.Block, "Zero Ads", "100% Ad-Free"),
        Triple(Icons.Default.Add, "Unlimited DNS", "Custom Servers"),
        Triple(Icons.Default.Speed, "Speed Tests", "Find Fastest DNS"),
        Triple(Icons.Default.Star, "Home Widget", "Quick Access"),
        Triple(Icons.Default.Lightbulb, "Feature Requests", "Shape the App")
    )

    val isCompact = config.heightClass == HeightSizeClass.VERY_COMPACT ||
                   config.heightClass == HeightSizeClass.COMPACT

    // Create rows (pairs of benefits)
    val benefitRows = allBenefits.chunked(2)
    val numRows = benefitRows.size

    // Number of visible rows (calculate first as it affects other dimensions)
    val visibleRows = config.benefitCount / 2

    // Calculate dimensions - larger rows for bigger screens
    val rowHeightDp = when {
        isCompact -> 38.dp
        visibleRows >= 5 -> 54.dp  // Larger screens get bigger rows
        visibleRows >= 4 -> 52.dp
        else -> 48.dp
    }
    val rowSpacingDp = if (isCompact) 4.dp else 8.dp

    // Visible area height (rows + gaps between them)
    val visibleHeightDp = rowHeightDp * visibleRows + rowSpacingDp * (visibleRows - 1)

    // Calculate pixel values for animation
    val density = LocalDensity.current
    val rowHeightPx = with(density) { rowHeightDp.toPx() }
    val rowSpacingPx = with(density) { rowSpacingDp.toPx() }
    val rowUnitPx = rowHeightPx + rowSpacingPx
    val cycleHeightPx = rowUnitPx * numRows
    val visibleHeightPx = with(density) { visibleHeightDp.toPx() }

    // Scroll position in pixels
    var scrollPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val scrollSpeedPxPerMs = cycleHeightPx / (numRows * 2000f)
        var lastFrameNanos = 0L

        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaMs = (frameNanos - lastFrameNanos) / 1_000_000f
                    scrollPx = (scrollPx + scrollSpeedPxPerMs * deltaMs) % cycleHeightPx
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    // Get background color for fade gradients
    val backgroundColor = MaterialTheme.colorScheme.surface
    val fadeHeight = when {
        isCompact -> 16.dp
        visibleRows >= 5 -> 40.dp
        visibleRows >= 4 -> 32.dp
        else -> 24.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = config.horizontalPadding)
            .height(visibleHeightDp)
            .clip(RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
    ) {
        // Render each row at its calculated position
        // We need enough rows to fill visible area plus one cycle
        val totalRowsNeeded = numRows + visibleRows + 1

        for (i in 0 until totalRowsNeeded) {
            val rowIndex = i % numRows
            val rowItems = benefitRows[rowIndex]

            // Calculate y position for this row
            val baseY = i * rowUnitPx
            var y = baseY - scrollPx

            // Wrap around for seamless loop
            if (y < -rowUnitPx) {
                y += cycleHeightPx
            }

            // Only render if within visible bounds (with some margin)
            if (y >= -rowHeightPx && y <= visibleHeightPx) {
                Row(
                    modifier = Modifier
                        .offset { IntOffset(0, y.toInt()) }
                        .fillMaxWidth()
                        .height(rowHeightDp),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
                ) {
                    rowItems.forEach { (icon, title, subtitle) ->
                        BenefitItem(
                            icon = icon,
                            title = title,
                            subtitle = if (isCompact) null else subtitle,
                            modifier = Modifier.weight(1f),
                            isCompact = isCompact
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Top fade gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fadeHeight)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0f)
                        )
                    )
                )
        )

        // Bottom fade gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0f),
                            backgroundColor
                        )
                    )
                )
        )
    }
}

@Composable
private fun BenefitsSectionLandscape() {
    val benefits = listOf(
        Pair(Icons.Default.Https, "DNS over HTTPS"),
        Pair(Icons.Default.PowerSettingsNew, "Auto-Connect"),
        Pair(Icons.Default.Widgets, "Quick Toggle"),
        Pair(Icons.Default.Block, "Zero Ads"),
        Pair(Icons.Default.Add, "Unlimited DNS"),
        Pair(Icons.Default.Speed, "Speed Tests")
    )

    Column(
        modifier = Modifier.padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        benefits.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowItems.forEach { (icon, title) ->
                    BenefitItemCompact(icon = icon, title = title, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BenefitsSectionLargeLandscape() {
    val benefits = listOf(
        Triple(Icons.Default.Https, "DNS over HTTPS", "Encrypted & Private"),
        Triple(Icons.Default.PowerSettingsNew, "Auto-Connect", "Start on Boot"),
        Triple(Icons.Default.Widgets, "Quick Toggle", "One-Tap Access"),
        Triple(Icons.Default.Block, "Zero Ads", "100% Ad-Free"),
        Triple(Icons.Default.Add, "Unlimited DNS", "Custom Servers"),
        Triple(Icons.Default.Speed, "Speed Tests", "Find Fastest DNS")
    )

    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        benefits.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (icon, title, subtitle) ->
                    BenefitItem(
                        icon = icon,
                        title = title,
                        subtitle = subtitle,
                        modifier = Modifier.weight(1f),
                        isCompact = false
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
    subtitle: String?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.4f else 0.6f),
                shape = RoundedCornerShape(if (isCompact) 8.dp else 10.dp)
            )
            .then(
                if (!isDark) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(if (isCompact) 8.dp else 10.dp))
                else Modifier
            )
            .padding(if (isCompact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 26.dp else 32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.3f else 0.5f),
                    shape = RoundedCornerShape(if (isCompact) 6.dp else 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (isCompact) 14.dp else 18.dp)
            )
        }
        Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 8.dp))
        Column {
            Text(
                text = title,
                style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BenefitItemCompact(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.4f else 0.6f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
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
    config: PaywallLayoutConfig
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isCompact = config.heightClass == HeightSizeClass.VERY_COMPACT ||
                   config.heightClass == HeightSizeClass.COMPACT
    val isWideScreen = config.widthClass == WidthSizeClass.EXPANDED || config.widthClass == WidthSizeClass.LARGE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = if (isCompact) 20.dp else 28.dp, topEnd = if (isCompact) 20.dp else 28.dp),
        shadowElevation = if (isDark) 20.dp else 10.dp,
        tonalElevation = if (isDark) 2.dp else 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = config.maxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = config.horizontalPadding, vertical = if (isCompact) 10.dp else 16.dp)
            ) {
                // Plan selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 10.dp)
                ) {
                    plans.forEach { plan ->
                        PlanOptionCard(
                            plan = plan,
                            isSelected = selectedPlan == plan.id,
                            onClick = { onPlanSelected(plan.id) },
                            modifier = Modifier.weight(1f),
                            isCompact = isCompact,
                            config = config
                        )
                    }
                }

            Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))

            // Main CTA
            PurchaseCTAButton(
                selectedPlanDetails = selectedPlanDetails,
                isLoading = isLoading,
                onPurchase = onPurchase,
                height = config.ctaHeight
            )

            Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

            // Transparency text
            Text(
                text = if (selectedPlanDetails?.hasFreeTrial == true) {
                    "${selectedPlanDetails.trialDays}-day free trial, then ${selectedPlanDetails.price}/year. Cancel anytime."
                } else {
                    "Cancel anytime. Instant access to all features."
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (isCompact) 10.sp else 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (isCompact) 10.dp else 12.dp))
                Text(" Secure payment", style = MaterialTheme.typography.labelSmall, fontSize = if (isCompact) 9.sp else 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("  •  ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = if (isCompact) 9.sp else 10.sp)
                Text(
                    "Restore Purchases",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onRestore() }
                )
            }
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
    isLarge: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = if (isLarge) 480.dp else 380.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(if (isLarge) 24.dp else 20.dp),
        shadowElevation = if (isDark) 12.dp else 6.dp,
        tonalElevation = if (isDark) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = if (isLarge) 20.dp else 12.dp,
                vertical = if (isLarge) 16.dp else 10.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Choose Your Plan",
                style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(if (isLarge) 12.dp else 8.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (isLarge) 8.dp else 4.dp)) {
                plans.forEach { plan ->
                    PlanOptionCardHorizontal(plan = plan, isSelected = selectedPlan == plan.id, onClick = { onPlanSelected(plan.id) }, isLarge = isLarge)
                }
            }

            Spacer(Modifier.height(if (isLarge) 16.dp else 10.dp))
            PurchaseCTAButton(selectedPlanDetails = selectedPlanDetails, isLoading = isLoading, onPurchase = onPurchase, height = if (isLarge) 52.dp else 44.dp)
            Spacer(Modifier.height(if (isLarge) 10.dp else 6.dp))

            Text(
                text = if (selectedPlanDetails?.hasFreeTrial == true) "${selectedPlanDetails.trialDays}-day free trial, cancel anytime" else "Cancel anytime",
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (isLarge) 12.sp else 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(if (isLarge) 8.dp else 4.dp))
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (isLarge) 14.dp else 10.dp))
                Text(" Secure", style = MaterialTheme.typography.labelSmall, fontSize = if (isLarge) 11.sp else 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = if (isLarge) 11.sp else 9.sp)
                Text("Restore", style = MaterialTheme.typography.labelSmall, fontSize = if (isLarge) 11.sp else 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onRestore() })
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
    isCompact: Boolean = false,
    config: PaywallLayoutConfig? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val goldPrimary = if (isDark) PremiumColors.GoldPrimary else PremiumColors.GoldLightMode
    val goldDeep = if (isDark) PremiumColors.GoldDeep else PremiumColors.GoldDeepLightMode
    val goldDark = PremiumColors.GoldDark
    val successGreen = if (isDark) PremiumColors.SuccessGreen else PremiumColors.SuccessGreenLight

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

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
        plan.isBestValue && isSelected -> 2.dp
        plan.isBestValue -> 1.5.dp
        isSelected -> 2.dp
        else -> 1.dp
    }

    // Determine font size based on price length - auto-scale for long prices like "TRY 37.99"
    val priceLength = plan.price.length
    val priceFontSize = when {
        isCompact && priceLength > 10 -> 14.sp
        isCompact && priceLength > 8 -> 16.sp
        isCompact -> 18.sp
        priceLength > 10 -> 18.sp
        priceLength > 8 -> 20.sp
        else -> 22.sp
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(if (isCompact) 12.dp else 14.dp))
                .background(backgroundColor)
                .border(borderWidth, borderColor, RoundedCornerShape(if (isCompact) 12.dp else 14.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                .padding(vertical = if (isCompact) 8.dp else 12.dp, horizontal = if (isCompact) 6.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (plan.isBestValue) {
                Box(
                    modifier = Modifier
                        .background(brush = Brush.linearGradient(listOf(goldPrimary, goldDeep)), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("BEST VALUE", style = MaterialTheme.typography.labelSmall, fontSize = if (isCompact) 7.sp else 8.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.3.sp)
                }
                Spacer(Modifier.height(if (isCompact) 4.dp else 6.dp))
            }

            Text(
                text = plan.title,
                style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))

            // Price with auto-scaling font size to prevent wrapping
            Text(
                text = plan.price,
                fontSize = priceFontSize,
                fontWeight = FontWeight.Bold,
                color = if (plan.isBestValue) goldDark else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "/${plan.period}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (isCompact) 9.sp else 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            if (plan.hasFreeTrial) {
                Spacer(Modifier.height(if (isCompact) 4.dp else 6.dp))
                Box(
                    modifier = Modifier
                        .background(successGreen.copy(alpha = if (isDark) 0.15f else 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("${plan.trialDays} DAYS FREE", style = MaterialTheme.typography.labelSmall, fontSize = if (isCompact) 8.sp else 9.sp, fontWeight = FontWeight.Bold, color = successGreen, maxLines = 1)
                }
            } else if (plan.savings != null && plan.savings > 0) {
                Spacer(Modifier.height(if (isCompact) 4.dp else 6.dp))
                Text("Save ${plan.savings}%", style = MaterialTheme.typography.labelSmall, fontSize = if (isCompact) 9.sp else 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            } else {
                Spacer(Modifier.height(if (isCompact) 14.dp else 18.dp))
            }

            Spacer(Modifier.height(if (isCompact) 4.dp else 6.dp))
            Box(
                modifier = Modifier.size(if (isCompact) 14.dp else 16.dp).border(2.dp, if (isSelected) { if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary } else MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(Modifier.size(if (isCompact) 8.dp else 10.dp).background(if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun PlanOptionCardHorizontal(
    plan: PlanDetails,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLarge: Boolean = false
) {
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
            .clip(RoundedCornerShape(if (isLarge) 14.dp else 10.dp))
            .background(backgroundColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(if (isLarge) 14.dp else 10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = if (isLarge) 16.dp else 10.dp, vertical = if (isLarge) 12.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(if (isLarge) 18.dp else 14.dp).border(2.dp, if (isSelected) { if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary } else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Box(Modifier.size(if (isLarge) 10.dp else 7.dp).background(if (plan.isBestValue) goldDeep else MaterialTheme.colorScheme.primary, CircleShape))
        }

        Spacer(Modifier.width(if (isLarge) 12.dp else 8.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plan.title,
                    style = if (isLarge) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (plan.isBestValue) {
                    Spacer(Modifier.width(if (isLarge) 6.dp else 4.dp))
                    Box(Modifier.background(brush = Brush.linearGradient(listOf(goldPrimary, goldDeep)), shape = RoundedCornerShape(if (isLarge) 4.dp else 3.dp)).padding(horizontal = if (isLarge) 5.dp else 3.dp, vertical = if (isLarge) 2.dp else 1.dp)) {
                        Text("BEST", style = MaterialTheme.typography.labelSmall, fontSize = if (isLarge) 8.sp else 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            if (plan.hasFreeTrial) {
                Text("${plan.trialDays}-day free trial", style = MaterialTheme.typography.labelSmall, fontSize = if (isLarge) 11.sp else 9.sp, color = successGreen, fontWeight = FontWeight.Medium)
            } else if (plan.savings != null && plan.savings > 0) {
                Text("Save ${plan.savings}%", style = MaterialTheme.typography.labelSmall, fontSize = if (isLarge) 11.sp else 9.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Text(plan.price, style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (plan.isBestValue) goldDark else MaterialTheme.colorScheme.onSurface)
        Text("/${if (plan.period == "year") "yr" else "mo"}", style = MaterialTheme.typography.labelSmall, fontSize = if (isLarge) 11.sp else 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PurchaseCTAButton(
    selectedPlanDetails: PlanDetails?,
    isLoading: Boolean,
    onPurchase: (StoreProduct) -> Unit,
    height: Dp
) {
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
            .height(height)
            .scale(pulse)
            .shadow(
                elevation = if (isDark) 14.dp else 6.dp,
                shape = RoundedCornerShape(height / 4),
                ambientColor = PremiumColors.ButtonEnd.copy(alpha = glowAlpha),
                spotColor = PremiumColors.ButtonEnd.copy(alpha = glowAlpha)
            ),
        enabled = !isLoading && selectedPlanDetails?.storeProduct != null,
        shape = RoundedCornerShape(height / 4),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.horizontalGradient(listOf(PremiumColors.ButtonStart, PremiumColors.ButtonEnd)))
                .drawBehind {
                    val shimmerWidth = size.width * 0.4f
                    val start = size.width * shimmer
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                            start = Offset(start - shimmerWidth, 0f),
                            end = Offset(start + shimmerWidth, size.height)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(height * 0.5f), color = Color.White, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Rocket, null, tint = Color.White, modifier = Modifier.size(height * 0.4f))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedPlanDetails?.hasFreeTrial == true) "Start Free Trial" else "Get Premium Now",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
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

    val monthlyPrice = monthlyProduct?.price?.formatted ?: "---"
    val yearlyPrice = yearlyProduct?.price?.formatted ?: "---"
    val yearlyTrialPrice = yearlyTrialProduct?.price?.formatted ?: "---"

    val monthlyAmountMicros = monthlyProduct?.price?.amountMicros ?: 0L
    val yearlyAmountMicros = yearlyProduct?.price?.amountMicros ?: 0L

    val monthlyAmount = monthlyAmountMicros / 1_000_000.0
    val yearlyAmount = yearlyAmountMicros / 1_000_000.0

    val savingsPercent = if (monthlyAmount > 0 && yearlyAmount > 0) {
        ((monthlyAmount * 12 - yearlyAmount) / (monthlyAmount * 12) * 100).toInt()
    } else 0

    val yearlyPerMonth = if (yearlyAmount > 0) {
        val perMonth = yearlyAmount / 12
        val currencySymbol = yearlyProduct?.price?.formatted?.firstOrNull { !it.isDigit() && it != '.' && it != ',' } ?: ""
        "$currencySymbol${String.format("%.2f", perMonth)}"
    } else null

    val yearlyTrialAmountMicros = yearlyTrialProduct?.price?.amountMicros ?: 0L
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
