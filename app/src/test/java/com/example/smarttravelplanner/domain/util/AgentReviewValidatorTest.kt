package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ActivityFeedbackInput
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AgentReviewSubmission
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentReviewValidatorTest {

    private fun activity(
        day: Int = 1,
        index: Int = 0,
        verdict: ActivityVerdict = ActivityVerdict.AVAILABLE,
        suggestion: String = ""
    ) = ActivityFeedbackInput(
        dayNumber = day,
        activityIndex = index,
        activityTitle = "Amber Fort",
        verdict = verdict,
        suggestion = suggestion
    )

    @Test
    fun simpleApprovalIsValid() {
        val review = AgentReviewSubmission(
            decision = ReviewDecision.APPROVED,
            activities = listOf(activity())
        )
        assertTrue(AgentReviewValidator.validate(review).isEmpty())
    }

    @Test
    fun negativePriceIsRejected() {
        val review = AgentReviewSubmission(decision = ReviewDecision.APPROVED, priceEstimate = -500.0)
        assertEquals(1, AgentReviewValidator.validate(review).size)
    }

    @Test
    fun suggestChangeNeedsASuggestion() {
        val review = AgentReviewSubmission(
            decision = ReviewDecision.CHANGES_REQUESTED,
            activities = listOf(activity(verdict = ActivityVerdict.SUGGEST_CHANGE))
        )
        val problems = AgentReviewValidator.validate(review)
        assertEquals(1, problems.size)
        assertTrue(problems.first().contains("Amber Fort"))
    }

    @Test
    fun suggestChangeWithSuggestionIsValid() {
        val review = AgentReviewSubmission(
            decision = ReviewDecision.CHANGES_REQUESTED,
            activities = listOf(activity(verdict = ActivityVerdict.SUGGEST_CHANGE, suggestion = "Nahargarh Fort at sunset"))
        )
        assertTrue(AgentReviewValidator.validate(review).isEmpty())
    }

    @Test
    fun changesRequestedNeedsAReason() {
        val noReason = AgentReviewSubmission(
            decision = ReviewDecision.CHANGES_REQUESTED,
            activities = listOf(activity())
        )
        assertEquals(1, AgentReviewValidator.validate(noReason).size)

        val withComment = noReason.copy(overallComment = "Hotels are full that week")
        assertTrue(AgentReviewValidator.validate(withComment).isEmpty())
    }

    @Test
    fun sameActivityTwiceIsRejected() {
        val review = AgentReviewSubmission(
            decision = ReviewDecision.APPROVED,
            activities = listOf(activity(day = 1, index = 0), activity(day = 1, index = 0))
        )
        assertEquals(1, AgentReviewValidator.validate(review).size)
    }
}
