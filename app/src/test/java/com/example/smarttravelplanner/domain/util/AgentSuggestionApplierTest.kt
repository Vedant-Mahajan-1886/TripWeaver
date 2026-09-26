package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AgentSuggestionApplierTest {

    private val day1 = Itinerary(
        id = 1, tripId = 9, dayNumber = 1, date = "2026-10-12",
        activities = listOf(
            Activity(itineraryId = 1, title = "Amber Fort", startTime = "09:00", location = "Amer", lat = 26.98, lng = 75.85),
            Activity(itineraryId = 1, title = "Hawa Mahal", startTime = "13:00"),
            Activity(itineraryId = 1, title = "Chokhi Dhani", startTime = "19:00")
        )
    )

    private fun feedback(
        index: Int,
        title: String,
        verdict: ActivityVerdict,
        suggestion: String = "",
        comment: String = ""
    ) = ActivityFeedback(
        id = 1, shareId = 5, dayNumber = 1, activityIndex = index, activityTitle = title,
        verdict = verdict, comment = comment, suggestion = suggestion, response = SuggestionResponse.PENDING
    )

    @Test
    fun suggestChangeReplacesTitleAndKeepsTimeSlot() {
        val result = AgentSuggestionApplier.apply(
            listOf(day1),
            feedback(0, "Amber Fort", ActivityVerdict.SUGGEST_CHANGE, suggestion = "Nahargarh Fort", comment = "Less crowded")
        )
        val changed = result.activities[0]
        assertEquals("Nahargarh Fort", changed.title)
        assertEquals("09:00", changed.startTime)
        assertEquals("Less crowded", changed.description)
        assertEquals("", changed.location)
        assertNull(changed.lat)
        assertEquals(3, result.activities.size)
    }

    @Test
    fun notAvailableRemovesTheActivity() {
        val result = AgentSuggestionApplier.apply(listOf(day1), feedback(1, "Hawa Mahal", ActivityVerdict.NOT_AVAILABLE))
        assertEquals(listOf("Amber Fort", "Chokhi Dhani"), result.activities.map { it.title })
    }

    @Test
    fun findsActivityByTitleWhenPositionsShifted() {
        // Hawa Mahal was removed earlier, so Chokhi Dhani moved from index 2 to 1
        val shifted = AgentSuggestionApplier.apply(listOf(day1), feedback(1, "Hawa Mahal", ActivityVerdict.NOT_AVAILABLE))
        val result = AgentSuggestionApplier.apply(
            listOf(shifted),
            feedback(2, "Chokhi Dhani", ActivityVerdict.SUGGEST_CHANGE, suggestion = "Bar Palladio")
        )
        assertEquals(listOf("Amber Fort", "Bar Palladio"), result.activities.map { it.title })
    }

    @Test
    fun missingActivityGivesReadableError() {
        val error = assertThrows(IllegalStateException::class.java) {
            AgentSuggestionApplier.apply(listOf(day1), feedback(0, "City Palace", ActivityVerdict.NOT_AVAILABLE))
        }
        assertEquals("\"City Palace\" is no longer in Day 1.", error.message)
    }

    @Test
    fun availableHasNothingToApply() {
        assertThrows(IllegalStateException::class.java) {
            AgentSuggestionApplier.apply(listOf(day1), feedback(0, "Amber Fort", ActivityVerdict.AVAILABLE))
        }
    }
}
