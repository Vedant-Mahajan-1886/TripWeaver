package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import java.time.LocalDate

/**
 * The booking lifecycle after the traveller goes ahead with an agent:
 *
 *   Chosen → Quote sent → Quote accepted → Booked → Completed
 *        \________ Cancelled (by either side, before Booked) ________/
 *
 * Pure Kotlin (no Android, no database) so every transition can be unit tested.
 */
object BookingRules {

    /** Agent can send (or update) a quote until the traveller has answered it. */
    fun canSendQuote(status: ShareStatus): Boolean =
        status == ShareStatus.CONFIRMED || status == ShareStatus.QUOTE_SENT

    /** Traveller can accept/decline while the quote is open and not expired. */
    fun canRespondToQuote(status: ShareStatus, validUntil: LocalDate?, today: LocalDate): Boolean =
        status == ShareStatus.QUOTE_SENT && !isQuoteExpired(validUntil, today)

    fun isQuoteExpired(validUntil: LocalDate?, today: LocalDate): Boolean =
        validUntil != null && today.isAfter(validUntil)

    fun canMarkBooked(status: ShareStatus): Boolean = status == ShareStatus.QUOTE_ACCEPTED

    /** Either side can cancel until it's booked. */
    fun canCancel(status: ShareStatus): Boolean =
        status == ShareStatus.CONFIRMED || status == ShareStatus.QUOTE_SENT || status == ShareStatus.QUOTE_ACCEPTED

    /** A booked trip becomes Completed the day after its end date. */
    fun isTripFinished(endDate: LocalDate, today: LocalDate): Boolean = today.isAfter(endDate)

    fun canRate(status: ShareStatus): Boolean = status == ShareStatus.COMPLETED

    /**
     * True if the traveller changed the trip after the agent last confirmed the booking,
     * so the agent should re-confirm.
     */
    fun needsReconfirm(status: ShareStatus, confirmedAt: Long?, lastTripChangeAt: Long?): Boolean =
        status == ShareStatus.BOOKED && confirmedAt != null && lastTripChangeAt != null && lastTripChangeAt > confirmedAt

    /** Problems with a quote; empty list = OK to send. */
    fun validateQuote(total: Double?, validUntil: LocalDate?, today: LocalDate): List<String> = buildList {
        if (total == null || total <= 0 || total.isNaN()) add("Enter the total price of the quote.")
        if (validUntil != null && validUntil.isBefore(today)) add("\"Valid until\" can't be in the past.")
    }

    fun validateRating(stars: Int): List<String> =
        if (stars in 1..5) emptyList() else listOf("Choose between 1 and 5 stars.")
}
