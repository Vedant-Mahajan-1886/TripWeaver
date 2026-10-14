package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Where a trip is relative to today. */
sealed interface TripTiming {
    data class Upcoming(val daysToGo: Long) : TripTiming
    data object StartsToday : TripTiming
    /** [day] is 1-based: day 1 of [totalDays] is the start date. */
    data class Ongoing(val day: Long, val totalDays: Long) : TripTiming
    data object Finished : TripTiming
    /** The dates could not be read. */
    data object Unknown : TripTiming
}

/** Works out [TripTiming] from ISO dates (yyyy-MM-dd). */
fun tripTiming(startDate: String, endDate: String, today: LocalDate): TripTiming {
    val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return TripTiming.Unknown
    val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: start
    return when {
        today.isBefore(start) -> TripTiming.Upcoming(ChronoUnit.DAYS.between(today, start))
        today == start -> TripTiming.StartsToday
        !today.isAfter(end) -> TripTiming.Ongoing(
            day = ChronoUnit.DAYS.between(start, today) + 1,
            totalDays = ChronoUnit.DAYS.between(start, end) + 1
        )
        else -> TripTiming.Finished
    }
}

/** Short label for the countdown pill. */
fun TripTiming.label(): String = when (this) {
    is TripTiming.Upcoming -> when (daysToGo) {
        1L -> "Tomorrow"
        in 2..13 -> "In $daysToGo days"
        else -> "In ${daysToGo / 7} weeks"
    }
    TripTiming.StartsToday -> "Starts today"
    is TripTiming.Ongoing -> "On trip \u00B7 day $day of $totalDays"
    TripTiming.Finished -> "Finished"
    TripTiming.Unknown -> "Date not set"
}

/** True when the agent should look at it soon: on the trip now, or starting within a week. */
fun TripTiming.isSoon(): Boolean = when (this) {
    is TripTiming.Upcoming -> daysToGo <= 7
    TripTiming.StartsToday, is TripTiming.Ongoing -> true
    else -> false
}

/** Booked trips that are on now or still to come, soonest first. */
fun upcomingBookedTrips(incoming: List<IncomingShare>, today: LocalDate, limit: Int = 5): List<IncomingShare> =
    incoming
        .filter { it.share.status == ShareStatus.BOOKED }
        .filter {
            val timing = tripTiming(it.tripStartDate, it.tripEndDate, today)
            timing != TripTiming.Finished && timing != TripTiming.Unknown
        }
        .sortedBy { LocalDate.parse(it.tripStartDate) }
        .take(limit)
