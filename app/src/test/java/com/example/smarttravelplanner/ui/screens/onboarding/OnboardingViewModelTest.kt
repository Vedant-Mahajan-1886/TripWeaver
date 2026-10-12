package com.example.smarttravelplanner.ui.screens.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.entity.UserEntity
import com.example.smarttravelplanner.data.remote.service.EmailVerificationService
import com.example.smarttravelplanner.data.repository.AuthRepositoryImpl
import com.example.smarttravelplanner.data.repository.UserPreferencesRepositoryImpl
import com.example.smarttravelplanner.domain.model.CardAction
import com.example.smarttravelplanner.domain.model.DefaultPreferenceDeck
import com.example.smarttravelplanner.domain.model.OnboardingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OnboardingViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var userPreferencesRepository: UserPreferencesRepositoryImpl
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var viewModel: OnboardingViewModel

    private class FakeEmailVerificationService : EmailVerificationService {
        override suspend fun sendVerificationOtp(email: String): Result<String> = Result.success("123456")
        override fun verifyOtp(email: String, enteredCode: String): Result<Boolean> = Result.success(true)
        override suspend fun sendPasswordResetOtp(email: String): Result<String> = Result.success("123456")
        override fun clearSession(email: String) {}
    }

    private class FakeUserDao : UserDao {
        val users = mutableMapOf<String, UserEntity>()
        override suspend fun getUserByEmail(email: String): UserEntity? = users[email]
        override suspend fun getUserById(userId: Long): UserEntity? = null
        override suspend fun emailExists(email: String): Int = if (users.containsKey(email)) 1 else 0
        override suspend fun insertUser(user: UserEntity): Long {
            users[user.email] = user
            return 1L
        }
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun updatePassword(email: String, passwordHash: String, salt: String) {}
        override suspend fun updateProfileDetails(
            email: String, fullName: String, phoneNumber: String,
            profilePictureUrl: String?, gender: String, homeLocation: String,
            dateOfBirth: String, maritalStatus: String
        ) {}
        override suspend fun updateProfilePicture(email: String, profilePictureUrl: String?) {}
        override fun observeRole(email: String): Flow<String?> = flowOf(null)
        override suspend fun updateRole(email: String, role: String) {}
        override suspend fun deleteUser(userId: Long) {}
    }

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { tempFolder.newFile("test_onboarding_prefs.preferences_pb") }
        )
        userPreferencesRepository = UserPreferencesRepositoryImpl(dataStore)
        authRepository = AuthRepositoryImpl(
            userDao = FakeUserDao(),
            emailVerificationService = FakeEmailVerificationService(),
            userPreferencesRepository = userPreferencesRepository,
            dataStore = dataStore
        )
        viewModel = OnboardingViewModel(userPreferencesRepository, authRepository).apply {
            scopeOverride = scope
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun likeActionSavesInterestWithPlusOneWeightAndAdvances() = runBlocking {
        val email = "traveler.like@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        val firstCard = DefaultPreferenceDeck.cards[0]
        val job = viewModel.onCardAction(CardAction.LIKE)
        job?.join()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertTrue(state.likedInterestIds.contains(firstCard.id))
        assertFalse(state.stronglyPreferredInterestIds.contains(firstCard.id))
        assertFalse(state.dislikedInterestIds.contains(firstCard.id))
        assertEquals(1.0f, state.preferenceWeights[firstCard.id] ?: 0f, 0.01f)

        // Verify repository persistence
        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(setOf(firstCard.id), prefs.likedInterestIds)
        assertEquals(1.0f, prefs.preferenceWeights[firstCard.id] ?: 0f, 0.01f)
        assertEquals(OnboardingStatus.IN_PROGRESS, prefs.onboardingStatus)
    }

    @Test
    fun dislikeActionSavesInterestWithMinusOneWeightAndAdvances() = runBlocking {
        val email = "traveler.dislike@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        val firstCard = DefaultPreferenceDeck.cards[0]
        val job = viewModel.onCardAction(CardAction.DISLIKE)
        job?.join()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertFalse(state.likedInterestIds.contains(firstCard.id))
        assertTrue(state.dislikedInterestIds.contains(firstCard.id))
        assertEquals(-1.0f, state.preferenceWeights[firstCard.id] ?: 0f, 0.01f)

        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(setOf(firstCard.id), prefs.dislikedInterestIds)
        assertEquals(-1.0f, prefs.preferenceWeights[firstCard.id] ?: 0f, 0.01f)
    }

    @Test
    fun strongPreferenceActionSavesWithPlusTwoWeightInLikedAndStrong() = runBlocking {
        val email = "traveler.super@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        val firstCard = DefaultPreferenceDeck.cards[0]
        val job = viewModel.onCardAction(CardAction.STRONG_PREFERENCE)
        job?.join()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertTrue(state.likedInterestIds.contains(firstCard.id))
        assertTrue(state.stronglyPreferredInterestIds.contains(firstCard.id))
        assertFalse(state.dislikedInterestIds.contains(firstCard.id))
        assertEquals(2.0f, state.preferenceWeights[firstCard.id] ?: 0f, 0.01f)

        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertTrue(prefs.likedInterestIds.contains(firstCard.id))
        assertTrue(prefs.stronglyPreferredInterestIds.contains(firstCard.id))
        assertEquals(2.0f, prefs.preferenceWeights[firstCard.id] ?: 0f, 0.01f)
    }

    @Test
    fun duplicateSubmissionsAreIgnoredWhileActionIsProcessing() = runBlocking {
        val email = "traveler.dedup@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        // First action starts processing
        val job1 = viewModel.onCardAction(CardAction.LIKE)
        // Immediate second action while action is in progress
        val job2 = viewModel.onCardAction(CardAction.DISLIKE)

        assertNull("Duplicate invocation while isActionInProgress must return null and be ignored", job2)
        job1?.join()

        val state = viewModel.uiState.value
        assertEquals("Index should only advance by 1 despite rapid duplicate trigger", 1, state.currentIndex)
        assertTrue("Only the first gesture action should be recorded", state.likedInterestIds.contains(DefaultPreferenceDeck.cards[0].id))
        assertFalse(state.dislikedInterestIds.contains(DefaultPreferenceDeck.cards[0].id))
    }

    @Test
    fun undoRollsBackCardIndexAndRevertsPreferences() = runBlocking {
        val email = "traveler.undo@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        val card0 = DefaultPreferenceDeck.cards[0]
        val card1 = DefaultPreferenceDeck.cards[1]

        viewModel.onCardAction(CardAction.LIKE)?.join()
        viewModel.onCardAction(CardAction.STRONG_PREFERENCE)?.join()

        assertEquals(2, viewModel.uiState.value.currentIndex)
        assertTrue(viewModel.uiState.value.canUndo)

        // Perform Undo
        viewModel.undoLastAction()?.join()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertTrue(state.likedInterestIds.contains(card0.id))
        assertFalse("Undone card should be removed from liked", state.likedInterestIds.contains(card1.id))
        assertFalse("Undone card should be removed from strong", state.stronglyPreferredInterestIds.contains(card1.id))
        assertNull(state.preferenceWeights[card1.id])

        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(setOf(card0.id), prefs.likedInterestIds)
        assertNull(prefs.preferenceWeights[card1.id])
    }

    @Test
    fun skipOnboardingMarksStatusSkippedAndPreservesPreviousDecisions() = runBlocking {
        val email = "traveler.skip@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        val card0 = DefaultPreferenceDeck.cards[0]
        viewModel.onCardAction(CardAction.LIKE)?.join()

        viewModel.skipOnboarding()?.join()

        assertTrue(viewModel.uiState.value.isSkipped)

        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.SKIPPED, prefs.onboardingStatus)
        assertEquals(setOf(card0.id), prefs.likedInterestIds)
        assertTrue("Unanswered cards must NOT be marked as dislikes", prefs.dislikedInterestIds.isEmpty())
    }

    @Test
    fun completingFinalCardMarksOnboardingCompleted() = runBlocking {
        val email = "traveler.complete@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        // Rate all 12 cards
        for (i in 0 until DefaultPreferenceDeck.cards.size) {
            viewModel.onCardAction(if (i % 2 == 0) CardAction.LIKE else CardAction.DISLIKE)?.join()
        }

        assertTrue(viewModel.uiState.value.isCompleted)
        assertTrue(viewModel.uiState.value.isFinishedDeck)

        val prefs = userPreferencesRepository.getTravelPreferencesOnce(email)
        assertEquals(OnboardingStatus.COMPLETED, prefs.onboardingStatus)
        assertEquals(DefaultPreferenceDeck.cards.size, prefs.preferenceWeights.size)
    }

    @Test
    fun interruptedOnboardingResumesAtFirstUnansweredCard() = runBlocking {
        val email = "traveler.resume@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        // Simulate previous decisions for first 3 cards
        val card0 = DefaultPreferenceDeck.cards[0].id
        val card1 = DefaultPreferenceDeck.cards[1].id
        val card2 = DefaultPreferenceDeck.cards[2].id

        userPreferencesRepository.updateTravelPreferences(
            email = email,
            liked = setOf(card0, card1),
            disliked = setOf(card2),
            weights = mapOf(card0 to 1f, card1 to 2f, card2 to -1f)
        )
        userPreferencesRepository.updateOnboardingStatus(email, OnboardingStatus.IN_PROGRESS)

        // Fresh load into ViewModel
        val freshViewModel = OnboardingViewModel(userPreferencesRepository, authRepository).apply {
            scopeOverride = scope
        }
        freshViewModel.loadPreferencesForAccount(email).join()

        val state = freshViewModel.uiState.value
        assertEquals("Must resume at index 3 (first undecided card)", 3, state.currentIndex)
        assertEquals(setOf(card0, card1), state.likedInterestIds)
        assertEquals(setOf(card2), state.dislikedInterestIds)
    }

    @Test
    fun accountIsolationProtectsPreferencesAcrossUsers() = runBlocking {
        val userA = "usera.iso@example.com"
        val userB = "userb.iso@example.com"

        userPreferencesRepository.initializeNewAccountOnboarding(userA)
        userPreferencesRepository.initializeNewAccountOnboarding(userB)

        // Rate card for User A
        viewModel.loadPreferencesForAccount(userA).join()
        viewModel.onCardAction(CardAction.LIKE)?.join()

        // Switch and rate card for User B
        viewModel.loadPreferencesForAccount(userB).join()
        viewModel.onCardAction(CardAction.DISLIKE)?.join()

        val aPrefs = userPreferencesRepository.getTravelPreferencesOnce(userA)
        val bPrefs = userPreferencesRepository.getTravelPreferencesOnce(userB)

        val cardId = DefaultPreferenceDeck.cards[0].id
        assertTrue(aPrefs.likedInterestIds.contains(cardId))
        assertFalse(aPrefs.dislikedInterestIds.contains(cardId))
        assertEquals(1.0f, aPrefs.preferenceWeights[cardId] ?: 0f, 0.01f)

        assertFalse(bPrefs.likedInterestIds.contains(cardId))
        assertTrue(bPrefs.dislikedInterestIds.contains(cardId))
        assertEquals(-1.0f, bPrefs.preferenceWeights[cardId] ?: 0f, 0.01f)
    }

    @Test
    fun deckContainsExactlyTwelveCardsAcrossThreeSpecifiedCategories() {
        val deck = DefaultPreferenceDeck.cards
        assertEquals("Deck must have exactly 12 cards", 12, deck.size)

        val travelStyle = deck.filter { it.category == "Travel Style" }
        assertEquals(4, travelStyle.size)
        assertEquals(listOf("adventure", "relaxation", "nature_wildlife", "culture_heritage"), travelStyle.map { it.id })

        val interests = deck.filter { it.category == "Interests & Experiences" }
        assertEquals(4, interests.size)
        assertEquals(listOf("food_cafes", "history_architecture", "shopping_city_life", "photography_scenic"), interests.map { it.id })

        val waysToExplore = deck.filter { it.category == "Ways to Explore" }
        assertEquals(4, waysToExplore.size)
        assertEquals(listOf("road_trips", "beaches_islands", "mountains_trekking", "local_offbeat"), waysToExplore.map { it.id })

        deck.forEach { card ->
            assertTrue(card.id.isNotBlank())
            assertTrue(card.title.isNotBlank())
            assertTrue(card.description.isNotBlank())
            assertTrue(card.imageRes > 0)
        }
    }

    @Test
    fun persistenceFailureDoesNotAdvanceOrFalselyComplete() = runBlocking {
        val failingRepo = object : com.example.smarttravelplanner.data.repository.UserPreferencesRepository by userPreferencesRepository {
            override suspend fun updateTravelPreferences(
                email: String,
                liked: Set<String>,
                stronglyPreferred: Set<String>,
                disliked: Set<String>,
                weights: Map<String, Float>
            ) {
                throw java.io.IOException("Storage write failed")
            }
        }

        val failingVm = OnboardingViewModel(failingRepo, authRepository).apply {
            scopeOverride = scope
        }

        val email = "fail@example.com"
        failingVm.loadPreferencesForAccount(email).join()

        val job = failingVm.onCardAction(CardAction.LIKE)
        job?.join()

        val state = failingVm.uiState.value
        assertEquals("Card index must NOT advance on failure", 0, state.currentIndex)
        assertFalse("Must not mark completed on failure", state.isCompleted)
        assertFalse("Action lock must be released so user can retry", state.isActionInProgress)
        assertEquals("Storage write failed", state.errorMessage)
    }

    @Test
    fun superLikeAffectsOnlySelectedInterestAndSubsequentCardDoesNotInheritState() = runBlocking {
        val email = "superlike.isolation@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        val card0 = DefaultPreferenceDeck.cards[0]
        val card1 = DefaultPreferenceDeck.cards[1]

        // 1. Super Like the first card
        val job = viewModel.onCardAction(CardAction.STRONG_PREFERENCE)
        job?.join()

        val state = viewModel.uiState.value

        // Card 0 must have +2.0 weight, and be present in liked and stronglyPreferred
        assertTrue(state.likedInterestIds.contains(card0.id))
        assertTrue(state.stronglyPreferredInterestIds.contains(card0.id))
        assertFalse(state.dislikedInterestIds.contains(card0.id))
        assertEquals(2.0f, state.preferenceWeights[card0.id] ?: 0f, 0.01f)

        // Card 1 must NOT inherit any preference state
        assertEquals("Must advance to next card", 1, state.currentIndex)
        assertEquals(card1.id, state.currentCard?.id)
        assertFalse("Subsequent card must NOT be liked", state.likedInterestIds.contains(card1.id))
        assertFalse("Subsequent card must NOT be strongly preferred", state.stronglyPreferredInterestIds.contains(card1.id))
        assertFalse("Subsequent card must NOT be disliked", state.dislikedInterestIds.contains(card1.id))
        assertNull("Subsequent card must have no weight assigned", state.preferenceWeights[card1.id])

        // Verify per-card derived states for Card 1
        val isCard1SuperLiked = card1.id in state.stronglyPreferredInterestIds
        val isCard1Liked = card1.id in state.likedInterestIds && !isCard1SuperLiked
        val isCard1Disliked = card1.id in state.dislikedInterestIds
        assertFalse(isCard1SuperLiked)
        assertFalse(isCard1Liked)
        assertFalse(isCard1Disliked)
    }

    @Test
    fun normalLikeOrDislikeCorrectlyReplacesPreviousDecisionForThatInterest() = runBlocking {
        val email = "replace.decision@example.com"
        userPreferencesRepository.initializeNewAccountOnboarding(email)
        viewModel.loadPreferencesForAccount(email).join()

        val card0 = DefaultPreferenceDeck.cards[0]

        // Initially rate card 0 with LIKE (+1.0)
        viewModel.onCardAction(CardAction.LIKE)?.join()
        assertTrue(viewModel.uiState.value.likedInterestIds.contains(card0.id))
        assertEquals(1.0f, viewModel.uiState.value.preferenceWeights[card0.id] ?: 0f, 0.01f)

        // Undo back to card 0
        viewModel.undoLastAction()?.join()
        assertEquals(0, viewModel.uiState.value.currentIndex)

        // Replace with DISLIKE (-1.0)
        viewModel.onCardAction(CardAction.DISLIKE)?.join()
        val stateAfterDislike = viewModel.uiState.value
        assertFalse("Card must no longer be liked", stateAfterDislike.likedInterestIds.contains(card0.id))
        assertFalse("Card must not be strongly preferred", stateAfterDislike.stronglyPreferredInterestIds.contains(card0.id))
        assertTrue("Card must be in disliked", stateAfterDislike.dislikedInterestIds.contains(card0.id))
        assertEquals(-1.0f, stateAfterDislike.preferenceWeights[card0.id] ?: 0f, 0.01f)

        // Undo back to card 0 and replace with STRONG_PREFERENCE (+2.0)
        viewModel.undoLastAction()?.join()
        viewModel.onCardAction(CardAction.STRONG_PREFERENCE)?.join()
        val stateAfterSuper = viewModel.uiState.value
        assertTrue("Card must be in liked", stateAfterSuper.likedInterestIds.contains(card0.id))
        assertTrue("Card must be in strongly preferred", stateAfterSuper.stronglyPreferredInterestIds.contains(card0.id))
        assertFalse("Card must NOT be in disliked", stateAfterSuper.dislikedInterestIds.contains(card0.id))
        assertEquals(2.0f, stateAfterSuper.preferenceWeights[card0.id] ?: 0f, 0.01f)
    }

    @Test
    fun allTwelveCardIdsAreUniqueWithVerifiedMatchingDrawables() {
        val deck = DefaultPreferenceDeck.cards
        assertEquals(12, deck.size)

        // Verify all 12 IDs are distinct
        val distinctIds = deck.map { it.id }.toSet()
        assertEquals("All 12 card IDs must be unique", 12, distinctIds.size)

        // Verify all titles are unique
        val distinctTitles = deck.map { it.title }.toSet()
        assertEquals("All 12 card titles must be unique", 12, distinctTitles.size)

        // Verify exact verified image mappings
        val expectedMappings = mapOf(
            "adventure" to com.example.smarttravelplanner.R.drawable.pref_adventure,
            "relaxation" to com.example.smarttravelplanner.R.drawable.pref_relaxation,
            "nature_wildlife" to com.example.smarttravelplanner.R.drawable.pref_nature_wildlife,
            "culture_heritage" to com.example.smarttravelplanner.R.drawable.dest_jaipur,
            "food_cafes" to com.example.smarttravelplanner.R.drawable.pref_food_cafes,
            "history_architecture" to com.example.smarttravelplanner.R.drawable.dest_agra,
            "shopping_city_life" to com.example.smarttravelplanner.R.drawable.pref_city_shopping,
            "photography_scenic" to com.example.smarttravelplanner.R.drawable.dest_kashmir,
            "road_trips" to com.example.smarttravelplanner.R.drawable.pref_road_trips,
            "beaches_islands" to com.example.smarttravelplanner.R.drawable.pref_beaches_islands,
            "mountains_trekking" to com.example.smarttravelplanner.R.drawable.pref_mountains_trekking,
            "local_offbeat" to com.example.smarttravelplanner.R.drawable.pref_local_offbeat
        )

        deck.forEach { card ->
            val expectedRes = expectedMappings[card.id]
            assertNotNull("Card ${card.id} must be in the expected mappings", expectedRes)
            assertEquals("Card ${card.id} must have the verified drawable", expectedRes, card.imageRes)
            assertTrue("Card description must be informative", card.description.length > 20)
        }
    }
}
