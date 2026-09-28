package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Booking details for a trip share once the traveller has chosen that agent:
 * the agent's final quote, the booking reference, and cancellation info.
 * One row per share; deleted together with the share (and the trip).
 */
@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = TripShareEntity::class,
            parentColumns = ["id"],
            childColumns = ["shareId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookingEntity(
    @PrimaryKey val shareId: Long,
    // ── Quote (sent by the agent) ────────────────────────────────────────────
    val quoteTotal: Double? = null,
    val quoteIncludes: String = "",
    val quoteExcludes: String = "",
    /** How to pay, arranged outside the app (e.g. "50% advance via UPI to …") */
    val paymentNote: String = "",
    /** ISO date (yyyy-MM-dd) after which the quote expires, or null */
    val quoteValidUntil: String? = null,
    val quoteSentAt: Long? = null,
    // ── Traveller's answer ───────────────────────────────────────────────────
    val quoteRespondedAt: Long? = null,
    // ── Booking (done by the agent) ──────────────────────────────────────────
    val bookingReference: String = "",
    val bookingNotes: String = "",
    val bookedAt: Long? = null,
    /** When the agent last re-confirmed the booking after the traveller changed the trip */
    val reconfirmedAt: Long? = null,
    // ── Cancellation ─────────────────────────────────────────────────────────
    /** "TRAVELLER" or "AGENT" */
    val cancelledBy: String? = null,
    val cancelReason: String = "",
    val cancelledAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
