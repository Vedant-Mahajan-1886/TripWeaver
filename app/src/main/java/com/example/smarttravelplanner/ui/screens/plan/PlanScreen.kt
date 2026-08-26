package com.example.smarttravelplanner.ui.screens.plan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlanScreen(viewModel: PlanViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is PlanUiState.Idle -> Text("Plan a new trip")
            is PlanUiState.GeneratingItinerary -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating your itinerary with AI...")
                }
            }
            is PlanUiState.ItineraryGenerated -> Text(state.content)
            is PlanUiState.Error -> Text("Error: ${state.message}")
        }
    }
}
