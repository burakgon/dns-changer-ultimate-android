package com.dns.changer.ultimate.ui.screens.applock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.ads.AnalyticsEvents
import com.dns.changer.ultimate.ads.LocalAnalyticsManager
import com.dns.changer.ultimate.ui.theme.isAndroidTv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AppLockScreen(
    onPinEntered: (String) -> Unit,
    onBiometricRequest: () -> Unit,
    isPinError: Boolean = false,
    isLockout: Boolean = false,
    lockoutRemainingSeconds: Int = 0,
    isBiometricAvailable: Boolean = true,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val analytics = LocalAnalyticsManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isTv = isAndroidTv()

    // Log screen view
    LaunchedEffect(Unit) {
        analytics.logScreenView("app_lock")
        analytics.logEvent(AnalyticsEvents.APP_LOCK_SHOWN)
    }

    var pin by remember { mutableStateOf("") }
    val maxPinLength = 4

    // Shake animation for error
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isPinError) {
        if (isPinError) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            // Shake animation
            repeat(4) {
                shakeOffset.animateTo(
                    targetValue = if (it % 2 == 0) 15f else -15f,
                    animationSpec = tween(50)
                )
            }
            shakeOffset.animateTo(0f)
            pin = ""
        }
    }

    // Auto-submit when PIN is complete
    LaunchedEffect(pin) {
        if (pin.length == maxPinLength) {
            delay(150)  // Small delay for visual feedback
            onPinEntered(pin)
        }
    }

    // Background gradient
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val isCompact = maxWidth < 400.dp
            val isLandscape = maxWidth > maxHeight
            val isLargeScreen = maxWidth > 600.dp

            // Adjust sizing based on screen size
            val iconSize = when {
                isCompact -> 72.dp
                isLargeScreen -> 120.dp
                else -> 96.dp
            }

            val pinDotSize = when {
                isCompact -> 16.dp
                isLargeScreen -> 24.dp
                else -> 20.dp
            }

            val numberButtonSize = when {
                isCompact -> 64.dp
                isLargeScreen -> 80.dp
                isTv -> 72.dp
                else -> 72.dp
            }

            if (isLandscape && !isLargeScreen) {
                // Landscape layout for phones
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Lock icon and status
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LockIcon(
                            size = iconSize,
                            isError = isPinError,
                            isLockout = isLockout
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when {
                                isLockout -> stringResource(R.string.app_lock_too_many_attempts)
                                errorMessage != null -> errorMessage
                                else -> stringResource(R.string.app_lock_enter_pin)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isPinError || isLockout) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            textAlign = TextAlign.Center
                        )

                        if (isLockout) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.app_lock_try_again_in, lockoutRemainingSeconds),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // PIN dots
                        PinDots(
                            pinLength = pin.length,
                            maxLength = maxPinLength,
                            dotSize = pinDotSize,
                            isError = isPinError,
                            modifier = Modifier.offset(x = shakeOffset.value.dp)
                        )

                        if (isBiometricAvailable && !isLockout) {
                            Spacer(modifier = Modifier.height(24.dp))
                            BiometricButton(
                                onClick = onBiometricRequest,
                                isTv = isTv
                            )
                        }
                    }

                    // Right side - Number pad
                    NumberPad(
                        onNumberClick = { number ->
                            if (pin.length < maxPinLength && !isLockout) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pin += number
                            }
                        },
                        onBackspace = {
                            if (pin.isNotEmpty()) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pin = pin.dropLast(1)
                            }
                        },
                        buttonSize = numberButtonSize,
                        isEnabled = !isLockout,
                        isTv = isTv,
                        modifier = Modifier.widthIn(max = 280.dp)
                    )
                }
            } else {
                // Portrait layout (default)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isLargeScreen) 64.dp else 24.dp,
                            vertical = if (isLargeScreen) 48.dp else 32.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.5f))

                    // Lock Icon with animation
                    LockIcon(
                        size = iconSize,
                        isError = isPinError,
                        isLockout = isLockout
                    )

                    Spacer(modifier = Modifier.height(if (isLargeScreen) 32.dp else 24.dp))

                    // Title
                    Text(
                        text = stringResource(R.string.app_lock_title),
                        style = if (isLargeScreen) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Subtitle / Error message
                    Text(
                        text = when {
                            isLockout -> stringResource(R.string.app_lock_too_many_attempts)
                            errorMessage != null -> errorMessage
                            else -> stringResource(R.string.app_lock_enter_pin)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isPinError || isLockout) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )

                    if (isLockout) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LockoutTimer(seconds = lockoutRemainingSeconds)
                    }

                    Spacer(modifier = Modifier.height(if (isLargeScreen) 48.dp else 32.dp))

                    // PIN dots
                    PinDots(
                        pinLength = pin.length,
                        maxLength = maxPinLength,
                        dotSize = pinDotSize,
                        isError = isPinError,
                        modifier = Modifier.offset(x = shakeOffset.value.dp)
                    )

                    Spacer(modifier = Modifier.weight(0.3f))

                    // Number Pad
                    NumberPad(
                        onNumberClick = { number ->
                            if (pin.length < maxPinLength && !isLockout) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pin += number
                            }
                        },
                        onBackspace = {
                            if (pin.isNotEmpty()) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pin = pin.dropLast(1)
                            }
                        },
                        onBiometric = if (isBiometricAvailable && !isLockout) onBiometricRequest else null,
                        buttonSize = numberButtonSize,
                        isEnabled = !isLockout,
                        isTv = isTv,
                        modifier = Modifier.widthIn(max = if (isLargeScreen) 360.dp else 300.dp)
                    )

                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }
        }
    }
}

@Composable
private fun LockIcon(
    size: Dp,
    isError: Boolean,
    isLockout: Boolean
) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val iconColor = when {
        isError || isLockout -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val containerColor = when {
        isError || isLockout -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    }

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(size * 1.4f)
                .scale(scale)
                .alpha(glowAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            iconColor.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Icon container
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = containerColor,
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = iconColor.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLockout) Icons.Outlined.Lock else Icons.Filled.Lock,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
private fun PinDots(
    pinLength: Int,
    maxLength: Int,
    dotSize: Dp,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val filledColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val emptyColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(maxLength) { index ->
            val isFilled = index < pinLength

            // Scale animation when filled
            val scale by animateFloatAsState(
                targetValue = if (isFilled) 1.2f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "dot_scale"
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        color = if (isFilled) filledColor else Color.Transparent,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (isFilled) filledColor else emptyColor,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun NumberPad(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)? = null,
    buttonSize: Dp,
    isEnabled: Boolean,
    isTv: Boolean,
    modifier: Modifier = Modifier
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("biometric", "0", "backspace")
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        numbers.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                row.forEach { item ->
                    when (item) {
                        "biometric" -> {
                            if (onBiometric != null) {
                                NumberButton(
                                    onClick = onBiometric,
                                    size = buttonSize,
                                    isEnabled = isEnabled,
                                    isTv = isTv,
                                    isSpecial = true
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = stringResource(R.string.app_lock_use_biometric),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(buttonSize * 0.4f)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(buttonSize))
                            }
                        }
                        "backspace" -> {
                            NumberButton(
                                onClick = onBackspace,
                                size = buttonSize,
                                isEnabled = isEnabled,
                                isTv = isTv,
                                isSpecial = true
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = stringResource(R.string.app_lock_backspace),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(buttonSize * 0.35f)
                                )
                            }
                        }
                        else -> {
                            NumberButton(
                                onClick = { onNumberClick(item) },
                                size = buttonSize,
                                isEnabled = isEnabled,
                                isTv = isTv
                            ) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
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
private fun NumberButton(
    onClick: () -> Unit,
    size: Dp,
    isEnabled: Boolean,
    isTv: Boolean,
    isSpecial: Boolean = false,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isFocused && isTv -> MaterialTheme.colorScheme.primaryContainer
        isSpecial -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = when {
        isFocused && isTv -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .focusable(interactionSource = interactionSource)
            .background(backgroundColor, CircleShape)
            .then(
                if (isFocused && isTv) {
                    Modifier.border(2.dp, borderColor, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun BiometricButton(
    onClick: () -> Unit,
    isTv: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Surface(
        modifier = Modifier
            .focusable(interactionSource = interactionSource)
            .then(
                if (isFocused && isTv) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            ),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.app_lock_use_biometric),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LockoutTimer(seconds: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.app_lock_try_again_in, seconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
