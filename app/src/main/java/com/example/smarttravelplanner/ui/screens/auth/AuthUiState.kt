package com.example.smarttravelplanner.ui.screens.auth

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val email: String) : AuthUiState()
    data class ForgotPasswordEmail(val initialEmail: String = "") : AuthUiState()
    data class ForgotPasswordOtp(val email: String) : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
