package com.example.smarttravelplanner.ui.screens.auth

sealed interface AuthViewMode {
    data object StandardForm : AuthViewMode
    data class RegistrationOtp(val email: String) : AuthViewMode
    data class ForgotPasswordEmail(val initialEmail: String = "") : AuthViewMode
    data class ForgotPasswordOtp(val email: String) : AuthViewMode
}

data class AuthUiState(
    val viewMode: AuthViewMode = AuthViewMode.StandardForm,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
