package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Where a group trip is in its life, as the agent sees it. */
enum class GroupStatus(val label: String) {
    DRAFT("Draft"),
    OPEN("Open"),
    FULL("Full"),
    ONGOING("On the trip"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun of(group: GroupTrip, today: LocalDate): GroupStatus = when {
            group.isCancelled -> CANCELLED
            group.endDate.isBefore(today) -> COMPLETED
            !group.isPublished -> DRAFT
            !today.isBefore(group.startDate) -> ONGOING
            group.isFull -> FULL
            else -> OPEN
        }
    }
}

/** Text helpers for showing group trips. Pure functions, unit tested. */
object GroupFormat {
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.US)
    private val full = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)

    /** "12 \u2013 16 Nov 2026", "28 Nov \u2013 2 Dec 2026" or "12 Nov 2026". */
    fun dateRange(start: LocalDate, end: LocalDate): String = when {
        start == end -> start.format(full)
        start.year == end.year && start.month == end.month -> "${start.dayOfMonth} \u2013 ${end.format(full)}"
        start.year == end.year -> "${start.format(dayMonth)} \u2013 ${end.format(full)}"
        else -> "${start.format(full)} \u2013 ${end.format(full)}"
    }

    /** "\u20B912,500 / person" */
    fun price(pricePerPerson: Double): String =
        "\u20B9" + String.format(Locale.US, "%,d", pricePerPerson.toLong()) + " / person"

    /** "4 nights / 5 days" (a one-day trip is "Day trip"). */
    fun duration(group: GroupTrip): String {
        val nights = group.nights
        if (nights == 0L) return "Day trip"
        return "$nights night${if (nights == 1L) "" else "s"} / ${nights + 1} days"
    }

    /** "3 of 8 seats taken" */
    fun seatsTaken(group: GroupTrip): String = "${group.seatsTaken} of ${group.totalSeats} seats taken"
}
