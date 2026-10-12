package com.example.smarttravelplanner.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.UserEntity
import com.example.smarttravelplanner.data.remote.service.EmailVerificationService
import com.example.smarttravelplanner.domain.model.AuthResult
import com.example.smarttravelplanner.util.SecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val emailVerificationService: EmailVerificationService,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataStore: DataStore<Preferences>
) : AuthRepository {

    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("auth_is_logged_in")
        val LOGGED_IN_EMAIL = stringPreferencesKey("auth_logged_in_email")
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        fun isValidEmail(email: String): Boolean = email.isNotBlank() && EMAIL_REGEX.matches(email)
    }

    override val isLoggedIn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.IS_LOGGED_IN] ?: false
    }

    override val currentUserEmail: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.LOGGED_IN_EMAIL]
    }

    private suspend fun syncUserProfile(
        fullName: String,
        email: String,
        profilePictureUrl: String? = null,
        isGoogleVerified: Boolean? = null
    ) {
        val currentPrefs = userPreferencesRepository.userPreferences.first()
        val names = fullName.trim().split(" ", limit = 2)
        val fName = names.getOrNull(0) ?: ""
        val lName = names.getOrNull(1) ?: ""
        userPreferencesRepository.update(
            currentPrefs.copy(
                firstName = fName,
                lastName = lName,
                fullNameInput = fullName,
                email = email,
                profilePictureUrl = profilePictureUrl ?: currentPrefs.profilePictureUrl,
                isGoogleVerified = isGoogleVerified ?: currentPrefs.isGoogleVerified
            )
        )
    }

    override suspend fun sendRegistrationOtp(email: String): Result<Unit> = runCatching {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || !isValidEmail(trimmedEmail)) {
            throw IllegalArgumentException("Please enter a valid email address.")
        }

        // Check duplicate email
        val exists = userDao.emailExists(trimmedEmail)
        if (exists > 0) {
            throw IllegalArgumentException("An account with this email already exists. Please Sign In.")
        }

        emailVerificationService.sendVerificationOtp(trimmedEmail).getOrThrow()
    }

    override suspend fun verifyOtpAndCreateAccount(
        fullName: String,
        email: String,
        password: String,
        otp: String
    ): Result<AuthResult> = runCatching {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedName.isBlank()) {
            throw IllegalArgumentException("Full name cannot be empty.")
        }
        if (!isValidEmail(trimmedEmail)) {
            throw IllegalArgumentException("Please enter a valid email address.")
        }
        if (password.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters.")
        }

        // Verify OTP code
        emailVerificationService.verifyOtp(trimmedEmail, otp).getOrThrow()

        // Salt and hash password
        val salt = SecurityUtils.generateSalt()
        val passwordHash = SecurityUtils.hashPassword(password, salt)

        // Save to Room DB
        val newUser = UserEntity(
            email = trimmedEmail,
            fullName = trimmedName,
            passwordHash = passwordHash,
            salt = salt,
            isEmailVerified = true
        )
        userDao.insertUser(newUser)

        // Update profile in UserPreferences
        syncUserProfile(trimmedName, trimmedEmail)

        // Initialize fresh onboarding state for this new account
        userPreferencesRepository.initializeNewAccountOnboarding(trimmedEmail)

        // Set session
        dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.LOGGED_IN_EMAIL] = trimmedEmail
        }

        AuthResult(email = trimmedEmail, isNewAccount = true)
    }

    override suspend fun login(email: String, password: String): Result<AuthResult> = runCatching {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank()) {
            throw IllegalArgumentException("Please enter your email.")
        }
        if (password.isBlank()) {
            throw IllegalArgumentException("Please enter your password.")
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: throw IllegalArgumentException("No account found with this email. Please Create Account.")

        if (user.isGoogleUser) {
            throw IllegalArgumentException("This account is linked with Google. Please tap 'Continue with Google'.")
        }

        val passwordValid = SecurityUtils.verifyPassword(password, user.salt, user.passwordHash)
        if (!passwordValid) {
            throw IllegalArgumentException("Incorrect password. Please try again.")
        }

        // Sync name & email with UserPreferences
        syncUserProfile(user.fullName, user.email)

        // Migrate legacy account if this returning user has no onboarding record yet
        userPreferencesRepository.migrateLegacyAccountIfNeeded(user.email)

        // Set session
        dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.LOGGED_IN_EMAIL] = user.email
        }

        AuthResult(email = user.email, isNewAccount = false)
    }

    override suspend fun loginWithGoogle(
        email: String,
        fullName: String,
        profilePictureUrl: String?
    ): Result<AuthResult> = runCatching {
        val googleEmail = email.trim().lowercase()
        val googleName = fullName.trim().ifBlank { "Google Traveler" }

        // Determine whether user existed before authentication
        val existing = userDao.getUserByEmail(googleEmail)
        val isNew = (existing == null)

        if (isNew) {
            val salt = SecurityUtils.generateSalt()
            userDao.insertUser(
                UserEntity(
                    email = googleEmail,
                    fullName = googleName,
                    passwordHash = SecurityUtils.hashPassword("google_oauth_token", salt),
                    salt = salt,
                    isEmailVerified = true,
                    isGoogleUser = true,
                    profilePictureUrl = profilePictureUrl
                )
            )
            // Initialize fresh onboarding state for new Google traveller
            userPreferencesRepository.initializeNewAccountOnboarding(googleEmail)
        } else {
            if (!profilePictureUrl.isNullOrBlank()) {
                userDao.updateProfilePicture(googleEmail, profilePictureUrl)
            }
            // Migrate legacy account if no onboarding record exists yet
            userPreferencesRepository.migrateLegacyAccountIfNeeded(googleEmail)
        }

        syncUserProfile(
            fullName = googleName,
            email = googleEmail,
            profilePictureUrl = profilePictureUrl,
            isGoogleVerified = true
        )

        dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.LOGGED_IN_EMAIL] = googleEmail
        }

        AuthResult(email = googleEmail, isNewAccount = isNew)
    }

    override suspend fun sendPasswordResetOtp(email: String): Result<Unit> = runCatching {
        val trimmedEmail = email.trim().lowercase()
        if (trimmedEmail.isBlank() || !isValidEmail(trimmedEmail)) {
            throw IllegalArgumentException("Please enter a valid email address.")
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: throw IllegalArgumentException("No account registered with this email address.")

        if (user.isGoogleUser) {
            throw IllegalArgumentException("This account is linked with Google. Please tap 'Continue with Google' to sign in.")
        }

        emailVerificationService.sendPasswordResetOtp(trimmedEmail).getOrThrow()
    }

    override suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String
    ): Result<AuthResult> = runCatching {
        val trimmedEmail = email.trim().lowercase()

        if (!isValidEmail(trimmedEmail)) {
            throw IllegalArgumentException("Please enter a valid email address.")
        }
        if (newPassword.length < 6) {
            throw IllegalArgumentException("New password must be at least 6 characters.")
        }

        // 1. Verify OTP code
        emailVerificationService.verifyOtp(trimmedEmail, otp).getOrThrow()

        // 2. Fetch user
        val user = userDao.getUserByEmail(trimmedEmail)
            ?: throw IllegalArgumentException("User account not found.")

        // 3. Salt and hash new password
        val salt = SecurityUtils.generateSalt()
        val passwordHash = SecurityUtils.hashPassword(newPassword, salt)

        // 4. Update in Room DB
        userDao.updatePassword(trimmedEmail, passwordHash, salt)

        // 5. Sync profile and start session
        syncUserProfile(user.fullName, user.email)

        // Migrate legacy account if needed
        userPreferencesRepository.migrateLegacyAccountIfNeeded(user.email)

        dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.LOGGED_IN_EMAIL] = user.email
        }

        AuthResult(email = user.email, isNewAccount = false)
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        val currentEmail = dataStore.data.first()[Keys.LOGGED_IN_EMAIL]
        if (!currentEmail.isNullOrBlank()) {
            userPreferencesRepository.clearAccountState(currentEmail)
        }
        dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.LOGGED_IN_EMAIL)
        }
    }
}
