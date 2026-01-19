package com.dns.changer.ultimate.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Window size categories for adaptive layouts
 */
enum class WindowSize {
    COMPACT,   // Phones in portrait
    MEDIUM,    // Tablets in portrait, foldables unfolded
    EXPANDED   // Tablets in landscape, large screens
}

/**
 * Adaptive layout configuration based on window size
 */
data class AdaptiveLayoutConfig(
    val windowSize: WindowSize,
    val contentMaxWidth: Dp,
    val horizontalPadding: Dp,
    val showNavigationRail: Boolean,
    val useListDetailLayout: Boolean
)

/**
 * Composable function to get current adaptive layout configuration
 */
@Composable
fun rememberAdaptiveLayoutConfig(): AdaptiveLayoutConfig {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    return remember(windowSizeClass) {
        // Use the new isWidthAtLeastBreakpoint API
        val windowSize = when {
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WindowSize.EXPANDED
            windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WindowSize.MEDIUM
            else -> WindowSize.COMPACT
        }

        AdaptiveLayoutConfig(
            windowSize = windowSize,
            contentMaxWidth = when (windowSize) {
                WindowSize.COMPACT -> Dp.Unspecified
                WindowSize.MEDIUM -> 600.dp
                WindowSize.EXPANDED -> 840.dp
            },
            horizontalPadding = when (windowSize) {
                WindowSize.COMPACT -> 16.dp
                WindowSize.MEDIUM -> 24.dp
                WindowSize.EXPANDED -> 32.dp
            },
            showNavigationRail = windowSize != WindowSize.COMPACT,
            useListDetailLayout = windowSize == WindowSize.EXPANDED
        )
    }
}

/**
 * Returns true if we're on a tablet or foldable (medium or expanded width)
 */
@Composable
fun isExpandedScreen(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
}

/**
 * Returns true if we're on a large tablet landscape (expanded width)
 */
@Composable
fun isLargeScreen(): Boolean {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
}

/**
 * Returns true if we're running on Android TV or a TV-like device
 */
@Composable
fun isAndroidTv(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        isAndroidTvDevice(context)
    }
}

/**
 * Non-composable version - checks if device is Android TV
 */
fun isAndroidTvDevice(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
           context.packageManager.hasSystemFeature("android.software.leanback")
}

// ============================================
// TV Focus Utilities
// ============================================

/**
 * TV-aware focus modifier that adds visible focus indication on TV devices.
 * On mobile, it simply makes the element focusable without visual changes.
 *
 * @param shape The shape for the focus border (should match the component's shape)
 * @param focusedBorderColor The border color when focused (defaults to primary)
 * @param unfocusedBorderColor The border color when not focused (defaults to transparent)
 * @param focusedBorderWidth The border width when focused
 * @param focusedScale The scale factor when focused (subtle zoom effect)
 * @param interactionSource Optional interaction source for tracking focus state
 */
fun Modifier.tvFocusable(
    shape: Shape? = null,
    focusedBorderColor: Color? = null,
    unfocusedBorderColor: Color = Color.Transparent,
    focusedBorderWidth: Dp = 3.dp,
    focusedScale: Float = 1.02f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val isTv = isAndroidTv()

    if (!isTv) {
        // On mobile, just make it focusable without visual changes
        this.focusable()
    } else {
        // On TV, add focus indication
        val source = interactionSource ?: remember { MutableInteractionSource() }
        val isFocused by source.collectIsFocusedAsState()

        val borderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
        val actualShape = shape ?: MaterialTheme.shapes.medium

        val animatedBorderWidth by animateDpAsState(
            targetValue = if (isFocused) focusedBorderWidth else 0.dp,
            animationSpec = tween(durationMillis = 150),
            label = "focusBorder"
        )

        val animatedScale by animateDpAsState(
            targetValue = if (isFocused) focusedScale.dp else 1.dp,
            animationSpec = tween(durationMillis = 150),
            label = "focusScale"
        )

        this
            .focusable(interactionSource = source)
            .scale(animatedScale.value)
            .then(
                if (animatedBorderWidth > 0.dp) {
                    Modifier.border(
                        width = animatedBorderWidth,
                        color = borderColor,
                        shape = actualShape
                    )
                } else {
                    Modifier
                }
            )
    }
}

/**
 * TV-aware focus modifier with a FocusRequester for programmatic focus control.
 * Useful for setting initial focus on dialogs or screens.
 */
fun Modifier.tvFocusableWithRequester(
    focusRequester: FocusRequester,
    shape: Shape? = null,
    focusedBorderColor: Color? = null,
    focusedBorderWidth: Dp = 3.dp,
    focusedScale: Float = 1.02f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val isTv = isAndroidTv()

    if (!isTv) {
        // On mobile, just add focus requester
        this
            .focusRequester(focusRequester)
            .focusable()
    } else {
        // On TV, add focus indication with requester
        val source = interactionSource ?: remember { MutableInteractionSource() }
        val isFocused by source.collectIsFocusedAsState()

        val borderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
        val actualShape = shape ?: MaterialTheme.shapes.medium

        val animatedBorderWidth by animateDpAsState(
            targetValue = if (isFocused) focusedBorderWidth else 0.dp,
            animationSpec = tween(durationMillis = 150),
            label = "focusBorder"
        )

        val animatedScale by animateDpAsState(
            targetValue = if (isFocused) focusedScale.dp else 1.dp,
            animationSpec = tween(durationMillis = 150),
            label = "focusScale"
        )

        this
            .focusRequester(focusRequester)
            .focusable(interactionSource = source)
            .scale(animatedScale.value)
            .then(
                if (animatedBorderWidth > 0.dp) {
                    Modifier.border(
                        width = animatedBorderWidth,
                        color = borderColor,
                        shape = actualShape
                    )
                } else {
                    Modifier
                }
            )
    }
}

/**
 * Simple focus border modifier for TV - adds a colored border when focused.
 * Lighter weight than tvFocusable, useful for lists and grids.
 */
fun Modifier.tvFocusBorder(
    shape: Shape? = null,
    focusedBorderColor: Color? = null,
    focusedBorderWidth: Dp = 2.dp
): Modifier = composed {
    val isTv = isAndroidTv()

    if (!isTv) {
        this
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()

        val borderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
        val actualShape = shape ?: MaterialTheme.shapes.medium

        this
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = focusedBorderWidth,
                        color = borderColor,
                        shape = actualShape
                    )
                } else {
                    Modifier
                }
            )
    }
}

/**
 * Configuration for TV-specific UI adjustments
 */
data class TvUiConfig(
    val isTv: Boolean,
    val focusBorderWidth: Dp,
    val minTouchTarget: Dp,
    val cardPadding: Dp,
    val iconSize: Dp
)

/**
 * Returns TV-aware UI configuration
 */
@Composable
fun rememberTvUiConfig(): TvUiConfig {
    val isTv = isAndroidTv()
    return remember(isTv) {
        if (isTv) {
            TvUiConfig(
                isTv = true,
                focusBorderWidth = 3.dp,
                minTouchTarget = 56.dp,  // Larger for D-pad navigation
                cardPadding = 16.dp,
                iconSize = 28.dp
            )
        } else {
            TvUiConfig(
                isTv = false,
                focusBorderWidth = 0.dp,
                minTouchTarget = 48.dp,
                cardPadding = 12.dp,
                iconSize = 24.dp
            )
        }
    }
}
