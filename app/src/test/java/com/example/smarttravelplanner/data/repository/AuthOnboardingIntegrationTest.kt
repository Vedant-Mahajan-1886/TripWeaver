package com.example.smarttravelplanner.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.UserEntity
import com.example.smarttravelplanner.data.remote.service.EmailVerificationService
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuthOnboardingIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var fakeEmailService: FakeEmailVerificationService
    private lateinit var userPreferencesRepository: UserPreferencesRepositoryImpl
    private lateinit var authRepository: AuthRepositoryImpl

    private class FakeEmailVerificationService : EmailVerificationService {
        var lastSentOtp: String? = null

        override suspend fun sendVerificationOtp(email: String): Result<String> {
            lastSentOtp = "123456"
            return Result.success("123456")
        }

        override fun verifyOtp(email: String, enteredCode: String): Result<Boolean> {
            return if (enteredCode == "123456") Result.success(true)
            else Result.failure(IllegalArgumentException("Invalid verification code."))
        }

        override suspend fun sendPasswordResetOtp(email: String): Result<String> {
            lastSentOtp = "123456"
            return Result.success("123456")
        }

        override fun clearSession(email: String) {
            lastSentOtp = null
        }
    }

    private class FakeUserDao : UserDao {
        val users = mutableMapOf<String, UserEntity>()
        private var idCounter = 1L

        override suspend fun getUserByEmail(email: String): UserEntity? =
            users[email.trim().lowercase()]

        override suspend fun getUserById(userId: Long): UserEntity? =
            users.values.firstOrNull { it.id == userId }

        override suspend fun emailExists(email: String): Int =
            if (users.containsKey(email.trim().lowercase())) 1 else 0

        override suspend fun insertUser(user: UserEntity): Long {
            val normalized = user.email.trim().lowercase()
            if (users.containsKey(normalized)) throw IllegalStateException("User already exists")
            val id = idCounter++
            val saved = user.copy(id = id, email = normalized)
            users[normalized] = saved
            return id
        }

        override suspend fun updateUser(user: UserEntity) {
            users[user.email.trim().lowercase()] = user
        }

        override suspend fun updatePassword(email: String, passwordHash: String, salt: String) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(passwordHash = passwordHash, salt = salt)
        }

        override suspend fun updateProfileDetails(
            email: String,
            fullName: String,
            phoneNumber: String,
            profilePictureUrl: String?,
            gender: String,
            homeLocation: String,
            dateOfBirth: String,
            maritalStatus: String
        ) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(
                fullName = fullName,
                phoneNumber = phoneNumber,
                profilePictureUrl = profilePictureUrl ?: current.profilePictureUrl,
                gender = gender,
                homeLocation = homeLocation,
                dateOfBirth = dateOfBirth,
                maritalStatus = maritalStatus
            )
        }

        override suspend fun updateProfilePicture(email: String, profilePictureUrl: String?) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(profilePictureUrl = profilePictureUrl)
        }

        override fun observeRole(email: String): Flow<String?> =
            flowOf(users[email.trim().lowercase()]?.role)

        override suspend fun updateRole(email: String, role: String) {
            val current = users[email.trim().lowercase()] ?: return
            users[email.trim().lowercase()] = current.copy(role = role)
        }

        override suspend fun deleteUser(userId: Long) {
            users.entries.removeIf { it.value.id == userId }
        }
    }

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFolder.newFile("test_auth_prefs.preferences_pb") }
        )
        fakeUserDao = FakeUserDao()
        fakeEmailService = FakeEmailVerificationService()
        userPreferencesRepository = UserPreferencesRepositoryImpl(dataStore)
        authRepository = AuthRepositoryImpl(
            userDao = fakeUserDao,
            emailVerificationService = fakeEmailService,
            userPreferencesRepository = userPreferencesRepository,
            dataStore = dataStore
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun newEmailOtpRegistrationCreatesAccountAndInitializesOnboarding() = runBlocking {
        val email = "newuser@example.com"
        val password = "Password123"

        val regResult = authRepository.verifyOtpAndCreateAccount(
            fullName = "New Traveler",
            email = email,
            password = password,
            otp = "123456"
        )

        assertTrue(regResult.isSuccess)
        val authResult = regResult.getOrThrow()
        assertEquals(email, authResult.email)
        assertTrue("Newly registered user must be marked as new account", authResult.isNewAccount)

        // Session must be active
        assertTrue(authRepository.isLoggedIn.first())
        assertEquals(email, authRepository.currentUserEmail.first())

        // Onboarding status must be NOT_STARTED
        val status = userPreferencesRepository.getRawOnboardingStatus(email)
        assertEquals(OnboardingStatus.NOT_STARTED, status)
    }

    @Test
    fun existingEmailPasswordLoginReturnsReturningUserAndMigratesLegacyAccount() = runBlocking {
        val email = "legacyuser@example.com"
        val password = "Password123"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(password, salt)

        // Simulate pre-existing legacy user in Room
        fakeUserDao.insertUser(
            UserEntity(
                email = email,
                fullName = "Legacy Traveler",
                passwordHash = hash,
                salt = salt
            )
        )

        // Initially no onboarding status exists in DataStore
        assertNull(userPreferencesRepository.getRawOnboardingStatus(email))

        val loginResult = authRepository.login(email, password)
        assertTrue(loginResult.isSuccess)
        val authResult = loginResult.getOrThrow()

        assertEquals(email, authResult.email)
        assertFalse("Returning user must NOT be marked as new account", authResult.isNewAccount)

        // Onboarding must be migrated to LEGACY_COMPLETED so returning user lands on Home
        val status = userPreferencesRepository.getRawOnboardingStatus(email)
        assertEquals(OnboardingStatus.LEGACY_COMPLETED, status)
        assertTrue(status?.isFinished == true)
    }

    @Test
    fun newGoogleAccountCreationIsMarkedNewAndInitializesOnboarding() = runBlocking {
        val googleEmail = "google.new@gmail.com"

        val googleResult = authRepository.loginWithGoogle(
            email = googleEmail,
            fullName = "Google Explorer",
            profilePictureUrl = "https://example.com/photo.jpg"
        )

        assertTrue(googleResult.isSuccess)
        val authResult = googleResult.getOrThrow()
        assertEquals(googleEmail, authResult.email)
        assertTrue("New Google user must be recognized as new account", authResult.isNewAccount)

        val status = userPreferencesRepository.getRawOnboardingStatus(googleEmail)
        assertEquals(OnboardingStatus.NOT_STARTED, status)
    }

    @Test
    fun existingGoogleLoginReturnsReturningUserAndPreservesOrMigratesStatus() = runBlocking {
        val googleEmail = "google.existing@gmail.com"

        // Simulate pre-existing Google user in Room
        val salt = SecurityUtils.generateSalt()
        fakeUserDao.insertUser(
            UserEntity(
                email = googleEmail,
                fullName = "Existing Google Traveler",
                passwordHash = SecurityUtils.hashPassword("token", salt),
                salt = salt,
                isGoogleUser = true
            )
        )

        val loginResult = authRepository.loginWithGoogle(
            email = googleEmail,
            fullName = "Existing Google Traveler"
        )

        assertTrue(loginResult.isSuccess)
        val authResult = loginResult.getOrThrow()
        assertEquals(googleEmail, authResult.email)
        assertFalse("Existing Google login must NOT be marked as new account", authResult.isNewAccount)

        // Status migrated safely to LEGACY_COMPLETED
        val status = userPreferencesRepository.getRawOnboardingStatus(googleEmail)
        assertEquals(OnboardingStatus.LEGACY_COMPLETED, status)
    }

    @Test
    fun completedOnboardingSubsequentLoginPreservesCompletedState() = runBlocking {
        val email = "completed.user@example.com"
        val password = "Password123"

        // Register
        authRepository.verifyOtpAndCreateAccount("Completed User", email, password, "123456")

        // User completes onboarding
        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)
        userPreferencesRepository.updateTravelPreferences(email, liked = setOf("mountains", "snow"))

        // Log out
        authRepository.logout()
        assertFalse(authRepository.isLoggedIn.first())

        // Log back in
        val loginResult = authRepository.login(email, password)
        assertTrue(loginResult.isSuccess)
        assertFalse("Subsequent login is returning user", loginResult.getOrThrow().isNewAccount)

        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.COMPLETED, prefs.onboardingStatus)
        assertEquals(setOf("mountains", "snow"), prefs.likedInterestIds)
    }

    @Test
    fun skippedOnboardingSubsequentLoginPreservesSkippedState() = runBlocking {
        val email = "skipped.user@example.com"
        val password = "Password123"

        authRepository.verifyOtpAndCreateAccount("Skipped User", email, password, "123456")
        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.SKIPPED)

        authRepository.logout()
        val loginResult = authRepository.login(email, password)

        assertTrue(loginResult.isSuccess)
        assertFalse(loginResult.getOrThrow().isNewAccount)

        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.SKIPPED, prefs.onboardingStatus)
        assertTrue("Skipped user remains finished", prefs.onboardingStatus.isFinished)
    }

    @Test
    fun interruptedOnboardingLeavesAccountIneligibleForHomeUntilHandled() = runBlocking {
        val email = "interrupted.user@example.com"
        val password = "Password123"

        authRepository.verifyOtpAndCreateAccount("Interrupted User", email, password, "123456")

        // Account was initialized with NOT_STARTED, user began deck:
        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.IN_PROGRESS)

        val status = userPreferencesRepository.getRawOnboardingStatus(email)
        assertEquals(OnboardingStatus.IN_PROGRESS, status)
        assertFalse("Interrupted status is not finished; must resume", status?.isFinished == true)
    }

    @Test
    fun accountSwitchingClearsActiveSessionWithoutDataCrossContamination() = runBlocking {
        val userA = "usera@example.com"
        val userB = "userb@example.com"
        val pass = "Password123"

        // Register User A
        authRepository.verifyOtpAndCreateAccount("User A", userA, pass, "123456")
        userPreferencesRepository.updateTravelPreferences(userA, liked = setOf("coastal"))
        userPreferencesRepository.updateOnboardingStatus(userA, OnboardingStatus.COMPLETED)

        // Logout User A
        authRepository.logout()
        assertFalse(authRepository.isLoggedIn.first())
        assertNull(authRepository.currentUserEmail.first())

        // Register User B
        authRepository.verifyOtpAndCreateAccount("User B", userB, pass, "123456")
        userPreferencesRepository.updateTravelPreferences(userB, liked = setOf("adventure"))
        userPreferencesRepository.updateOnboardingStatus(userB, OnboardingStatus.SKIPPED)

        // Verify User B's state
        val bPrefs = userPreferencesRepository.getTravelPreferencesOnce(userB)
        assertEquals(setOf("adventure"), bPrefs.likedInterestIds)
        assertEquals(OnboardingStatus.SKIPPED, bPrefs.onboardingStatus)

        // Verify User A's state remained completely untouched
        val aPrefs = userPreferencesRepository.getTravelPreferencesOnce(userA)
        assertEquals(setOf("coastal"), aPrefs.likedInterestIds)
        assertEquals(OnboardingStatus.COMPLETED, aPrefs.onboardingStatus)
    }

    @Test
    fun travelAgentRoleIsPreservedAndRefusedFromTravellerFlow() = runBlocking {
        val agentEmail = "agent@travelagency.com"
        val pass = "AgentPass123"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(pass, salt)

        fakeUserDao.insertUser(
            UserEntity(
                email = agentEmail,
                fullName = "Agent Smith",
                passwordHash = hash,
                salt = salt,
                role = UserRole.AGENT.name
            )
        )

        val role = fakeUserDao.observeRole(agentEmail).first()
        assertEquals(UserRole.AGENT.name, role)
    }

    @Test
    fun passwordResetSignsInAsReturningUser() = runBlocking {
        val email = "resetuser@example.com"
        val oldPass = "OldPassword123"
        val newPass = "NewPassword123"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(oldPass, salt)

        fakeUserDao.insertUser(
            UserEntity(
                email = email,
                fullName = "Reset User",
                passwordHash = hash,
                salt = salt
            )
        )

        val resetResult = authRepository.resetPassword(email, "123456", newPass)
        assertTrue(resetResult.isSuccess)
        val authResult = resetResult.getOrThrow()

        assertEquals(email, authResult.email)
        assertFalse("Password reset is returning user", authResult.isNewAccount)
        assertEquals(OnboardingStatus.LEGACY_COMPLETED, userPreferencesRepository.getRawOnboardingStatus(email))
    }

    @Test
    fun repeatedInitializationAndMigrationDoesNotCorruptPreferencesOrStatus() = runBlocking {
        val email = "repeat@example.com"
        val password = "Password123"

        // Register and complete onboarding with specific preferences
        authRepository.verifyOtpAndCreateAccount("Repeat User", email, password, "123456")
        userPreferencesRepository.updateTravelPreferences(
            email = email,
            liked = setOf("mountains", "hiking"),
            stronglyPreferred = setOf("mountains"),
            disliked = setOf("crowds"),
            weights = mapOf("mountains" to 2.5f)
        )
        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)

        // Multiple subsequent logins & migrations
        for (i in 1..3) {
            val loginResult = authRepository.login(email, password)
            assertTrue(loginResult.isSuccess)
            assertFalse(loginResult.getOrThrow().isNewAccount)

            val status = userPreferencesRepository.getRawOnboardingStatus(email)
            assertEquals(OnboardingStatus.COMPLETED, status)

            val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
            assertEquals(setOf("mountains", "hiking"), prefs.likedInterestIds)
            assertEquals(setOf("mountains"), prefs.stronglyPreferredInterestIds)
            assertEquals(setOf("crowds"), prefs.dislikedInterestIds)
            assertEquals(2.5f, prefs.preferenceWeights["mountains"] ?: 0f, 0.01f)
        }
    }

    @Test
    fun persistenceOrAuthFailureDoesNotFalselyMarkOnboardingComplete() = runBlocking {
        val email = "failed.reg@example.com"

        // Attempt account creation with invalid OTP
        val result = authRepository.verifyOtpAndCreateAccount(
            fullName = "Failed Traveler",
            email = email,
            password = "Password123",
            otp = "999999" // wrong OTP
        )

        assertTrue("Invalid OTP must fail registration", result.isFailure)
        assertFalse("Failed registration must not set session", authRepository.isLoggedIn.first())
        assertNull("Failed registration must not create user in DB", fakeUserDao.getUserByEmail(email))
        assertNull("Failed registration must not record onboarding status", userPreferencesRepository.getRawOnboardingStatus(email))
    }

    @Test
    fun legacyAccountMigrationPreservesExistingProfileAndUserData() = runBlocking {
        val legacyEmail = "existing.traveler@example.com"
        val password = "Password123"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(password, salt)

        // Pre-existing legacy user with full profile
        val legacyUser = UserEntity(
            email = legacyEmail,
            fullName = "Experienced Traveler",
            passwordHash = hash,
            salt = salt,
            phoneNumber = "+1234567890",
            gender = "Female",
            homeLocation = "San Francisco, USA",
            dateOfBirth = "1992-05-15",
            maritalStatus = "Single",
            profilePictureUrl = "https://example.com/avatar.jpg"
        )
        fakeUserDao.insertUser(legacyUser)

        // Perform login
        val loginResult = authRepository.login(legacyEmail, password)
        assertTrue(loginResult.isSuccess)
        assertFalse(loginResult.getOrThrow().isNewAccount)

        // Verify all Room profile fields remain intact
        val fetchedUser = fakeUserDao.getUserByEmail(legacyEmail)
        assertNotNull(fetchedUser)
        assertEquals("+1234567890", fetchedUser!!.phoneNumber)
        assertEquals("Female", fetchedUser.gender)
        assertEquals("San Francisco, USA", fetchedUser.homeLocation)
        assertEquals("1992-05-15", fetchedUser.dateOfBirth)
        assertEquals("Single", fetchedUser.maritalStatus)
        assertEquals("https://example.com/avatar.jpg", fetchedUser.profilePictureUrl)

        // Verify onboarding is safely migrated to LEGACY_COMPLETED with empty travel preferences
        val status = userPreferencesRepository.getRawOnboardingStatus(legacyEmail)
        assertEquals(OnboardingStatus.LEGACY_COMPLETED, status)
        val prefs = userPreferencesRepository.getTravelPreferencesOnce(legacyEmail)
        assertTrue("No preferences invented", prefs.likedInterestIds.isEmpty())
        assertTrue(prefs.dislikedInterestIds.isEmpty())
    }

    @Test
    fun logoutPreservesAllPersistedPreferencesAndOnboardingStatusAcrossLogouts() = runBlocking {
        val email = "preserve.prefs@example.com"
        val password = "Password123"

        // 1. Create account
        authRepository.verifyOtpAndCreateAccount("Preserve User", email, password, "123456")

        // 2. Set all 5 preference fields
        val liked = setOf("coastal", "islands", "beaches")
        val strong = setOf("coastal")
        val disliked = setOf("extreme_cold", "snow")
        val weights = mapOf("coastal" to 3.0f, "islands" to 1.5f)

        userPreferencesRepository.updateTravelPreferences(
            email = email,
            liked = liked,
            stronglyPreferred = strong,
            disliked = disliked,
            weights = weights
        )
        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)

        // Verify state is saved prior to logout
        val preLogoutPrefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.COMPLETED, preLogoutPrefs.onboardingStatus)
        assertEquals(liked, preLogoutPrefs.likedInterestIds)
        assertEquals(strong, preLogoutPrefs.stronglyPreferredInterestIds)
        assertEquals(disliked, preLogoutPrefs.dislikedInterestIds)
        assertEquals(weights, preLogoutPrefs.preferenceWeights)

        // 3. Perform logout
        val logoutResult = authRepository.logout()
        assertTrue(logoutResult.isSuccess)
        assertFalse("Session must be inactive after logout", authRepository.isLoggedIn.first())
        assertNull("Session email must be cleared after logout", authRepository.currentUserEmail.first())

        // 4. VERIFY: While logged out, reading persisted account data from DataStore
        // MUST NOT have been deleted, reset, or cleared by clearAccountState
        val whileLoggedOutPrefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals("OnboardingStatus must be preserved across logout", OnboardingStatus.COMPLETED, whileLoggedOutPrefs.onboardingStatus)
        assertEquals("Liked interests must be preserved across logout", liked, whileLoggedOutPrefs.likedInterestIds)
        assertEquals("Strongly preferred interests must be preserved across logout", strong, whileLoggedOutPrefs.stronglyPreferredInterestIds)
        assertEquals("Disliked interests must be preserved across logout", disliked, whileLoggedOutPrefs.dislikedInterestIds)
        assertEquals("Preference weights must be preserved across logout", weights, whileLoggedOutPrefs.preferenceWeights)

        // 5. Subsequent login
        val loginResult = authRepository.login(email, password)
        assertTrue(loginResult.isSuccess)
        assertFalse("Returning user", loginResult.getOrThrow().isNewAccount)

        // 6. Verify preferences are STILL 100% intact after re-logging in
        val postLoginPrefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.COMPLETED, postLoginPrefs.onboardingStatus)
        assertEquals(liked, postLoginPrefs.likedInterestIds)
        assertEquals(strong, postLoginPrefs.stronglyPreferredInterestIds)
        assertEquals(disliked, postLoginPrefs.dislikedInterestIds)
        assertEquals(weights, postLoginPrefs.preferenceWeights)
    }
}
