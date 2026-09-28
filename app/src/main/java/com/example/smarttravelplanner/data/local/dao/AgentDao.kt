package com.example.smarttravelplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smarttravelplanner.data.local.entity.ActivityFeedbackEntity
import com.example.smarttravelplanner.data.local.entity.AgentProfileEntity
import com.example.smarttravelplanner.data.local.entity.TripShareEntity
import kotlinx.coroutines.flow.Flow

/** An agent account joined with its (optional) profile. */
data class AgentListRow(
    val email: String,
    val fullName: String,
    val agencyName: String?,
    val city: String?,
    /** JSON list from agent_profiles.specialities, or null if the agent has no profile yet */
    val specialitiesJson: String?
)

/** A share joined with the trip's headline details (for the agent dashboard). */
data class TripShareWithTripRow(
    @Embedded val share: TripShareEntity,
    val tripTitle: String,
    val tripDestination: String,
    val tripStartDate: String,
    val tripEndDate: String,
    val travellerName: String?
)

/** A traveller's share joined with the agent's agency name. */
data class TravellerShareRow(
    @Embedded val share: TripShareEntity,
    val agencyName: String?
)

/** A feedback row joined with the share it belongs to. */
data class TripFeedbackRow(
    @Embedded val feedback: ActivityFeedbackEntity,
    val agentEmail: String,
    val tripId: Long
)

@Dao
interface AgentDao {

    // ── Agent profiles ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAgentProfile(profile: AgentProfileEntity)

    @Query("SELECT * FROM agent_profiles WHERE email = :email LIMIT 1")
    fun observeAgentProfile(email: String): Flow<AgentProfileEntity?>

    /** Every account with role AGENT, with its profile if it has one. */
    @Query(
        """
        SELECT u.email AS email, u.fullName AS fullName,
               p.agencyName AS agencyName, p.city AS city, p.specialities AS specialitiesJson
        FROM users u
        LEFT JOIN agent_profiles p ON p.email = LOWER(u.email)
        WHERE u.role = 'AGENT'
        ORDER BY u.fullName COLLATE NOCASE
        """
    )
    fun observeAgents(): Flow<List<AgentListRow>>

    // ── Shares ───────────────────────────────────────────────────────────────

    /** Returns the new row id, or -1 if this trip is already shared with that agent. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShare(share: TripShareEntity): Long

    @Query("SELECT * FROM trip_shares WHERE id = :shareId LIMIT 1")
    suspend fun getShare(shareId: Long): TripShareEntity?

    @Query("SELECT * FROM trip_shares WHERE tripId = :tripId ORDER BY sentAt DESC")
    fun observeSharesForTrip(tripId: Long): Flow<List<TripShareEntity>>

    /** Re-sends a previously withdrawn share. */
    @Query(
        """
        UPDATE trip_shares
        SET status = 'SENT', message = :message, decision = NULL, overallComment = '',
            priceEstimate = NULL, sentAt = :now, updatedAt = :now
        WHERE tripId = :tripId AND agentEmail = :agentEmail AND status = 'WITHDRAWN'
        """
    )
    suspend fun resendWithdrawnShare(tripId: Long, agentEmail: String, message: String, now: Long): Int

    @Query(
        """
        SELECT s.*, t.title AS tripTitle, t.destination AS tripDestination,
               t.startDate AS tripStartDate, t.endDate AS tripEndDate,
               u.fullName AS travellerName
        FROM trip_shares s
        INNER JOIN trips t ON t.id = s.tripId
        LEFT JOIN users u ON LOWER(u.email) = s.travellerEmail
        WHERE s.agentEmail = :agentEmail AND s.status != 'WITHDRAWN'
        ORDER BY s.sentAt DESC
        """
    )
    fun observeSharesForAgent(agentEmail: String): Flow<List<TripShareWithTripRow>>

    @Query("UPDATE trip_shares SET status = :status, updatedAt = :now WHERE id = :shareId")
    suspend fun updateShareStatus(shareId: Long, status: String, now: Long)

    /** All open shares created by one traveller (for status badges on My Trips). */
    @Query(
        """
        SELECT s.*, p.agencyName AS agencyName
        FROM trip_shares s
        LEFT JOIN agent_profiles p ON p.email = s.agentEmail
        WHERE s.travellerEmail = :travellerEmail AND s.status != 'WITHDRAWN'
        """
    )
    fun observeSharesForTraveller(travellerEmail: String): Flow<List<TravellerShareRow>>

    /**
     * Sets the status without touching updatedAt, so "reviewed at" stays the time
     * of the agent's review (used for Chosen / Closed).
     */
    @Query("UPDATE trip_shares SET status = :status WHERE id = :shareId")
    suspend fun setShareStatusKeepingReviewTime(shareId: Long, status: String)

    /** Closes every other still-open share of the trip once the traveller has chosen an agent. */
    @Query(
        """
        UPDATE trip_shares SET status = 'CLOSED'
        WHERE tripId = :tripId AND id != :chosenShareId AND status NOT IN ('WITHDRAWN', 'CLOSED', 'CANCELLED')
        """
    )
    suspend fun closeOtherShares(tripId: Long, chosenShareId: Long)

    @Query(
        """
        UPDATE trip_shares
        SET status = 'RESPONDED', decision = :decision, overallComment = :overallComment,
            priceEstimate = :priceEstimate, updatedAt = :now
        WHERE id = :shareId
        """
    )
    suspend fun saveReviewSummary(
        shareId: Long,
        decision: String,
        overallComment: String,
        priceEstimate: Double?,
        now: Long
    )

    // ── Activity feedback ────────────────────────────────────────────────────

    @Insert
    suspend fun insertFeedback(feedback: List<ActivityFeedbackEntity>)

    @Query("DELETE FROM activity_feedback WHERE shareId = :shareId")
    suspend fun deleteFeedbackForShare(shareId: Long)

    @Query("SELECT * FROM activity_feedback WHERE shareId = :shareId ORDER BY dayNumber, activityIndex")
    fun observeFeedbackForShare(shareId: Long): Flow<List<ActivityFeedbackEntity>>

    @Query("UPDATE activity_feedback SET response = :response, updatedAt = :now WHERE id = :feedbackId")
    suspend fun updateFeedbackResponse(feedbackId: Long, response: String, now: Long)

    /** All agents' feedback on one trip (withdrawn shares excluded). */
    @Query(
        """
        SELECT f.*, s.agentEmail AS agentEmail, s.tripId AS tripId
        FROM activity_feedback f
        INNER JOIN trip_shares s ON s.id = f.shareId
        WHERE s.tripId = :tripId AND s.status != 'WITHDRAWN'
        ORDER BY f.dayNumber, f.activityIndex
        """
    )
    fun observeFeedbackForTrip(tripId: Long): Flow<List<TripFeedbackRow>>

    /** Applied feedback on every trip that was shared with [agentEmail] (for "Trip changed" badges). */
    @Query(
        """
        SELECT f.*, s.agentEmail AS agentEmail, s.tripId AS tripId
        FROM activity_feedback f
        INNER JOIN trip_shares s ON s.id = f.shareId
        WHERE f.response = 'APPLIED'
          AND s.tripId IN (SELECT tripId FROM trip_shares WHERE agentEmail = :agentEmail)
        """
    )
    fun observeAppliedFeedbackOnAgentTrips(agentEmail: String): Flow<List<TripFeedbackRow>>

    @Query("UPDATE activity_feedback SET response = 'SUPERSEDED', updatedAt = :now WHERE id IN (:feedbackIds)")
    suspend fun markSuperseded(feedbackIds: List<Long>, now: Long)
}
