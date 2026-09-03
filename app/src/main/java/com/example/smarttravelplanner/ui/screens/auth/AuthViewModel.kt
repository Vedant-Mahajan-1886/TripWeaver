package com.example.smarttravelplanner.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /** Observes persistent login session across app runs */
    val isLoggedIn = authRepository.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    /** Countdown timer in seconds for OTP resend cooldown */
    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown = _resendCountdown.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Signs in an existing user with email and password.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.login(email, password)
                .onSuccess {
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to sign in. Please check your credentials."
                    )
                }
        }
    }

    /**
     * Requests a 6-digit email verification OTP before account creation.
     */
    fun requestRegistrationOtp(fullName: String, email: String, password: String) {
        if (fullName.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your full name.")
            return
        }
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email.")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.sendRegistrationOtp(email)
                .onSuccess {
                    _uiState.value = AuthUiState.OtpSent(email.trim())
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to send verification code. Please try again."
                    )
                }
        }
    }

    /**
     * Resends the OTP code if cooldown is finished.
     */
    fun resendOtp(email: String) {
        if (_resendCountdown.value > 0) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.sendRegistrationOtp(email)
                .onSuccess {
                    _uiState.value = AuthUiState.OtpSent(email.trim())
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to resend code."
                    )
                }
        }
    }

    /**
     * Verifies the 6-digit OTP code and completes registration in Room DB.
     */
    fun verifyOtpAndCreateAccount(
        fullName: String,
        email: String,
        password: String,
        otp: String
    ) {
        if (otp.length != 6) {
            _uiState.value = AuthUiState.Error("Please enter the complete 6-digit verification code.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.verifyOtpAndCreateAccount(fullName, email, password, otp)
                .onSuccess {
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Verification failed. Please check the code."
                    )
                }
        }
    }

    /**
     * Google Sign-In flow.
     */
    fun loginWithGoogle(
        email: String = "google.traveler@gmail.com",
        fullName: String = "Google Traveler"
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginWithGoogle(email, fullName)
                .onSuccess {
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Google sign-in failed.")
                }
        }
    }

    /**
     * Clears active session and logs out.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Cancels OTP view and returns to the registration form.
     */
    fun cancelOtp() {
        countdownJob?.cancel()
        _resendCountdown.value = 0
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Initiates the forgot password flow.
     */
    fun startForgotPassword(initialEmail: String = "") {
        _uiState.value = AuthUiState.ForgotPasswordEmail(initialEmail.trim())
    }

    /**
     * Sends password reset OTP to the user's email.
     */
    fun sendPasswordResetOtp(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your registered email.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.sendPasswordResetOtp(email)
                .onSuccess {
                    _uiState.value = AuthUiState.ForgotPasswordOtp(email.trim())
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to send reset code. Please check your email."
                    )
                }
        }
    }

    /**
     * Resends password reset OTP if cooldown finished.
     */
    fun resendPasswordResetOtp(email: String) {
        if (_resendCountdown.value > 0) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.sendPasswordResetOtp(email)
                .onSuccess {
                    _uiState.value = AuthUiState.ForgotPasswordOtp(email.trim())
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to resend code.")
                }
        }
    }

    /**
     * Verifies OTP and updates the user's password in Room DB.
     */
    fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ) {
        if (otp.length != 6) {
            _uiState.value = AuthUiState.Error("Please enter the complete 6-digit verification code.")
            return
        }
        if (newPassword.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = AuthUiState.Error("Passwords do not match.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.resetPassword(email, otp, newPassword)
                .onSuccess {
                    _uiState.value = AuthUiState.Success
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to reset password. Please check the code."
                    )
                }
        }
    }

    /**
     * Cancels forgot password flow and returns to standard login.
     */
    fun cancelForgotPassword() {
        countdownJob?.cancel()
        _resendCountdown.value = 0
        _uiState.value = AuthUiState.Idle
    }

    fun setError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun startResendCooldown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _resendCountdown.value = 30
            while (_resendCountdown.value > 0) {
                delay(1000)
                _resendCountdown.value -= 1
            }
        }
    }
}
