package com.example.smarttravelplanner.ui.screens.auth

import com.example.smarttravelplanner.domain.model.UserRole

sealed interface AuthViewMode {
    data object StandardForm : AuthViewMode
    data class RegistrationOtp(val email: String) : AuthViewMode
    data class ForgotPasswordEmail(val initialEmail: String = "") : AuthViewMode
    data class ForgotPasswordOtp(val email: String) : AuthViewMode

    // ── Travel-agent accounts (opened from "Agent account" on the sign-in form) ──
    data object AgentSignIn : AuthViewMode
    data object AgentSignUp : AuthViewMode
    data class AgentRegistrationOtp(val email: String) : AuthViewMode
}

data class AuthUiState(
    val viewMode: AuthViewMode = AuthViewMode.StandardForm,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    /** Role of the account that just signed in; decides Home vs Agent Dashboard. */
    val sessionRole: UserRole = UserRole.TRAVELLER,
    /** True if this authentication operation created a brand-new account (eligible for onboarding). */
    val isNewAccount: Boolean = false
)
