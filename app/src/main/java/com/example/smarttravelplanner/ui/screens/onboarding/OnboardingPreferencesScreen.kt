package com.example.smarttravelplanner.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.CardAction
import com.example.smarttravelplanner.domain.model.PreferenceCardItem
import com.example.smarttravelplanner.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun OnboardingPreferencesScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    userEmail: String? = null,
    isEditMode: Boolean = false,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isEditMode) {
        viewModel.isEditMode = isEditMode
    }

    LaunchedEffect(userEmail) {
        if (!userEmail.isNullOrBlank()) {
            viewModel.loadPreferencesForAccount(userEmail)
        } else {
            viewModel.startWithSessionUser()
        }
    }

    // Reaction to completion or skip
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onComplete()
        }
    }

    LaunchedEffect(uiState.isSkipped) {
        if (uiState.isSkipped) {
            onSkip()
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val onSurfaceColor = if (isDark) OnBackgroundWhite else OnBackgroundDark
    val mutedColor = if (isDark) OnBackgroundGray else OnBackgroundMedium

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = Amber,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )
        } else if (uiState.isFinishedDeck) {
            // Deck Finished Celebration / Summary
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                FinishedDeckView(
                    uiState = uiState,
                    isDark = isDark,
                    onContinue = { viewModel.completeOnboarding() }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Clean Header (No hamburger menu: Title on left, "Skip this" on right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEditMode) "Edit Preferences" else "Choose Preferences",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = PlayfairFontFamily,
                        color = onSurfaceColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!isEditMode && !uiState.isFinishedDeck) {
                        TextButton(
                            onClick = { viewModel.skipOnboarding() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Skip this",
                                color = Amber,
                                fontSize = 14.sp,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Progress Indicator directly below the header
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentDisplay = (uiState.currentIndex + 1).coerceAtMost(uiState.totalCards)
                        Text(
                            text = "Card $currentDisplay of ${uiState.totalCards}",
                            fontSize = 12.sp,
                            color = mutedColor,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.Medium
                        )

                        val likedCount = uiState.likedInterestIds.size
                        if (likedCount > 0) {
                            Text(
                                text = "$likedCount vibe${if (likedCount > 1) "s" else ""} liked",
                                fontSize = 12.sp,
                                color = CompletedGreen,
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Amber,
                        trackColor = if (isDark) CardBorderDark else CardBorderLight
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Card Stack: occupies remaining available vertical space smoothly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Next card in stack (background depth effect)
                    uiState.nextCard?.let { nextCard ->
                        val nextCardSuperLiked = nextCard.id in uiState.stronglyPreferredInterestIds
                        val nextCardLiked = nextCard.id in uiState.likedInterestIds && !nextCardSuperLiked
                        val nextCardDisliked = nextCard.id in uiState.dislikedInterestIds

                        TinderCardView(
                            card = nextCard,
                            isDark = isDark,
                            isTopCard = false,
                            isSuperLiked = nextCardSuperLiked,
                            isLiked = nextCardLiked,
                            isDisliked = nextCardDisliked,
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .fillMaxHeight(0.97f)
                                .offset(y = 10.dp)
                                .graphicsLayer {
                                    scaleX = 0.94f
                                    scaleY = 0.94f
                                }
                        )
                    }

                    // Active top card (interactive swipe + double tap)
                    uiState.currentCard?.let { topCard ->
                        val topCardSuperLiked = topCard.id in uiState.stronglyPreferredInterestIds
                        val topCardLiked = topCard.id in uiState.likedInterestIds && !topCardSuperLiked
                        val topCardDisliked = topCard.id in uiState.dislikedInterestIds

                        InteractiveTopCard(
                            card = topCard,
                            isDark = isDark,
                            isSuperLiked = topCardSuperLiked,
                            isLiked = topCardLiked,
                            isDisliked = topCardDisliked,
                            onAction = { action ->
                                viewModel.onCardAction(action)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Action Controls: Centered, evenly spaced, comfortable spacing above bottom
                val currentCardId = uiState.currentCard?.id
                val isCurrentCardSuperLiked = currentCardId != null && currentCardId in uiState.stronglyPreferredInterestIds
                val isCurrentCardLiked = currentCardId != null && currentCardId in uiState.likedInterestIds && !isCurrentCardSuperLiked
                val isCurrentCardDisliked = currentCardId != null && currentCardId in uiState.dislikedInterestIds

                BottomActionControls(
                    canUndo = uiState.canUndo,
                    isActionInProgress = uiState.isActionInProgress,
                    isSuperLiked = isCurrentCardSuperLiked,
                    isLiked = isCurrentCardLiked,
                    isDisliked = isCurrentCardDisliked,
                    onUndo = { viewModel.undoLastAction() },
                    onDislike = { viewModel.onCardAction(CardAction.DISLIKE) },
                    onSuperLike = { viewModel.onCardAction(CardAction.STRONG_PREFERENCE) },
                    onLike = { viewModel.onCardAction(CardAction.LIKE) }
                )
            }
        }

        // Error snackbar / alert
        uiState.errorMessage?.let { errorMsg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = Amber)
                    }
                },
                containerColor = if (isDark) SurfaceElevated else SurfaceElevatedLight,
                contentColor = onSurfaceColor
            ) {
                Text(errorMsg, fontFamily = DmSansFontFamily, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Top interactive card with smooth drag rotation, spring return, and gesture detection.
 */
@Composable
private fun InteractiveTopCard(
    card: PreferenceCardItem,
    isDark: Boolean,
    isSuperLiked: Boolean,
    isLiked: Boolean,
    isDisliked: Boolean,
    onAction: (CardAction) -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(card.id) { Animatable(0f) }
    val offsetY = remember(card.id) { Animatable(0f) }
    var isDoubleTapHeartVisible by remember(card.id) { mutableStateOf(false) }

    val swipeThreshold = 260f
    val rotation = (offsetX.value / 45f).coerceIn(-18f, 18f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate(rotation)
            .pointerInput(card.id) {
                detectTapGestures(
                    onDoubleTap = {
                        isDoubleTapHeartVisible = true
                        scope.launch {
                            offsetY.animateTo(-1400f, tween(400, easing = FastOutSlowInEasing))
                            onAction(CardAction.STRONG_PREFERENCE)
                        }
                    }
                )
            }
            .pointerInput(card.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > swipeThreshold) {
                                // Swiped Right: Like
                                offsetX.animateTo(1400f, tween(300, easing = FastOutSlowInEasing))
                                onAction(CardAction.LIKE)
                            } else if (offsetX.value < -swipeThreshold) {
                                // Swiped Left: Dislike
                                offsetX.animateTo(-1400f, tween(300, easing = FastOutSlowInEasing))
                                onAction(CardAction.DISLIKE)
                            } else {
                                // Snap back to center
                                launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                launch { offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                            launch { offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                        }
                    }
                )
            }
    ) {
        TinderCardView(
            card = card,
            isDark = isDark,
            isTopCard = true,
            isSuperLiked = isSuperLiked,
            isLiked = isLiked,
            isDisliked = isDisliked,
            modifier = Modifier.fillMaxSize()
        )

        // Visual Feedback Badges during Drag
        // 1. LIKE badge (green, top-left)
        val likeAlpha = (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
        if (likeAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .graphicsLayer { alpha = likeAlpha }
                    .rotate(-15f)
                    .border(3.dp, CompletedGreen, RoundedCornerShape(12.dp))
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CompletedGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "LIKE",
                        color = CompletedGreen,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }

        // 2. NOPE/PASS badge (red, top-right)
        val nopeAlpha = (-offsetX.value / swipeThreshold).coerceIn(0f, 1f)
        if (nopeAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .graphicsLayer { alpha = nopeAlpha }
                    .rotate(15f)
                    .border(3.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "PASS",
                        color = Color(0xFFEF4444),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }

        // 3. Double-tap super like pulsing heart
        if (isDoubleTapHeartVisible) {
            val scale by animateFloatAsState(
                targetValue = 1.3f,
                animationSpec = tween(300),
                label = "heart_pulse"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xDDE11D48)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Super Like",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
        }
    }
}

/**
 * Visual card layout showing the photo, gradient scrim, category pill, title, and description.
 */
@Composable
private fun TinderCardView(
    card: PreferenceCardItem,
    isDark: Boolean,
    isTopCard: Boolean,
    isSuperLiked: Boolean = false,
    isLiked: Boolean = false,
    isDisliked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isDark) CardBorderDark else CardBorderLight

    Card(
        modifier = modifier
            .shadow(if (isTopCard) 12.dp else 4.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) SurfaceDark else SurfaceLight),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            Image(
                painter = painterResource(id = card.imageRes),
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Scrim for crystal clear readability across bright and dark regions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x1A000000),
                                Color(0x88000000),
                                Color(0xF00A0A0A)
                            ),
                            startY = 220f
                        )
                    )
            )

            // Optional Top-Right Decision Badge if card was previously rated
            if (isSuperLiked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCCF43F5E))
                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "LOVE IT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            } else if (isLiked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC10B981))
                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "LIKED",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            } else if (isDisliked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCCEF4444))
                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "PASSED",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DmSansFontFamily
                        )
                    }
                }
            }

            // Card Text Details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Category Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Amber.copy(alpha = 0.25f))
                        .border(1.dp, Amber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = card.category.uppercase(),
                        color = AmberLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DmSansFontFamily,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = card.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlayfairFontFamily,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = card.description,
                    fontSize = 13.sp,
                    fontFamily = DmSansFontFamily,
                    color = Color.White.copy(alpha = 0.90f),
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Bottom action controls: Undo, Dislike (X), Super Like (Heart), and Like (Check).
 * Centered and evenly spaced with per-card selected states derived from currentCard.id.
 */
@Composable
private fun BottomActionControls(
    canUndo: Boolean,
    isActionInProgress: Boolean,
    isSuperLiked: Boolean,
    isLiked: Boolean,
    isDisliked: Boolean,
    onUndo: () -> Unit,
    onDislike: () -> Unit,
    onSuperLike: () -> Unit,
    onLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Undo Button
        IconButton(
            onClick = onUndo,
            enabled = canUndo && !isActionInProgress,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (canUndo) Amber.copy(alpha = 0.12f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (canUndo) Amber.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.25f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.RotateLeft,
                contentDescription = "Undo",
                tint = if (canUndo) Amber else Color.Gray.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp)
            )
        }

        // 2. Dislike (X) Button
        IconButton(
            onClick = onDislike,
            enabled = !isActionInProgress,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isDisliked) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.12f)
                )
                .border(
                    width = if (isDisliked) 2.dp else 1.5.dp,
                    color = if (isDisliked) Color.White else Color(0xFFEF4444).copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Pass",
                tint = if (isDisliked) Color.White else Color(0xFFEF4444),
                modifier = Modifier.size(26.dp)
            )
        }

        // 3. Super Like (Heart) Button - Prominent centerpiece!
        IconButton(
            onClick = onSuperLike,
            enabled = !isActionInProgress,
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = if (isSuperLiked) 12.dp else 6.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFFE11D48).copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF43F5E),
                            Color(0xFFE11D48),
                            Color(0xFFBE123C)
                        )
                    )
                )
                .border(
                    width = if (isSuperLiked) 3.dp else 1.5.dp,
                    color = if (isSuperLiked) Color.White else Color.White.copy(alpha = 0.45f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Love it",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // 4. Like (Check) Button
        IconButton(
            onClick = onLike,
            enabled = !isActionInProgress,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isLiked) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.12f)
                )
                .border(
                    width = if (isLiked) 2.dp else 1.5.dp,
                    color = if (isLiked) Color.White else Color(0xFF10B981).copy(alpha = 0.6f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Like",
                tint = if (isLiked) Color.White else Color(0xFF10B981),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

/**
 * Finished deck view displayed when all 12 cards have been completed.
 */
@Composable
private fun FinishedDeckView(
    uiState: OnboardingUiState,
    isDark: Boolean,
    onContinue: () -> Unit
) {
    val surfaceColor = if (isDark) SurfaceDark else SurfaceLight
    val onSurfaceColor = if (isDark) OnBackgroundWhite else OnBackgroundDark
    val mutedColor = if (isDark) OnBackgroundGray else OnBackgroundMedium
    val borderColor = if (isDark) CardBorderDark else CardBorderLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Amber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Preferences Saved!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlayfairFontFamily,
                color = onSurfaceColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your travel tastes have been recorded. We'll use these to customize your AI recommendations and personalized itineraries.",
                fontSize = 14.sp,
                fontFamily = DmSansFontFamily,
                color = mutedColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Summary of liked vibes
            if (uiState.likedInterestIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "YOUR FAVORITE VIBES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = DmSansFontFamily,
                    color = AmberLight,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val displayed = uiState.likedInterestIds.take(4).map { id ->
                        uiState.deck.firstOrNull { it.id == id }?.title ?: id
                    }
                    Text(
                        text = displayed.joinToString(" • "),
                        fontSize = 13.sp,
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Start Planning",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = DmSansFontFamily
                )
            }
        }
    }
}
