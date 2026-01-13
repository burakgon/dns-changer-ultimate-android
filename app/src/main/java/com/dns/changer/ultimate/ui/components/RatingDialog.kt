package com.dns.changer.ultimate.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBounce
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dns.changer.ultimate.BuildConfig
import com.dns.changer.ultimate.R
import com.dns.changer.ultimate.ui.theme.DnsShapes
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Rating dialog state
enum class RatingDialogState {
    INITIAL,
    THANK_YOU,
    FEEDBACK
}

// Confetti particle data
data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val rotation: Float,
    val size: Float,
    val velocityY: Float,
    val velocityX: Float,
    val rotationSpeed: Float
)

@Composable
fun RatingDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    onFeedbackSubmit: (String) -> Unit,
    onFeedbackSkip: () -> Unit
) {
    var dialogState by remember { mutableStateOf(RatingDialogState.INITIAL) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Dialog(
            onDismissRequest = { /* Cannot dismiss by tapping scrim */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = dialogState,
                    transitionSpec = {
                        (slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutVertically(
                                    targetOffsetY = { -it },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(300)))
                    },
                    label = "dialogContentTransition"
                ) { state ->
                    when (state) {
                        RatingDialogState.INITIAL -> {
                            InitialRatingContent(
                                onClose = onDismiss,
                                onPositive = {
                                    onPositive()
                                    dialogState = RatingDialogState.THANK_YOU
                                },
                                onNegative = {
                                    onNegative()
                                    dialogState = RatingDialogState.FEEDBACK
                                }
                            )
                        }
                        RatingDialogState.THANK_YOU -> {
                            ThankYouContent(
                                onContinue = onDismiss
                            )
                        }
                        RatingDialogState.FEEDBACK -> {
                            FeedbackContent(
                                onSkip = {
                                    onFeedbackSkip()
                                    onDismiss()
                                },
                                onSubmit = { feedback ->
                                    onFeedbackSubmit(feedback)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InitialRatingContent(
    onClose: () -> Unit,
    onPositive: () -> Unit,
    onNegative: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "initial")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Card(
        modifier = Modifier
            .widthIn(max = 500.dp)
            .fillMaxWidth(0.92f)
            .offset { IntOffset(0, -floatOffset.roundToInt().dp.roundToPx()) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Prevent click through */ },
        shape = DnsShapes.Dialog,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box {
            // Glassmorphism background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = DnsShapes.Dialog
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        ),
                        shape = DnsShapes.Dialog
                    )
            )

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button - top right
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animated hero star icon
                AnimatedStarIcon()

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = stringResource(R.string.rating_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = stringResource(R.string.rating_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Primary button - Yes, I love it!
                FilledTonalButton(
                    onClick = onPositive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = DnsShapes.Button,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    elevation = ButtonDefaults.filledTonalButtonElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.rating_positive),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary button - Not really
                OutlinedButton(
                    onClick = onNegative,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = DnsShapes.Button,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.rating_negative),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedStarIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "starIcon")

    // Pulsing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Glow alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Rotation for outer ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Circular gradient glow
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .scale(pulseScale)
                .alpha(glowAlpha)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.6f),
                        tertiaryColor.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
        }

        // Rotating gradient ring
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .rotate(rotation)
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0f),
                        primaryColor.copy(alpha = 0.8f),
                        tertiaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0f)
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // Star icon background
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, tertiaryColor)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun ThankYouContent(
    onContinue: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Confetti particles
    val particles = remember { mutableStateListOf<ConfettiParticle>() }
    var confettiTime by remember { mutableFloatStateOf(0f) }
    var showConfetti by remember { mutableStateOf(true) }

    // Initialize confetti
    LaunchedEffect(Unit) {
        val colors = listOf(
            Color(0xFF6B4DE6), // Purple
            Color(0xFF2ECC71), // Green
            Color(0xFFFFD700), // Gold
            Color(0xFFFF6B6B), // Red
            Color(0xFF4ECDC4), // Teal
            Color(0xFFFFE66D)  // Yellow
        )

        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

        repeat(50) {
            particles.add(
                ConfettiParticle(
                    x = Random.nextFloat() * screenWidthPx,
                    y = -Random.nextFloat() * 200f,
                    color = colors.random(),
                    rotation = Random.nextFloat() * 360f,
                    size = Random.nextFloat() * 10f + 6f,
                    velocityY = Random.nextFloat() * 6f + 4f, // Faster falling
                    velocityX = Random.nextFloat() * 2f - 1f,
                    rotationSpeed = Random.nextFloat() * 8f - 4f
                )
            )
        }

        // Animate confetti until all particles fall off screen (max 3 seconds)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 3000) {
            confettiTime += 0.016f

            // Check if all particles are off screen
            val allOffScreen = particles.all { particle ->
                val currentY = particle.y + (confettiTime * 60f * particle.velocityY)
                currentY > screenHeightPx + 100f
            }
            if (allOffScreen) break

            delay(16)
        }

        // Hide confetti after animation completes
        showConfetti = false
        particles.clear()
    }

    // Checkmark animation
    val checkmarkScale = remember { Animatable(0f) }
    val checkmarkAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(200)
        checkmarkAlpha.animateTo(1f, tween(300))
        checkmarkScale.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Confetti canvas - only show while animating
        if (showConfetti) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val screenHeight = size.height

                particles.forEach { particle ->
                    val currentY = particle.y + (confettiTime * 60f * particle.velocityY)
                    val currentX = particle.x + (confettiTime * 60f * particle.velocityX)
                    val currentRotation = particle.rotation + (confettiTime * 60f * particle.rotationSpeed)

                    if (currentY < screenHeight + 50f) {
                        rotate(currentRotation, pivot = Offset(currentX, currentY)) {
                            drawRect(
                                color = particle.color,
                                topLeft = Offset(currentX - particle.size / 2, currentY - particle.size / 2),
                                size = androidx.compose.ui.geometry.Size(particle.size, particle.size * 0.6f)
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.92f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Prevent click through */ },
            shape = DnsShapes.Dialog,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated checkmark morphing from heart
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(checkmarkScale.value)
                        .alpha(checkmarkAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
                    // Gradient background circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2ECC71),
                                        Color(0xFF1ABC9C)
                                    )
                                )
                            )
                    )

                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title with gradient effect (simulated)
                Text(
                    text = stringResource(R.string.thank_you_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.thank_you_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Continue button with ripple
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = DnsShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackContent(
    onSkip: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }
    val minCharacters = 25
    val remainingChars = minCharacters - feedbackText.length
    val isValid = feedbackText.length >= minCharacters

    // Entry animation
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.9f) }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, tween(300))
        contentScale.animateTo(
            1f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Card(
        modifier = Modifier
            .widthIn(max = 500.dp)
            .fillMaxWidth(0.92f)
            .scale(contentScale.value)
            .alpha(contentAlpha.value)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Prevent click through */ },
        shape = DnsShapes.Dialog,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header icon with orange/red gradient
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B),
                                Color(0xFFFF8E53)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.feedback_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.feedback_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TextField with Material 3 styling
            OutlinedTextField(
                value = feedbackText,
                onValueChange = { feedbackText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.feedback_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                supportingText = {
                    AnimatedContent(
                        targetState = isValid,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        },
                        label = "charCountTransition"
                    ) { valid ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (valid) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF2ECC71),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.feedback_ready),
                                    color = Color(0xFF2ECC71),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.feedback_min_chars, remainingChars),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Skip button
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Send button
                Button(
                    onClick = { onSubmit(feedbackText) },
                    modifier = Modifier.weight(1f),
                    enabled = isValid,
                    shape = DnsShapes.Button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.send),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
