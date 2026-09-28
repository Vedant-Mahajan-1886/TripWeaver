package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.agent.AgentRating
import com.example.smarttravelplanner.domain.model.agent.AgentReview
import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.QuoteInput
import kotlinx.coroutines.flow.Flow

/**
 * What happens after the traveller goes ahead with an agent: quote, booking,
 * completion and rating. Each action checks who is allowed to do it.
 */
interface BookingRepository {

    fun observeBooking(shareId: Long): Flow<Booking?>

    /** Bookings of every share of one trip, keyed by share id. */
    fun observeBookingsForTrip(tripId: Long): Flow<Map<Long, Booking>>

    // ── Agent ────────────────────────────────────────────────────────────────
    suspend fun sendQuote(shareId: Long, quote: QuoteInput): Result<Unit>
    suspend fun markBooked(shareId: Long, reference: String, notes: String): Result<Unit>
    /** Agent confirms the booking still works after the traveller changed the trip. */
    suspend fun reconfirm(shareId: Long): Result<Unit>

    // ── Traveller ────────────────────────────────────────────────────────────
    suspend fun acceptQuote(shareId: Long): Result<Unit>
    /** Declining cancels this booking and re-opens the other agents' offers. */
    suspend fun declineQuote(shareId: Long, reason: String): Result<Unit>
    suspend fun rateAgent(shareId: Long, stars: Int, comment: String): Result<Unit>

    // ── Either side ──────────────────────────────────────────────────────────
    /** Cancels before it's booked; the traveller can then choose another agent. */
    suspend fun cancel(shareId: Long, reason: String): Result<Unit>

    /** Marks booked trips whose end date has passed as Completed. Returns how many. */
    suspend fun completeFinishedTrips(): Int

    // ── Ratings ──────────────────────────────────────────────────────────────
    fun observeReviewForShare(shareId: Long): Flow<AgentReview?>
    fun observeReviewsForAgent(agentEmail: String): Flow<List<AgentReview>>
    /** Reviews of every agent on one trip, keyed by share id. */
    fun observeReviewsForTrip(tripId: Long): Flow<Map<Long, AgentReview>>
    /** Reviews of the logged-in agent, newest first. */
    fun observeMyReviews(): Flow<List<AgentReview>>
    /** Average rating per agent email. */
    fun observeRatings(): Flow<Map<String, AgentRating>>
}
