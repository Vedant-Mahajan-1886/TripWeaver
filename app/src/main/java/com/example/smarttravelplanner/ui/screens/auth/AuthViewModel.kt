package com.example.smarttravelplanner.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttravelplanner.data.local.seed.DemoAgentSeeder
import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.AuthRepository
import com.example.smarttravelplanner.data.repository.UserRoleRepository
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository,
    private val agentRepository: AgentRepository,
    private val demoAgentSeeder: DemoAgentSeeder,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    init {
        // Ready-made agent accounts for demos (no-op if they already exist)
        viewModelScope.launch { runCatching { demoAgentSeeder.seedIfNeeded() } }
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Observes persistent login session across app runs.
     * initialValue is null to distinguish cold-start loading from an actual false.
     */
    val isLoggedIn: StateFlow<Boolean?> = authRepository.isLoggedIn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Role of the logged-in account, or null while it's still loading / nobody is logged in.
     * Used at app start to open Home (traveller) or the Agent Dashboard (agent).
     */
    val sessionRole: StateFlow<UserRole?> = combine(
        authRepository.isLoggedIn,
        userRoleRepository.currentUserRole
    ) { loggedIn, role -> if (loggedIn) role else null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /**
     * Account-specific onboarding status for the currently authenticated user.
     * Null if nobody is logged in or while loading.
     * Automatically triggers idempotent legacy account migration for older users.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionOnboardingStatus: StateFlow<OnboardingStatus?> = combine(
        authRepository.isLoggedIn,
        authRepository.currentUserEmail
    ) { loggedIn, email ->
        if (loggedIn && !email.isNullOrBlank()) email else null
    }.flatMapLatest { email ->
        if (email == null) {
            flowOf<OnboardingStatus?>(null)
        } else {
            userPreferencesRepository.getTravelPreferences(email)
                .map<com.example.smarttravelplanner.domain.model.UserTravelPreferences, OnboardingStatus?> { it.onboardingStatus }
                .onStart {
                    userPreferencesRepository.migrateLegacyAccountIfNeeded(email)
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
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
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            if (userRoleRepository.roleForEmail(normalizedEmail) == UserRole.AGENT) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "This is a travel agent account. Tap \"Agent account\" to sign in."
                    )
                }
                return@launch
            }
            authRepository.login(normalizedEmail, password)
                .onSuccess { authResult ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            sessionRole = UserRole.TRAVELLER,
                            isNewAccount = authResult.isNewAccount
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to sign in. Please check your credentials."
                        )
                    }
                }
        }
    }

    /**
     * Requests a 6-digit email verification OTP before account creation.
     */
    fun requestRegistrationOtp(fullName: String, email: String, password: String) {
        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your full name.") }
            return
        }
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            authRepository.sendRegistrationOtp(normalizedEmail)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            viewMode = AuthViewMode.RegistrationOtp(normalizedEmail),
                            errorMessage = null
                        )
                    }
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to send verification code. Please try again."
                        )
                    }
                }
        }
    }

    /**
     * Resends the OTP code if cooldown is finished.
     */
    fun resendOtp(email: String) {
        if (_resendCountdown.value > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            authRepository.sendRegistrationOtp(normalizedEmail)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to resend code."
                        )
                    }
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
            _uiState.update { it.copy(errorMessage = "Please enter the complete 6-digit verification code.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            authRepository.verifyOtpAndCreateAccount(fullName, normalizedEmail, password, otp)
                .onSuccess { authResult ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            sessionRole = UserRole.TRAVELLER,
                            isNewAccount = authResult.isNewAccount
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Verification failed. Please check the code."
                        )
                    }
                }
        }
    }

    /**
     * Google Sign-In flow.
     */
    fun loginWithGoogle(
        email: String = "google.traveler@gmail.com",
        fullName: String = "Google Traveler",
        profilePictureUrl: String? = null,
        idToken: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            if (userRoleRepository.roleForEmail(normalizedEmail) == UserRole.AGENT) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "This Google account is registered as a travel agent. Use \"Agent account\" to sign in."
                    )
                }
                return@launch
            }
            authRepository.loginWithGoogle(normalizedEmail, fullName, profilePictureUrl)
                .onSuccess { authResult ->
                    if (idToken != null) authRepository.connectGoogleToCloud(normalizedEmail, idToken)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            sessionRole = UserRole.TRAVELLER,
                            isNewAccount = authResult.isNewAccount
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Google sign-in failed."
                        )
                    }
                }
        }
    }

    /**
     * Clears active session and logs out. Resets UI state synchronously so
     * destination screens never receive stale isSuccess == true.
     */
    fun logout() {
        _uiState.value = AuthUiState()
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    /**
     * Cancels OTP view and returns to the registration form.
     */
    fun cancelOtp() {
        countdownJob?.cancel()
        _resendCountdown.value = 0
        _uiState.update {
            it.copy(
                viewMode = AuthViewMode.StandardForm,
                errorMessage = null,
                isLoading = false
            )
        }
    }

    /**
     * Initiates the forgot password flow.
     */
    fun startForgotPassword(initialEmail: String = "") {
        _uiState.update {
            it.copy(
                viewMode = AuthViewMode.ForgotPasswordEmail(initialEmail.trim()),
                errorMessage = null,
                isLoading = false
            )
        }
    }

    /**
     * Sends password reset OTP to the user's email.
     */
    fun sendPasswordResetOtp(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your registered email.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            authRepository.sendPasswordResetOtp(normalizedEmail)
                .onSuccess {
                    if (authRepository.passwordResetByEmailLink) {
                        // Cloud accounts: Firebase emailed a link; back to sign-in with a notice
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                viewMode = AuthViewMode.StandardForm,
                                errorMessage = null,
                                infoMessage = "If an account exists for $normalizedEmail, we've emailed a link to " +
                                    "reset your password. Open it, choose a new password, then sign in here."
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                viewMode = AuthViewMode.ForgotPasswordOtp(normalizedEmail),
                                errorMessage = null
                            )
                        }
                        startResendCooldown()
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to send reset code. Please check your email."
                        )
                    }
                }
        }
    }

    /**
     * Resends password reset OTP if cooldown finished.
     */
    fun resendPasswordResetOtp(email: String) {
        if (_resendCountdown.value > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            authRepository.sendPasswordResetOtp(normalizedEmail)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to resend code."
                        )
                    }
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
            _uiState.update { it.copy(errorMessage = "Please enter the complete 6-digit verification code.") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            authRepository.resetPassword(normalizedEmail, otp, newPassword)
                .onSuccess { authResult ->
                    // Resetting signs the user in: send agents to their dashboard
                    val role = userRoleRepository.roleForEmail(normalizedEmail) ?: UserRole.TRAVELLER
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            sessionRole = role,
                            isNewAccount = authResult.isNewAccount
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to reset password. Please check the code."
                        )
                    }
                }
        }
    }

    /**
     * Cancels forgot password flow and returns to standard login.
     */
    fun cancelForgotPassword() {
        countdownJob?.cancel()
        _resendCountdown.value = 0
        _uiState.update {
            it.copy(
                viewMode = AuthViewMode.StandardForm,
                errorMessage = null,
                isLoading = false
            )
        }
    }

    // ── Travel-agent accounts ────────────────────────────────────────────────

    /** Opens the agent sign-in view (from "Agent account" on the sign-in form). */
    fun startAgentSignIn() {
        countdownJob?.cancel()
        _resendCountdown.value = 0
        _uiState.update {
            it.copy(viewMode = AuthViewMode.AgentSignIn, errorMessage = null, isLoading = false)
        }
    }

    fun startAgentSignUp() {
        _uiState.update {
            it.copy(viewMode = AuthViewMode.AgentSignUp, errorMessage = null, isLoading = false)
        }
    }

    /** Leaves the agent views and returns to the normal traveller sign-in. */
    fun cancelAgent() {
        countdownJob?.cancel()
        _resendCountdown.value = 0
        _uiState.update {
            it.copy(viewMode = AuthViewMode.StandardForm, errorMessage = null, isLoading = false)
        }
    }

    /** Signs in an agent. Traveller accounts are refused here. */
    fun agentLogin(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            when (userRoleRepository.roleForEmail(normalizedEmail)) {
                null -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "No agent account found for this email.")
                    }
                    return@launch
                }
                UserRole.TRAVELLER -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "This is a traveller account. Go back and sign in normally."
                        )
                    }
                    return@launch
                }
                UserRole.AGENT -> Unit
            }
            authRepository.login(normalizedEmail, password)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, isSuccess = true, sessionRole = UserRole.AGENT, isNewAccount = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to sign in. Please check your credentials."
                        )
                    }
                }
        }
    }

    /** Validates the agent sign-up form and emails a verification code. */
    fun requestAgentRegistrationOtp(
        fullName: String,
        email: String,
        password: String,
        agencyName: String
    ) {
        if (agencyName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your agency name.") }
            return
        }
        if (fullName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your full name.") }
            return
        }
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            authRepository.sendRegistrationOtp(normalizedEmail)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            viewMode = AuthViewMode.AgentRegistrationOtp(normalizedEmail),
                            errorMessage = null
                        )
                    }
                    startResendCooldown()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to send verification code. Please try again."
                        )
                    }
                }
        }
    }

    /** Verifies the code, creates the account as an AGENT and saves the agency profile. */
    fun verifyAgentOtpAndCreateAccount(
        fullName: String,
        email: String,
        password: String,
        otp: String,
        agencyName: String,
        city: String
    ) {
        if (otp.length != 6) {
            _uiState.update { it.copy(errorMessage = "Please enter the complete 6-digit verification code.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val normalizedEmail = email.trim().lowercase()
            val result = authRepository.verifyOtpAndCreateAccount(fullName, normalizedEmail, password, otp)
                .mapCatching {
                    userRoleRepository.setCurrentUserRole(UserRole.AGENT)
                    agentRepository.saveMyAgentProfile(agencyName, city, emptyList()).getOrThrow()
                }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, isSuccess = true, sessionRole = UserRole.AGENT, isNewAccount = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Verification failed. Please check the code."
                        )
                    }
                }
        }
    }

    /** From the agent OTP screen back to the agent sign-up form. */
    fun backToAgentSignUp() {
        countdownJob?.cancel()
        _resendCountdown.value = 0
        _uiState.update {
            it.copy(viewMode = AuthViewMode.AgentSignUp, errorMessage = null, isLoading = false)
        }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }

    /** Completes preferences onboarding and saves personalized selections. */
    fun completeOnboarding(
        email: String,
        liked: Set<String>,
        stronglyPreferred: Set<String> = emptySet(),
        disliked: Set<String> = emptySet(),
        weights: Map<String, Float> = emptyMap()
    ) {
        viewModelScope.launch {
            userPreferencesRepository.updateTravelPreferences(
                email = email,
                liked = liked,
                stronglyPreferred = stronglyPreferred,
                disliked = disliked,
                weights = weights
            )
            userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)
        }
    }

    /** Skips preferences onboarding for the specified account without forcing it again. */
    fun skipOnboarding(email: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.SKIPPED)
        }
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
