package com.example.smarttravelplanner.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import com.example.smarttravelplanner.domain.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserTravelPreferencesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var repository: UserPreferencesRepositoryImpl
    private val dataStoreFile by lazy { tempFolder.newFile("test_user_preferences.preferences_pb") }

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { dataStoreFile }
        )
        repository = UserPreferencesRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun rawOnboardingStatusIsNullForUninitializedAccount() = runBlocking {
        val status = repository.getRawOnboardingStatus("brand_new@example.com")
        assertNull("Uninitialized account should have null raw onboarding status", status)
    }

    @Test
    fun initializeNewAccountSetsNotStartedAndEmptyPreferences() = runBlocking {
        val email = "new.traveler@example.com"
        repository.initializeNewAccountOnboarding(email)

        val rawStatus = repository.getRawOnboardingStatus(email)
        assertEquals(OnboardingStatus.NOT_STARTED, rawStatus)

        val prefs = repository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.NOT_STARTED, prefs.onboardingStatus)
        assertTrue(prefs.likedInterestIds.isEmpty())
        assertTrue(prefs.stronglyPreferredInterestIds.isEmpty())
        assertTrue(prefs.dislikedInterestIds.isEmpty())
        assertTrue(prefs.preferenceWeights.isEmpty())
        assertFalse(prefs.onboardingStatus.isFinished)
    }

    @Test
    fun legacyAccountMigrationSetsLegacyCompletedWithoutInventingPreferences() = runBlocking {
        val legacyEmail = "legacy.traveler@example.com"

        // Prior to migration: status is null
        assertNull(repository.getRawOnboardingStatus(legacyEmail))

        // Migrate
        val resolved = repository.migrateLegacyAccountIfNeeded(legacyEmail)
        assertEquals(OnboardingStatus.LEGACY_COMPLETED, resolved)

        val prefs = repository.getTravelPreferencesOnce(legacyEmail)
        assertEquals(OnboardingStatus.LEGACY_COMPLETED, prefs.onboardingStatus)
        assertTrue("No preferences should be invented for legacy users", prefs.likedInterestIds.isEmpty())
        assertTrue(prefs.stronglyPreferredInterestIds.isEmpty())
        assertTrue(prefs.dislikedInterestIds.isEmpty())
        assertTrue("Legacy completed is considered finished", prefs.onboardingStatus.isFinished)
    }

    @Test
    fun migrationIsIdempotentAndDoesNotOverwriteExistingStates() = runBlocking {
        val email = "active.traveler@example.com"

        // Account is IN_PROGRESS
        repository.initializeNewAccountOnboarding(email)
        repository.updateOnboardingStatus(email, OnboardingStatus.IN_PROGRESS)

        // Running legacy migration must NOT overwrite IN_PROGRESS
        val migrated = repository.migrateLegacyAccountIfNeeded(email)
        assertEquals(OnboardingStatus.IN_PROGRESS, migrated)
        assertEquals(OnboardingStatus.IN_PROGRESS, repository.getRawOnboardingStatus(email))

        // Account is COMPLETED
        repository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)
        val migratedCompleted = repository.migrateLegacyAccountIfNeeded(email)
        assertEquals(OnboardingStatus.COMPLETED, migratedCompleted)
        assertEquals(OnboardingStatus.COMPLETED, repository.getRawOnboardingStatus(email))

        // Account is SKIPPED
        repository.updateOnboardingStatus(email, OnboardingStatus.SKIPPED)
        val migratedSkipped = repository.migrateLegacyAccountIfNeeded(email)
        assertEquals(OnboardingStatus.SKIPPED, migratedSkipped)
        assertEquals(OnboardingStatus.SKIPPED, repository.getRawOnboardingStatus(email))
    }

    @Test
    fun completedOnboardingPersistsSelectionsAcrossReads() = runBlocking {
        val email = "foodie@example.com"
        repository.initializeNewAccountOnboarding(email)

        repository.updateTravelPreferences(
            email = email,
            liked = setOf("food", "culture", "heritage"),
            stronglyPreferred = setOf("food"),
            disliked = setOf("extreme_adventure"),
            weights = mapOf("food" to 1.5f, "culture" to 1.0f)
        )
        repository.updateOnboardingStatus(email, OnboardingStatus.COMPLETED)

        val prefs = repository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.COMPLETED, prefs.onboardingStatus)
        assertTrue(prefs.onboardingStatus.isFinished)
        assertEquals(setOf("food", "culture", "heritage"), prefs.likedInterestIds)
        assertEquals(setOf("food"), prefs.stronglyPreferredInterestIds)
        assertEquals(setOf("extreme_adventure"), prefs.dislikedInterestIds)
        assertEquals(1.5f, prefs.preferenceWeights["food"] ?: 0f, 0.01f)
        assertEquals(1.0f, prefs.preferenceWeights["culture"] ?: 0f, 0.01f)
    }

    @Test
    fun skippedOnboardingPersistsAndIsFinished() = runBlocking {
        val email = "skipper@example.com"
        repository.initializeNewAccountOnboarding(email)
        repository.updateOnboardingStatus(email, OnboardingStatus.SKIPPED)

        val prefs = repository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.SKIPPED, prefs.onboardingStatus)
        assertTrue("Skipped user must be considered finished so they land on Home", prefs.onboardingStatus.isFinished)
    }

    @Test
    fun twoAccountsOnSameDeviceDoNotLeakPreferences() = runBlocking {
        val user1 = "alice@example.com"
        val user2 = "bob@example.com"

        // User 1 setup
        repository.initializeNewAccountOnboarding(user1)
        repository.updateTravelPreferences(
            email = user1,
            liked = setOf("heritage", "palaces"),
            stronglyPreferred = setOf("heritage"),
            disliked = setOf("nightlife"),
            weights = mapOf("heritage" to 2.0f)
        )
        repository.updateOnboardingStatus(user1, OnboardingStatus.COMPLETED)

        // User 2 setup
        repository.initializeNewAccountOnboarding(user2)
        repository.updateTravelPreferences(
            email = user2,
            liked = setOf("trekking", "camping"),
            stronglyPreferred = setOf("trekking"),
            disliked = setOf("luxury"),
            weights = mapOf("trekking" to 1.8f)
        )
        repository.updateOnboardingStatus(user2, OnboardingStatus.COMPLETED)

        // Read Alice
        val alicePrefs = repository.getTravelPreferencesOnce(user1)
        assertEquals(setOf("heritage", "palaces"), alicePrefs.likedInterestIds)
        assertEquals(setOf("heritage"), alicePrefs.stronglyPreferredInterestIds)
        assertEquals(setOf("nightlife"), alicePrefs.dislikedInterestIds)
        assertEquals(2.0f, alicePrefs.preferenceWeights["heritage"] ?: 0f, 0.01f)

        // Read Bob
        val bobPrefs = repository.getTravelPreferencesOnce(user2)
        assertEquals(setOf("trekking", "camping"), bobPrefs.likedInterestIds)
        assertEquals(setOf("trekking"), bobPrefs.stronglyPreferredInterestIds)
        assertEquals(setOf("luxury"), bobPrefs.dislikedInterestIds)
        assertEquals(1.8f, bobPrefs.preferenceWeights["trekking"] ?: 0f, 0.01f)

        // Switch back and forth
        repository.clearAccountState(user1)
        repository.clearAccountState(user2)

        val alicePrefsAfterSwitch = repository.getTravelPreferencesOnce(user1)
        assertEquals(setOf("heritage", "palaces"), alicePrefsAfterSwitch.likedInterestIds)
    }

    @Test
    fun emailNormalizationIsConsistentAcrossCasingAndWhitespace() = runBlocking {
        val rawEmail = "  Alice.Traveler@GMAIL.com  "
        val normalized = "alice.traveler@gmail.com"

        repository.initializeNewAccountOnboarding(rawEmail)
        repository.updateOnboardingStatus(normalized, OnboardingStatus.COMPLETED)
        repository.updateTravelPreferences(rawEmail, liked = setOf("coastal"))

        val prefsFromRaw = repository.getTravelPreferencesOnce(rawEmail)
        val prefsFromNorm = repository.getTravelPreferencesOnce(normalized)

        assertEquals(prefsFromNorm, prefsFromRaw)
        assertEquals(OnboardingStatus.COMPLETED, prefsFromRaw.onboardingStatus)
        assertEquals(setOf("coastal"), prefsFromRaw.likedInterestIds)
    }

    @Test
    fun blankEmailThrowsIllegalArgumentException() = runBlocking {
        try {
            repository.getTravelPreferencesOnce("   ")
            fail("Should throw IllegalArgumentException on blank email")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("blank", ignoreCase = true) == true)
        }
    }

    @Test
    fun accountScopedPreferencesPreserveGlobalSettingsAndProfile() = runBlocking {
        // Set global preferences
        repository.update(
            UserPreferences(
                firstName = "Shaurya",
                themeMode = "Dark Theme",
                defaultBudget = 50000.0
            )
        )

        // Update account-scoped travel preferences
        val email = "shaurya@example.com"
        repository.initializeNewAccountOnboarding(email)
        repository.updateTravelPreferences(email, liked = setOf("hills"))

        // Global preferences must remain intact
        val global = repository.userPreferences.first()
        assertEquals("Shaurya", global.firstName)
        assertEquals("Dark Theme", global.themeMode)
        assertEquals(50000.0, global.defaultBudget, 0.01)

        // Account preferences must also be intact
        val travel = repository.getTravelPreferencesOnce(email)
        assertEquals(setOf("hills"), travel.likedInterestIds)
    }
}
