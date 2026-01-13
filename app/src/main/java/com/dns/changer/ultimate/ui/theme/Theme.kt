package com.dns.changer.ultimate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dns.changer.ultimate.ui.screens.settings.ThemeMode

// Fallback colors for devices without dynamic color support
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6B4DE6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1E192B),
    tertiary = Color(0xFF7E5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF31101D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1C1B1E),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF1C1B1E),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7A757F),
    outlineVariant = Color(0xFFCAC4CF),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFCFBCFF),
    surfaceTint = Color(0xFF6B4DE6)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF3B1E8F),
    primaryContainer = Color(0xFF5336A6),
    onPrimaryContainer = Color(0xFFE8DDFF),
    secondary = Color(0xFFCCC2DB),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF4A2532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD9E3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1E),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF1C1B1E),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454E),
    inverseSurface = Color(0xFFE6E1E6),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF6B4DE6),
    surfaceTint = Color(0xFFCFBCFF)
)

@Composable
fun DnsChangerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDarkTheme
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
