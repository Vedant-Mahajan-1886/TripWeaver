package com.example.smarttravelplanner.domain.model.agent

import java.time.LocalDate

/** Who did something in the booking flow. */
enum class BookingActor { TRAVELLER, AGENT }

/** The agent's final offer once the traveller has chosen them. */
data class QuoteInput(
    val total: Double,
    val includes: String = "",
    val excludes: String = "",
    /** How to pay, arranged outside the app */
    val paymentNote: String = "",
    val validUntil: LocalDate? = null
)

/** Booking details for one share (stage itself is the share's status). */
data class Booking(
    val shareId: Long,
    val quoteTotal: Double?,
    val quoteIncludes: String,
    val quoteExcludes: String,
    val paymentNote: String,
    val quoteValidUntil: LocalDate?,
    val quoteSentAt: Long?,
    val quoteRespondedAt: Long?,
    val bookingReference: String,
    val bookingNotes: String,
    val bookedAt: Long?,
    val reconfirmedAt: Long?,
    val cancelledBy: BookingActor?,
    val cancelReason: String,
    val cancelledAt: Long?
) {
    /** Latest moment the agent confirmed the booking (booked or re-confirmed). */
    val confirmedAt: Long? get() = listOfNotNull(bookedAt, reconfirmedAt).maxOrNull()
}

/** A traveller's rating of an agent. */
data class AgentReview(
    val shareId: Long,
    val agentEmail: String,
    val tripDestination: String,
    val stars: Int,
    val comment: String,
    val createdAt: Long,
    /** The agent's public reply; empty = none. */
    val reply: String = "",
    val repliedAt: Long? = null
) {
    val hasReply: Boolean get() = reply.isNotBlank()
}

/** Average rating of an agent. */
data class AgentRating(val average: Double, val count: Int) {
    /** e.g. "4.6 ★ (12)" */
    val label: String get() = String.format(java.util.Locale.US, "%.1f \u2605 (%d)", average, count)
}
