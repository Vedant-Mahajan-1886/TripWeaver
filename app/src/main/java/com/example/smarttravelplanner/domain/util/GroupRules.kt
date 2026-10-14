package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import java.time.LocalDate

/** Why a traveller can or can't join a group right now. */
enum class JoinCheck(val message: String) {
    OK(""),
    ALREADY_JOINED("You're already in this group."),
    FULL("This group is full."),
    CANCELLED("The agent cancelled this group."),
    NOT_PUBLISHED("This group isn't open yet."),
    STARTED("This group trip has already started.")
}

/** Rules for group trips. Pure functions, unit tested. */
object GroupRules {
    const val MIN_SEATS = 2
    const val MAX_SEATS = 40

    /** Problems with an agent's group before saving (empty list = OK). */
    fun validate(group: GroupTrip, today: LocalDate): List<String> = buildList {
        if (group.title.isBlank()) add("Give the group a title.")
        if (group.destination.isBlank()) add("Add a destination.")
        if (group.startDate.isBefore(today)) add("The start date can't be in the past.")
        if (group.endDate.isBefore(group.startDate)) add("The end date must be on or after the start date.")
        if (group.pricePerPerson <= 0) add("Set a price per person.")
        if (group.totalSeats !in MIN_SEATS..MAX_SEATS) add("Seats must be between $MIN_SEATS and $MAX_SEATS.")
        if (group.seatsTaken > group.totalSeats) add("${group.seatsTaken} people already joined; seats can't be fewer than that.")
    }

    /** Can a traveller join [group] today? */
    fun canJoin(group: GroupTrip, alreadyJoined: Boolean, today: LocalDate): JoinCheck = when {
        alreadyJoined -> JoinCheck.ALREADY_JOINED
        group.isCancelled -> JoinCheck.CANCELLED
        !group.isPublished -> JoinCheck.NOT_PUBLISHED
        !today.isBefore(group.startDate) -> JoinCheck.STARTED
        group.isFull -> JoinCheck.FULL
        else -> JoinCheck.OK
    }

    /** Members can leave until the day before the trip starts. */
    fun canLeave(group: GroupTrip, today: LocalDate): Boolean = today.isBefore(group.startDate)

    /** Label for listings, e.g. "3 seats left", "Last seat!", "Full". */
    fun seatsLabel(group: GroupTrip): String = when (group.seatsLeft) {
        0 -> "Full"
        1 -> "Last seat!"
        else -> "${group.seatsLeft} seats left"
    }
}
