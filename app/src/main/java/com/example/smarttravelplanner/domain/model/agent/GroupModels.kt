package com.example.smarttravelplanner.domain.model.agent

import java.time.LocalDate

/** A fixed-date group trip run by an agent, mainly for solo travellers. */
data class GroupTrip(
    val id: Long = 0,
    val agentEmail: String = "",
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val pricePerPerson: Double,
    val totalSeats: Int,
    val meetingPoint: String = "",
    val description: String = "",
    val packageId: Long? = null,
    val isPublished: Boolean = false,
    val isCancelled: Boolean = false,
    /** How many travellers have joined */
    val seatsTaken: Int = 0
) {
    val seatsLeft: Int get() = (totalSeats - seatsTaken).coerceAtLeast(0)
    val isFull: Boolean get() = seatsLeft == 0
    val nights: Long get() = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).coerceAtLeast(0)
}

/** A traveller who joined a group. */
data class GroupMember(
    val travellerEmail: String,
    val travellerName: String,
    val joinedAt: Long
)

/** A group as a traveller sees it: with the agent, their rating and whether I joined. */
data class GroupTripListing(
    val group: GroupTrip,
    val agent: AgentProfile?,
    val rating: AgentRating?,
    val joinedByMe: Boolean
)
