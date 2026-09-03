package com.example.smarttravelplanner.ui.screens.auth

sealed class AuthUiState {
    object Idle : AuthUiState()
    object LoggingIn : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
