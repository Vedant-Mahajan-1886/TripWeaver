package com.example.smarttravelplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarttravelplanner.domain.model.UserPreferences
import com.example.smarttravelplanner.navigation.AppNavigation
import com.example.smarttravelplanner.ui.screens.profile.ProfileUiState
import com.example.smarttravelplanner.ui.screens.profile.ProfileViewModel
import com.example.smarttravelplanner.ui.theme.SmartTravelPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Get the Activity-scoped ProfileViewModel — hiltViewModel() here is
            // Activity-scoped because setContent runs before any NavHost.
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

            val themeMode = when (val state = profileState) {
                is ProfileUiState.Success -> state.preferences.themeMode
                else                     -> UserPreferences().themeMode  // default: "Device Theme"
            }

            SmartTravelPlannerTheme(themeMode = themeMode) {
                // Pass the same ViewModel so ProfileScreen writes to the same instance
                AppNavigation(profileViewModel = profileViewModel)
            }
        }
    }
}