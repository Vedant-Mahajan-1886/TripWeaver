package com.example.smarttravelplanner.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> CircularProgressIndicator()
            is ProfileUiState.Success -> Text("Profile — ${state.preferences.name.ifEmpty { "Set your name" }}")
            is ProfileUiState.Error -> Text("Error: ${state.message}")
        }
    }
}
