package com.dns.changer.ultimate.ui.screens.applock

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.ui.theme.isAndroidTv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PinSetupStep {
    CREATE,
    CONFIRM
}

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit,
    isChangingPin: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        PinSetupContent(
            onDismiss = onDismiss,
            onPinSet = onPinSet,
            isChangingPin = isChangingPin
        )
    }
}

@Composable
private fun PinSetupContent(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit,
    isChangingPin: Boolean
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isTv = isAndroidTv()

    var step by remember { mutableStateOf(PinSetupStep.CREATE) }
    var firstPin by remember { mutableStateOf("") }
    var currentPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val maxPinLength = 4

    // Shake animation for error
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(4) {
                shakeOffset.animateTo(
                    targetValue = if (it % 2 == 0) 15f else -15f,
                    animationSpec = tween(50)
                )
            }
            shakeOffset.animateTo(0f)
            currentPin = ""
            isError = false
        }
    }

    // Auto-submit when PIN is complete
    LaunchedEffect(currentPin) {
        if (currentPin.length == maxPinLength) {
            delay(150)
            when (step) {
                PinSetupStep.CREATE -> {
                    firstPin = currentPin
                    currentPin = ""
                    step = PinSetupStep.CONFIRM
                }
                PinSetupStep.CONFIRM -> {
                    if (currentPin == firstPin) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPinSet(currentPin)
                    } else {
                        isError = true
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val isCompact = maxWidth < 400.dp
            val isLargeScreen = maxWidth > 600.dp

            val iconSize = when {
                isCompact -> 56.dp
                isLargeScreen -> 80.dp
                else -> 64.dp
            }

            val pinDotSize = when {
                isCompact -> 14.dp
                isLargeScreen -> 20.dp
                else -> 16.dp
            }

            val numberButtonSize = when {
                isCompact -> 56.dp
                isLargeScreen -> 72.dp
                isTv -> 64.dp
                else -> 64.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isLargeScreen) 64.dp else 24.dp,
                        vertical = if (isLargeScreen) 32.dp else 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Step indicator
                    StepIndicator(
                        currentStep = if (step == PinSetupStep.CREATE) 1 else 2,
                        totalSteps = 2
                    )

                    // Placeholder to balance the layout
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.weight(0.3f))

                // Animated step content
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState == PinSetupStep.CONFIRM) {
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "step_transition"
                ) { currentStep ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Lock Icon
                        SetupLockIcon(
                            size = iconSize,
                            step = currentStep,
                            isError = isError
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Title
                        Text(
                            text = when {
                                isChangingPin && currentStep == PinSetupStep.CREATE ->
                                    stringResource(R.string.app_lock_create_new_pin)
                                currentStep == PinSetupStep.CREATE ->
                                    stringResource(R.string.app_lock_create_pin)
                                else ->
                                    stringResource(R.string.app_lock_confirm_pin)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                            text = when {
                                isError -> stringResource(R.string.app_lock_pins_dont_match)
                                currentStep == PinSetupStep.CREATE ->
                                    stringResource(R.string.app_lock_create_pin_subtitle)
                                else ->
                                    stringResource(R.string.app_lock_confirm_pin_subtitle)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // PIN dots
                SetupPinDots(
                    pinLength = currentPin.length,
                    maxLength = maxPinLength,
                    dotSize = pinDotSize,
                    isError = isError,
                    modifier = Modifier.offset(x = shakeOffset.value.dp)
                )

                Spacer(modifier = Modifier.weight(0.2f))

                // Number Pad
                SetupNumberPad(
                    onNumberClick = { number ->
                        if (currentPin.length < maxPinLength) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentPin += number
                        }
                    },
                    onBackspace = {
                        if (currentPin.isNotEmpty()) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentPin = currentPin.dropLast(1)
                        } else if (step == PinSetupStep.CONFIRM) {
                            // Go back to create step
                            step = PinSetupStep.CREATE
                            firstPin = ""
                        }
                    },
                    buttonSize = numberButtonSize,
                    isTv = isTv,
                    modifier = Modifier.widthIn(max = if (isLargeScreen) 320.dp else 280.dp)
                )

                Spacer(modifier = Modifier.weight(0.3f))
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index < currentStep
            val isCurrent = index == currentStep - 1

            Box(
                modifier = Modifier
                    .size(if (isCurrent) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        color = when {
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}

@Composable
private fun SetupLockIcon(
    size: Dp,
    step: PinSetupStep,
    isError: Boolean
) {
    val iconColor = when {
        isError -> MaterialTheme.colorScheme.error
        step == PinSetupStep.CONFIRM -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        step == PinSetupStep.CONFIRM -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    }

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
            imageVector = when {
                step == PinSetupStep.CONFIRM -> Icons.Default.Check
                else -> Icons.Default.LockOpen
            },
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun SetupPinDots(
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
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(maxLength) { index ->
            val isFilled = index < pinLength

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
private fun SetupNumberPad(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit,
    buttonSize: Dp,
    isTv: Boolean,
    modifier: Modifier = Modifier
) {
    val numbers = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "backspace")
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        numbers.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                row.forEach { item ->
                    when (item) {
                        "" -> Spacer(modifier = Modifier.size(buttonSize))
                        "backspace" -> {
                            SetupNumberButton(
                                onClick = onBackspace,
                                size = buttonSize,
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
                            SetupNumberButton(
                                onClick = { onNumberClick(item) },
                                size = buttonSize,
                                isTv = isTv
                            ) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.headlineSmall,
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
private fun SetupNumberButton(
    onClick: () -> Unit,
    size: Dp,
    isTv: Boolean,
    isSpecial: Boolean = false,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor = when {
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
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = size / 2),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
