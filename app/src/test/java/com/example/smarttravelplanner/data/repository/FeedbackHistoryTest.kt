package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.local.entity.ActivityFeedbackEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackHistoryTest {

    private fun row(
        title: String,
        verdict: String,
        suggestion: String = "",
        response: String = "PENDING",
        updatedAt: Long = 0L,
        id: Long = 0L
    ) = ActivityFeedbackEntity(
        id = id, shareId = 5, dayNumber = 1, activityIndex = 0, activityTitle = title,
        verdict = verdict, suggestion = suggestion, response = response, updatedAt = updatedAt
    )

    @Test
    fun unchangedAnswerKeepsTravellersDecision() {
        val old = listOf(row("Hawa Mahal", "NOT_AVAILABLE", response = "DECLINED", updatedAt = 100, id = 1))
        val new = listOf(row("Hawa Mahal", "NOT_AVAILABLE", updatedAt = 500))

        val merged = mergeWithHistory(old, new)

        assertEquals(1, merged.size)
        assertEquals("DECLINED", merged.single().response)
        assertEquals(100L, merged.single().updatedAt)
    }

    @Test
    fun changedAnswerStartsPendingAgain() {
        val old = listOf(row("Hawa Mahal", "SUGGEST_CHANGE", suggestion = "Jal Mahal", response = "DECLINED"))
        val new = listOf(row("Hawa Mahal", "SUGGEST_CHANGE", suggestion = "City Palace"))
        assertEquals("PENDING", mergeWithHistory(old, new).single().response)
    }

    @Test
    fun appliedSuggestionsStayAsHistory() {
        // Traveller applied "remove Amber Fort", so the new review no longer mentions it
        val old = listOf(row("Amber Fort", "NOT_AVAILABLE", response = "APPLIED", updatedAt = 100, id = 7))
        val new = listOf(row("Hawa Mahal", "AVAILABLE"))

        val merged = mergeWithHistory(old, new)

        assertEquals(listOf("Hawa Mahal", "Amber Fort"), merged.map { it.activityTitle })
        val history = merged.last()
        assertEquals("APPLIED", history.response)
        assertEquals(0L, history.id) // re-inserted as a new row
    }

    @Test
    fun oldPendingAnswersAreDropped() {
        val old = listOf(row("Amber Fort", "NOT_AVAILABLE"))
        val new = listOf(row("Amber Fort", "AVAILABLE"))
        assertEquals(listOf("AVAILABLE"), mergeWithHistory(old, new).map { it.verdict })
    }
}
