package com.example.smarttravelplanner.ui.screens.agent

import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.model.agent.TripShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewDraftTest {

    private val days = listOf(
        Itinerary(
            id = 1, tripId = 9, dayNumber = 1, date = "2026-10-12",
            activities = listOf(
                Activity(itineraryId = 1, title = "Amber Fort"),
                Activity(itineraryId = 1, title = "Hawa Mahal")
            )
        ),
        Itinerary(
            id = 2, tripId = 9, dayNumber = 2, date = "2026-10-13",
            activities = listOf(Activity(itineraryId = 2, title = "City Palace"))
        )
    )

    private fun share(comment: String = "", price: Double? = null) = TripShare(
        id = 5, tripId = 9, travellerEmail = "t@x.com", agentEmail = "a@x.com",
        message = "", status = ShareStatus.VIEWED, decision = null,
        overallComment = comment, priceEstimate = price, sentAt = 0, updatedAt = 0
    )

    @Test
    fun untouchedActivitiesAreSubmittedAsAvailable() {
        val submission = buildSubmission(days, ReviewDraft(), ReviewDecision.APPROVED)

        assertEquals(3, submission.activities.size)
        assertEquals(listOf("Amber Fort", "Hawa Mahal", "City Palace"), submission.activities.map { it.activityTitle })
        assertEquals(setOf(ActivityVerdict.AVAILABLE), submission.activities.map { it.verdict }.toSet())
    }

    @Test
    fun choicesAreMappedToTheRightActivity() {
        val draft = ReviewDraft(
            activities = mapOf(
                ActivityKey(1, 1) to ActivityDraft(ActivityVerdict.SUGGEST_CHANGE, suggestion = "Jal Mahal"),
                ActivityKey(2, 0) to ActivityDraft(ActivityVerdict.NOT_AVAILABLE, comment = "Closed")
            )
        )
        val submission = buildSubmission(days, draft, ReviewDecision.CHANGES_REQUESTED)

        val hawa = submission.activities.first { it.dayNumber == 1 && it.activityIndex == 1 }
        assertEquals(ActivityVerdict.SUGGEST_CHANGE, hawa.verdict)
        assertEquals("Jal Mahal", hawa.suggestion)

        val palace = submission.activities.first { it.dayNumber == 2 }
        assertEquals(ActivityVerdict.NOT_AVAILABLE, palace.verdict)
        assertEquals("Closed", palace.comment)
    }

    @Test
    fun suggestionIsDroppedIfVerdictIsNotSuggestChange() {
        val draft = ReviewDraft(
            activities = mapOf(ActivityKey(1, 0) to ActivityDraft(ActivityVerdict.AVAILABLE, suggestion = "left over"))
        )
        val submission = buildSubmission(days, draft, ReviewDecision.APPROVED)
        assertEquals("", submission.activities.first().suggestion)
    }

    @Test
    fun priceTextBecomesEstimate() {
        assertEquals(45000.0, buildSubmission(days, ReviewDraft(priceText = "45000"), ReviewDecision.APPROVED).priceEstimate)
        assertNull(buildSubmission(days, ReviewDraft(priceText = ""), ReviewDecision.APPROVED).priceEstimate)
    }

    @Test
    fun earlierReviewPrefillsTheDraft() {
        val existing = listOf(
            ActivityFeedback(
                id = 1, shareId = 5, dayNumber = 2, activityIndex = 0, activityTitle = "City Palace",
                verdict = ActivityVerdict.NOT_AVAILABLE, comment = "Closed", suggestion = "",
                response = SuggestionResponse.PENDING
            )
        )
        val draft = initialDraft(share(comment = "Book early", price = 30000.0), existing)

        assertEquals(ActivityVerdict.NOT_AVAILABLE, draft.draftFor(ActivityKey(2, 0)).verdict)
        assertEquals(ActivityVerdict.AVAILABLE, draft.draftFor(ActivityKey(1, 0)).verdict)
        assertEquals("Book early", draft.overallComment)
        assertEquals("30000", draft.priceText)
        assertEquals(1, draft.issueCount)
    }
}
