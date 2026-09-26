package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.AgentReviewSubmission
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.model.agent.TripFeedbackItem
import com.example.smarttravelplanner.domain.model.agent.TripShare
import kotlinx.coroutines.flow.Flow

/**
 * Everything the travel-agent feature needs: agent profiles, sharing trips
 * with agents, and agents' feedback.
 *
 * Phase 1 uses [LocalAgentRepository] (Room, one phone). Phase 2 can add a
 * Firebase implementation of this same interface without changing any screen.
 */
interface AgentRepository {

    // ── Agents ───────────────────────────────────────────────────────────────

    /** All agent accounts, for the traveller's "Choose agent" screen. */
    fun observeAgents(): Flow<List<AgentProfile>>

    /** The logged-in agent's own profile, or null if not set up yet. */
    fun observeMyAgentProfile(): Flow<AgentProfile?>

    /** Creates or updates the logged-in agent's profile. */
    suspend fun saveMyAgentProfile(
        agencyName: String,
        city: String,
        specialities: List<String>
    ): Result<Unit>

    // ── Traveller side ───────────────────────────────────────────────────────

    /**
     * Sends [tripId] to each agent in [agentEmails]. Agents that already have
     * this trip are skipped (a withdrawn share is sent again).
     * Returns how many agents received it.
     */
    suspend fun shareTrip(tripId: Long, agentEmails: List<String>, message: String): Result<Int>

    /** Every share of one trip (one per agent), newest first. */
    fun observeSharesForTrip(tripId: Long): Flow<List<TripShare>>

    /** Stops an agent from seeing the trip. */
    suspend fun withdrawShare(shareId: Long): Result<Unit>

    /**
     * The traveller goes ahead with one agent after comparing offers: that share
     * becomes Chosen and every other open share of the trip becomes Closed.
     */
    suspend fun chooseAgent(tripId: Long, shareId: Long): Result<Unit>

    /** Marks one suggestion as applied or declined. */
    suspend fun respondToFeedback(feedbackId: Long, response: SuggestionResponse): Result<Unit>

    // ── Agent side ───────────────────────────────────────────────────────────

    /** Trips sent to the logged-in agent (withdrawn ones excluded). */
    fun observeIncomingShares(): Flow<List<IncomingShare>>

    /** Called when the agent opens a trip for the first time. */
    suspend fun markViewed(shareId: Long): Result<Unit>

    /** Saves the agent's review, replacing any earlier one for this share. */
    suspend fun submitReview(shareId: Long, review: AgentReviewSubmission): Result<Unit>

    // ── Both sides ───────────────────────────────────────────────────────────

    fun observeFeedback(shareId: Long): Flow<List<ActivityFeedback>>

    /** Every agent's feedback on one trip (to spot conflicts and changes). */
    fun observeFeedbackForTrip(tripId: Long): Flow<List<TripFeedbackItem>>

    /**
     * After [applied] changed an activity, marks other agents' pending
     * suggestions for that same activity as "Already changed".
     * Returns how many were marked.
     */
    suspend fun supersedeConflicting(tripId: Long, applied: ActivityFeedback): Result<Int>
}
