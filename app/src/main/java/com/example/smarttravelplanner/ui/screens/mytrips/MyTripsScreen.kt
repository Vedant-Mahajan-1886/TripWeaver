package com.example.smarttravelplanner.ui.screens.mytrips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.ui.components.DestinationTripCard
import com.example.smarttravelplanner.ui.theme.*

/**
 * Screen displaying the user's saved and planned trips.
 *
 * Uses the modular [DestinationTripCard] to render full-bleed destination cards
 * with deletion support and click handling.
 */
@Composable
fun MyTripsScreen(
    viewModel: MyTripsViewModel = hiltViewModel(),
    onTripClick: (Long) -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrollOffsetProvider: () -> Float = remember {
        {
            if (listState.firstVisibleItemIndex > 0) {
                1000f
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is MyTripsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Amber)
                }
            }
            is MyTripsUiState.Empty -> {
                EmptyTripsState(modifier = Modifier.padding(top = 64.dp))
            }
            is MyTripsUiState.Success -> {
                if (state.trips.isEmpty()) {
                    EmptyTripsState(modifier = Modifier.padding(top = 64.dp))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 40.dp, start = 20.dp, end = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(68.dp))
                        }

                        items(state.trips, key = { it.id }) { trip ->
                            DestinationTripCard(
                                trip = trip,
                                statusText = "Planning",
                                onClick = { onTripClick(trip.id) },
                                onDelete = { viewModel.deleteTrip(trip.id) }
                            )
                        }
                    }
                }
            }
            is MyTripsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DmSansFontFamily
                    )
                }
            }
        }

        // ── Pinned TopBar with Hamburger & Right-Aligned "My Trips" Title ──
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(64.dp)
        ) {
            // Cloudy soft gradient backdrop (fades in smoothly as user scrolls)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val offset = scrollOffsetProvider()
                        val topBarAlpha = (offset / 90f).coerceIn(0f, 1f)
                        alpha = topBarAlpha
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.82f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // TopBar Controls
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hamburger Menu Button (Top-Left, always pinned and clickable)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CircleShape)
                        .clickable { onMenuClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
                val tripsColor = if (isLightMode) AmberDark else Amber

                // Right-aligned "My Trips" title (always visible from top, styled with premium typography)
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            append("My ")
                        }
                        withStyle(
                            SpanStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 28.sp,
                                color = tripsColor
                            )
                        ) {
                            append("Trips")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Placeholder displayed when the user has not planned or saved any trips yet.
 */
@Composable
private fun EmptyTripsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Amber.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Luggage,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No Trips Yet",
                fontFamily = PlayfairFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use AI to plan your dream vacation and your saved itineraries will show up here.",
                fontFamily = DmSansFontFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
