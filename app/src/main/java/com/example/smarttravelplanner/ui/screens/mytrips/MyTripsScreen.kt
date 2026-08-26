package com.example.smarttravelplanner.ui.screens.mytrips

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MyTripsScreen(viewModel: MyTripsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is MyTripsUiState.Loading -> CircularProgressIndicator()
            is MyTripsUiState.Success -> {
                if (state.trips.isEmpty()) {
                    Text("No trips yet. Start planning!")
                } else {
                    Text("${state.trips.size} trip(s)")
                }
            }
            is MyTripsUiState.Error -> Text("Error: ${state.message}")
        }
    }
}
