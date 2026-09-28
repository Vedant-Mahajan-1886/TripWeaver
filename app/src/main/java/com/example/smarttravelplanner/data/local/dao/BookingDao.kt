package com.example.smarttravelplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smarttravelplanner.data.local.entity.AgentReviewEntity
import com.example.smarttravelplanner.data.local.entity.BookingEntity
import kotlinx.coroutines.flow.Flow

/** Average rating of one agent. */
data class AgentRatingRow(
    val agentEmail: String,
    val average: Double,
    val count: Int
)

/** A booked share whose trip may have finished (used to mark trips Completed). */
data class BookedTripRow(
    val shareId: Long,
    val endDate: String
)

@Dao
interface BookingDao {

    // ── Bookings ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBooking(booking: BookingEntity)

    @Query("SELECT * FROM bookings WHERE shareId = :shareId LIMIT 1")
    suspend fun getBooking(shareId: Long): BookingEntity?

    @Query("SELECT * FROM bookings WHERE shareId = :shareId LIMIT 1")
    fun observeBooking(shareId: Long): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE shareId IN (SELECT id FROM trip_shares WHERE tripId = :tripId)")
    fun observeBookingsForTrip(tripId: Long): Flow<List<BookingEntity>>

    /** Booked shares with their trip's end date (to mark finished trips Completed). */
    @Query(
        """
        SELECT s.id AS shareId, t.endDate AS endDate
        FROM trip_shares s INNER JOIN trips t ON t.id = s.tripId
        WHERE s.status = 'BOOKED'
        """
    )
    suspend fun getBookedTrips(): List<BookedTripRow>

    /** Other agents' requests go back to "feedback received" when the chosen booking is cancelled. */
    @Query("UPDATE trip_shares SET status = 'RESPONDED' WHERE tripId = :tripId AND id != :cancelledShareId AND status = 'CLOSED'")
    suspend fun reopenClosedShares(tripId: Long, cancelledShareId: Long)

    // ── Reviews ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReview(review: AgentReviewEntity)

    @Query("SELECT * FROM agent_reviews WHERE shareId = :shareId LIMIT 1")
    fun observeReviewForShare(shareId: Long): Flow<AgentReviewEntity?>

    @Query("SELECT * FROM agent_reviews WHERE shareId = :shareId LIMIT 1")
    suspend fun getReviewForShare(shareId: Long): AgentReviewEntity?

    @Query("SELECT * FROM agent_reviews WHERE shareId IN (SELECT id FROM trip_shares WHERE tripId = :tripId)")
    fun observeReviewsForTrip(tripId: Long): Flow<List<AgentReviewEntity>>

    @Query("SELECT * FROM agent_reviews WHERE agentEmail = :agentEmail ORDER BY createdAt DESC")
    fun observeReviewsForAgent(agentEmail: String): Flow<List<AgentReviewEntity>>

    @Query("SELECT agentEmail, AVG(stars) AS average, COUNT(*) AS count FROM agent_reviews GROUP BY agentEmail")
    fun observeRatings(): Flow<List<AgentRatingRow>>
}
