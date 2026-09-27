package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripEditorTest {

    private val start = LocalDate.of(2026, 10, 12)

    private fun act(title: String) = Activity(itineraryId = 0, title = title)

    private val days = listOf(
        Itinerary(id = 11, tripId = 9, dayNumber = 1, date = "2026-10-12",
            activities = listOf(act("Amber Fort"), act("Hawa Mahal"), act("Chokhi Dhani"))),
        Itinerary(id = 12, tripId = 9, dayNumber = 2, date = "2026-10-13",
            activities = listOf(act("City Palace"))),
        Itinerary(id = 13, tripId = 9, dayNumber = 3, date = "2026-10-14",
            activities = listOf(act("Nahargarh Fort")))
    )

    private fun titles(result: List<Itinerary>, day: Int) =
        result.first { it.dayNumber == day }.activities.map { it.title }

    // ── Details ─────────────────────────────────────────────────────────────

    @Test
    fun validDetailsHaveNoProblems() {
        assertTrue(TripEditor.validateDetails("Jaipur", start, start.plusDays(2), 25000.0, 2, 0).isEmpty())
    }

    @Test
    fun invalidDetailsAreReported() {
        val problems = TripEditor.validateDetails("  ", start, start.minusDays(1), -5.0, 0, -1)
        assertEquals(5, problems.size)
    }

    @Test
    fun tripCannotBeLongerThanThirtyDays() {
        val problems = TripEditor.validateDetails("Long", start, start.plusDays(30), 0.0, 1, 0)
        assertEquals(listOf("A trip can be at most 30 days long."), problems)
    }

    @Test
    fun describesDetailChanges() {
        val old = Trip(id = 9, title = "Jaipur", destination = "Jaipur", startDate = start,
            endDate = start.plusDays(2), budget = 25000.0, adults = 2)
        val new = old.copy(title = "Pink City", budget = 30000.0, kids = 1)
        assertEquals(
            listOf(
                "Renamed the trip to \u201CPink City\u201D",
                "Budget \u20B925,000 \u2192 \u20B930,000",
                "Travellers 2 adults \u2192 2 adults, 1 kid"
            ),
            TripEditor.describeDetailChanges(old, new)
        )
    }

    // ── Dates ───────────────────────────────────────────────────────────────

    @Test
    fun longerTripAddsEmptyDaysAndShiftsDates() {
        val result = TripEditor.reshapeDays(9, days, start.plusDays(1), start.plusDays(4))
        assertEquals(4, result.days.size)
        assertEquals("2026-10-13", result.days[0].date)
        assertEquals(listOf("Amber Fort", "Hawa Mahal", "Chokhi Dhani"), result.days[0].activities.map { it.title })
        assertTrue(result.days[3].activities.isEmpty())
        assertEquals(0L, result.days[3].id) // new day, inserted on save
        assertTrue(result.removedDays.isEmpty())
    }

    @Test
    fun shorterTripDropsExtraDaysAndCountsLostActivities() {
        val result = TripEditor.reshapeDays(9, days, start, start.plusDays(1))
        assertEquals(listOf(1, 2), result.days.map { it.dayNumber })
        assertEquals(listOf(3), result.removedDays.map { it.dayNumber })
        assertEquals(1, result.removedActivityCount)
    }

    // ── Activities ──────────────────────────────────────────────────────────

    @Test
    fun editRenamesAndDescribesTheChange() {
        val result = TripEditor.updateActivity(days, 1, 1, act("Jal Mahal"))
        assertEquals(listOf("Amber Fort", "Jal Mahal", "Chokhi Dhani"), titles(result.days, 1))
        assertEquals("Changed \u201CHawa Mahal\u201D to \u201CJal Mahal\u201D (Day 1)", result.summary)
    }

    @Test
    fun addAppendsToTheDay() {
        val result = TripEditor.addActivity(days, 2, act("Albert Hall"))
        assertEquals(listOf("City Palace", "Albert Hall"), titles(result.days, 2))
        assertEquals("Added \u201CAlbert Hall\u201D to Day 2", result.summary)
    }

    @Test
    fun deleteRemovesAndRenumbersIds() {
        val result = TripEditor.deleteActivity(days, 1, 0)
        val day1 = result.days.first { it.dayNumber == 1 }
        assertEquals(listOf("Hawa Mahal", "Chokhi Dhani"), day1.activities.map { it.title })
        assertEquals(listOf(0L, 1L), day1.activities.map { it.id })
        assertEquals(listOf(11L, 11L), day1.activities.map { it.itineraryId })
    }

    @Test
    fun moveWithinDayReorders() {
        val result = TripEditor.moveWithinDay(days, 1, 2, 0)
        assertEquals(listOf("Chokhi Dhani", "Amber Fort", "Hawa Mahal"), titles(result.days, 1))
    }

    @Test
    fun moveToAnotherDay() {
        val result = TripEditor.moveToDay(days, 1, 0, 3)
        assertEquals(listOf("Hawa Mahal", "Chokhi Dhani"), titles(result.days, 1))
        assertEquals(listOf("Nahargarh Fort", "Amber Fort"), titles(result.days, 3))
        assertEquals("Moved \u201CAmber Fort\u201D from Day 1 to Day 3", result.summary)
    }

    @Test
    fun blankTitleOrMissingActivityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { TripEditor.addActivity(days, 1, act(" ")) }
        assertThrows(IllegalStateException::class.java) { TripEditor.deleteActivity(days, 2, 5) }
        assertThrows(IllegalStateException::class.java) { TripEditor.deleteActivity(days, 7, 0) }
    }

    // ── Activity form ───────────────────────────────────────────────────────

    @Test
    fun timesAreValidated() {
        assertTrue(TripEditor.isValidTime("09:30"))
        assertTrue(TripEditor.isValidTime("9:05"))
        assertTrue(TripEditor.isValidTime(""))
        assertTrue(!TripEditor.isValidTime("25:00"))
        assertTrue(!TripEditor.isValidTime("9.30"))
    }

    @Test
    fun activityFormProblems() {
        assertTrue(TripEditor.validateActivity("Amber Fort", "09:00", "11:00", 500.0).isEmpty())
        assertEquals(
            listOf("Give the activity a title.", "End time should be after the start time.", "Cost can't be negative."),
            TripEditor.validateActivity(" ", "11:00", "10:00", -1.0)
        )
    }
}
