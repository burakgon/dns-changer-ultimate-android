package com.dns.changer.ultimate.ui.theme

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
