package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.model.agent.TripFeedbackItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentConflictsTest {

    private var nextId = 1L

    private fun item(
        shareId: Long,
        title: String,
        verdict: ActivityVerdict,
        response: SuggestionResponse = SuggestionResponse.PENDING,
        suggestion: String = "",
        updatedAt: Long = 0L,
        day: Int = 1
    ) = TripFeedbackItem(
        feedback = ActivityFeedback(
            id = nextId++, shareId = shareId, dayNumber = day, activityIndex = 0,
            activityTitle = title, verdict = verdict, comment = "", suggestion = suggestion,
            response = response, updatedAt = updatedAt
        ),
        agentEmail = "agent$shareId@x.com",
        tripId = 9
    )

    @Test
    fun applyingOneSuggestionSupersedesOtherAgentsOnTheSameActivity() {
        val a = item(shareId = 1, title = "Amber Fort", verdict = ActivityVerdict.SUGGEST_CHANGE, suggestion = "Nahargarh")
        val b = item(shareId = 2, title = "Amber Fort", verdict = ActivityVerdict.NOT_AVAILABLE)
        val otherActivity = item(shareId = 2, title = "Hawa Mahal", verdict = ActivityVerdict.NOT_AVAILABLE)
        val bConfirmed = item(shareId = 3, title = "Amber Fort", verdict = ActivityVerdict.AVAILABLE)

        val superseded = AgentConflicts.supersededBy(a.feedback, listOf(a, b, otherActivity, bConfirmed))

        assertEquals(listOf(b.feedback.id), superseded.map { it.id })
    }

    @Test
    fun alreadyAnsweredSuggestionsAreNotSuperseded() {
        val a = item(shareId = 1, title = "Amber Fort", verdict = ActivityVerdict.NOT_AVAILABLE)
        val declined = item(shareId = 2, title = "Amber Fort", verdict = ActivityVerdict.NOT_AVAILABLE, response = SuggestionResponse.DECLINED)
        assertTrue(AgentConflicts.supersededBy(a.feedback, listOf(a, declined)).isEmpty())
    }

    @Test
    fun conflictNeedsTwoDifferentAgentsOnTheSameActivity() {
        val all = listOf(
            item(shareId = 1, title = "Amber Fort", verdict = ActivityVerdict.SUGGEST_CHANGE),
            item(shareId = 2, title = "Amber Fort", verdict = ActivityVerdict.NOT_AVAILABLE),
            item(shareId = 1, title = "Hawa Mahal", verdict = ActivityVerdict.NOT_AVAILABLE),
            item(shareId = 2, title = "City Palace", verdict = ActivityVerdict.NOT_AVAILABLE, day = 2)
        )
        assertEquals(setOf(AgentConflicts.ActivityRef(1, "Amber Fort")), AgentConflicts.conflictingActivities(all))
    }

    @Test
    fun differentActivitiesAreNotAConflict() {
        // The scenario from the demo: two agents, two different removals
        val all = listOf(
            item(shareId = 1, title = "Hawa Mahal", verdict = ActivityVerdict.NOT_AVAILABLE),
            item(shareId = 2, title = "City Palace", verdict = ActivityVerdict.NOT_AVAILABLE)
        )
        assertTrue(AgentConflicts.conflictingActivities(all).isEmpty())
    }

    @Test
    fun changesSinceReviewListsOnlyLaterAppliedChangesWithAttribution() {
        val reviewedAt = 1_000L
        val all = listOf(
            item(shareId = 1, title = "Hawa Mahal", verdict = ActivityVerdict.NOT_AVAILABLE,
                response = SuggestionResponse.APPLIED, updatedAt = 2_000L),
            item(shareId = 2, title = "City Palace", verdict = ActivityVerdict.NOT_AVAILABLE,
                response = SuggestionResponse.APPLIED, updatedAt = 1_500L),
            item(shareId = 2, title = "Old change", verdict = ActivityVerdict.NOT_AVAILABLE,
                response = SuggestionResponse.APPLIED, updatedAt = 500L),
            item(shareId = 2, title = "Declined one", verdict = ActivityVerdict.NOT_AVAILABLE,
                response = SuggestionResponse.DECLINED, updatedAt = 3_000L)
        )

        val changes = AgentConflicts.changesSince(reviewedAt, myShareId = 1, all = all)

        assertEquals(listOf("City Palace", "Hawa Mahal"), changes.map { it.activityTitle })
        assertEquals(listOf(false, true), changes.map { it.suggestedByMe })
    }
}
