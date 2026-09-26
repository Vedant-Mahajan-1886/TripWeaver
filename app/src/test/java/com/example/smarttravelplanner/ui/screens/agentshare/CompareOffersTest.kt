package com.example.smarttravelplanner.ui.screens.agentshare

import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.model.agent.TripFeedbackItem
import com.example.smarttravelplanner.domain.model.agent.TripShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompareOffersTest {

    private fun row(
        id: Long,
        name: String,
        status: ShareStatus,
        decision: ReviewDecision? = null,
        price: Double? = null,
        reviewedAt: Long = 1_000L
    ) = AgentShareRow(
        agent = AgentProfile(email = "$name@x.com", fullName = name, agencyName = name),
        share = TripShare(
            id = id, tripId = 9, travellerEmail = "t@x.com", agentEmail = "$name@x.com",
            message = "", status = status, decision = decision, overallComment = "",
            priceEstimate = price, sentAt = 0, updatedAt = reviewedAt
        )
    )

    private fun feedback(
        shareId: Long,
        verdict: ActivityVerdict,
        response: SuggestionResponse = SuggestionResponse.PENDING,
        updatedAt: Long = 0
    ) = TripFeedbackItem(
        feedback = ActivityFeedback(
            id = shareId * 100 + verdict.ordinal, shareId = shareId, dayNumber = 1, activityIndex = 0,
            activityTitle = "A$shareId", verdict = verdict, comment = "", suggestion = "x",
            response = response, updatedAt = updatedAt
        ),
        agentEmail = "",
        tripId = 9
    )

    @Test
    fun approvedOffersComeFirstAndCheapestIsMarked() {
        val offers = buildOffers(
            rows = listOf(
                row(1, "Pending", ShareStatus.SENT),
                row(2, "Changes", ShareStatus.RESPONDED, ReviewDecision.CHANGES_REQUESTED, price = 30_000.0),
                row(3, "Approved", ShareStatus.RESPONDED, ReviewDecision.APPROVED, price = 45_000.0)
            ),
            tripFeedback = emptyList()
        )

        assertEquals(listOf("Approved", "Changes", "Pending"), offers.map { it.row.agent.fullName })
        assertTrue(offers.first { it.row.agent.fullName == "Changes" }.isLowestPrice)
        assertFalse(offers.first { it.row.agent.fullName == "Approved" }.isLowestPrice)
    }

    @Test
    fun countsFlaggedAndAppliedSuggestionsPerAgent() {
        val offers = buildOffers(
            rows = listOf(row(1, "A", ShareStatus.RESPONDED, ReviewDecision.CHANGES_REQUESTED)),
            tripFeedback = listOf(
                feedback(1, ActivityVerdict.NOT_AVAILABLE, SuggestionResponse.APPLIED),
                feedback(1, ActivityVerdict.SUGGEST_CHANGE),
                feedback(1, ActivityVerdict.AVAILABLE)
            )
        )
        assertEquals(2, offers.single().flaggedCount)
        assertEquals(1, offers.single().appliedCount)
    }

    @Test
    fun marksOffersReviewedBeforeChangesFromAnotherAgent() {
        val offers = buildOffers(
            rows = listOf(
                row(1, "Early", ShareStatus.RESPONDED, ReviewDecision.APPROVED, reviewedAt = 1_000L),
                row(2, "Other", ShareStatus.RESPONDED, ReviewDecision.CHANGES_REQUESTED, reviewedAt = 500L)
            ),
            // The traveller applied agent 2's suggestion after agent 1 reviewed
            tripFeedback = listOf(feedback(2, ActivityVerdict.NOT_AVAILABLE, SuggestionResponse.APPLIED, updatedAt = 2_000L))
        )
        assertTrue(offers.first { it.share.id == 1L }.reviewedBeforeLatestChanges)
        assertFalse(offers.first { it.share.id == 2L }.reviewedBeforeLatestChanges)
    }

    @Test
    fun withdrawnAgentsAreLeftOutAndClosedOnesKeepTheirReview() {
        val offers = buildOffers(
            rows = listOf(
                row(1, "Gone", ShareStatus.WITHDRAWN),
                row(2, "Chosen", ShareStatus.CONFIRMED, ReviewDecision.APPROVED),
                row(3, "Closed", ShareStatus.CLOSED, ReviewDecision.APPROVED)
            ),
            tripFeedback = emptyList()
        )
        assertEquals(listOf("Chosen", "Closed"), offers.map { it.row.agent.fullName })
        assertTrue(offers.all { it.hasResponded })
    }
}
