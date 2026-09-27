package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Every rule for editing a trip by hand. Pure Kotlin (no Android, no database)
 * so it can be unit tested. Each change returns the updated days plus a short,
 * human-readable summary that is stored in the trip's edit log (shown to agents).
 *
 * Days are identified by dayNumber, activities by their position in the day.
 * After every change activity ids are renumbered 0..n within each day.
 */
object TripEditor {

    const val MAX_TRIP_DAYS = 30

    /** Updated itinerary + what changed, in words. */
    data class Result(val days: List<Itinerary>, val summary: String)

    /** Result of changing the trip's dates. */
    data class Reshape(
        val days: List<Itinerary>,
        /** Days dropped because the trip got shorter (their activities are lost). */
        val removedDays: List<Itinerary>
    ) {
        val removedActivityCount: Int get() = removedDays.sumOf { it.activities.size }
    }

    // ── Trip details ─────────────────────────────────────────────────────────

    /** Problems with the edited details; empty list = OK to save. */
    fun validateDetails(
        title: String,
        startDate: LocalDate,
        endDate: LocalDate,
        budget: Double,
        adults: Int,
        kids: Int
    ): List<String> = buildList {
        if (title.isBlank()) add("Give your trip a name.")
        if (endDate.isBefore(startDate)) add("The end date can't be before the start date.")
        val length = ChronoUnit.DAYS.between(startDate, endDate) + 1
        if (length > MAX_TRIP_DAYS) add("A trip can be at most $MAX_TRIP_DAYS days long.")
        if (budget < 0 || budget.isNaN()) add("Budget can't be negative.")
        if (adults < 1) add("At least one adult is needed.")
        if (kids < 0) add("Kids can't be negative.")
    }

    /** Number of days a trip from [start] to [end] (inclusive) lasts. */
    fun dayCount(start: LocalDate, end: LocalDate): Int =
        (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)

    /**
     * Fits the itinerary to new dates: existing days keep their activities
     * and get the new calendar dates, extra days are added empty at the end,
     * and days past the new end date are removed.
     */
    fun reshapeDays(
        tripId: Long,
        current: List<Itinerary>,
        newStart: LocalDate,
        newEnd: LocalDate
    ): Reshape {
        val count = dayCount(newStart, newEnd)
        val byNumber = current.associateBy { it.dayNumber }
        val kept = (1..count).map { dayNumber ->
            val date = newStart.plusDays((dayNumber - 1).toLong()).toString()
            byNumber[dayNumber]?.copy(date = date)
                ?: Itinerary(tripId = tripId, dayNumber = dayNumber, date = date, activities = emptyList())
        }
        val removed = current.filter { it.dayNumber > count }.sortedBy { it.dayNumber }
        return Reshape(days = kept, removedDays = removed)
    }

    /** Short descriptions of what changed between two versions of the trip's details. */
    fun describeDetailChanges(old: Trip, new: Trip): List<String> = buildList {
        if (old.title.trim() != new.title.trim()) add("Renamed the trip to \u201C${new.title.trim()}\u201D")
        if (old.startDate != new.startDate || old.endDate != new.endDate) {
            add("Changed dates to ${formatRange(new.startDate, new.endDate)}")
        }
        if (old.budget != new.budget) add("Budget ${rupees(old.budget)} \u2192 ${rupees(new.budget)}")
        if (old.adults != new.adults || old.kids != new.kids) {
            add("Travellers ${travellers(old.adults, old.kids)} \u2192 ${travellers(new.adults, new.kids)}")
        }
        if (old.interests.toSet() != new.interests.toSet()) add("Updated interests")
        if (old.notes.trim() != new.notes.trim()) add("Updated notes")
    }

    // ── Activities ───────────────────────────────────────────────────────────

    fun updateActivity(days: List<Itinerary>, dayNumber: Int, index: Int, updated: Activity): Result {
        require(updated.title.isNotBlank()) { "An activity needs a title." }
        val day = requireDay(days, dayNumber)
        val old = requireActivity(day, index)
        val activities = day.activities.toMutableList().apply { this[index] = updated }
        val summary = if (old.title == updated.title) {
            "Edited \u201C${updated.title}\u201D (Day $dayNumber)"
        } else {
            "Changed \u201C${old.title}\u201D to \u201C${updated.title}\u201D (Day $dayNumber)"
        }
        return Result(replaceDay(days, day.copy(activities = activities)), summary)
    }

    fun addActivity(days: List<Itinerary>, dayNumber: Int, activity: Activity): Result {
        require(activity.title.isNotBlank()) { "An activity needs a title." }
        val day = requireDay(days, dayNumber)
        val activities = day.activities + activity
        return Result(
            replaceDay(days, day.copy(activities = activities)),
            "Added \u201C${activity.title}\u201D to Day $dayNumber"
        )
    }

    fun deleteActivity(days: List<Itinerary>, dayNumber: Int, index: Int): Result {
        val day = requireDay(days, dayNumber)
        val removed = requireActivity(day, index)
        val activities = day.activities.toMutableList().apply { removeAt(index) }
        return Result(
            replaceDay(days, day.copy(activities = activities)),
            "Removed \u201C${removed.title}\u201D from Day $dayNumber"
        )
    }

    /** Moves an activity to a new position inside the same day. */
    fun moveWithinDay(days: List<Itinerary>, dayNumber: Int, from: Int, to: Int): Result {
        val day = requireDay(days, dayNumber)
        val moving = requireActivity(day, from)
        require(to in day.activities.indices) { "Can't move there." }
        val activities = day.activities.toMutableList().apply {
            removeAt(from)
            add(to, moving)
        }
        return Result(
            replaceDay(days, day.copy(activities = activities)),
            "Reordered Day $dayNumber (\u201C${moving.title}\u201D moved ${if (to < from) "up" else "down"})"
        )
    }

    /** Moves an activity to the end of another day. */
    fun moveToDay(days: List<Itinerary>, fromDay: Int, index: Int, toDay: Int): Result {
        require(fromDay != toDay) { "It's already on Day $toDay." }
        val source = requireDay(days, fromDay)
        val target = requireDay(days, toDay)
        val moving = requireActivity(source, index)
        val updatedSource = source.copy(activities = source.activities.toMutableList().apply { removeAt(index) })
        val updatedTarget = target.copy(activities = target.activities + moving)
        return Result(
            replaceDay(replaceDay(days, updatedSource), updatedTarget),
            "Moved \u201C${moving.title}\u201D from Day $fromDay to Day $toDay"
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun requireDay(days: List<Itinerary>, dayNumber: Int): Itinerary =
        days.firstOrNull { it.dayNumber == dayNumber } ?: error("Day $dayNumber isn't in this trip.")

    private fun requireActivity(day: Itinerary, index: Int): Activity =
        day.activities.getOrNull(index) ?: error("That activity is no longer in Day ${day.dayNumber}.")

    /** Replaces one day and renumbers its activity ids 0..n. */
    private fun replaceDay(days: List<Itinerary>, updated: Itinerary): List<Itinerary> {
        val renumbered = updated.copy(
            activities = updated.activities.mapIndexed { i, a -> a.copy(id = i.toLong(), itineraryId = updated.id) }
        )
        return days.map { if (it.dayNumber == updated.dayNumber) renumbered else it }
    }

    private fun formatRange(start: LocalDate, end: LocalDate): String {
        val dm = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
        val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        return "${start.format(dm)} \u2013 ${end.format(full)}"
    }

    private fun rupees(amount: Double): String = "\u20B9" + String.format(Locale.US, "%,d", amount.toLong())

    private fun travellers(adults: Int, kids: Int): String = buildString {
        append("$adults adult${if (adults == 1) "" else "s"}")
        if (kids > 0) append(", $kids kid${if (kids == 1) "" else "s"}")
    }
}
