package com.example.smarttravelplanner.data.repository

import androidx.room.withTransaction
import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.AgentListRow
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.dao.TripShareWithTripRow
import com.example.smarttravelplanner.data.local.db.Converters
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.local.entity.ActivityFeedbackEntity
import com.example.smarttravelplanner.data.local.entity.AgentProfileEntity
import com.example.smarttravelplanner.data.local.entity.TripShareEntity
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.AgentReviewSubmission
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.model.agent.TripShare
import com.example.smarttravelplanner.domain.util.AgentReviewValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [AgentRepository]: traveller and agent use the same phone. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalAgentRepository @Inject constructor(
    private val database: TravelDatabase,
    private val agentDao: AgentDao,
    private val tripDao: TripDao,
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository
) : AgentRepository {

    private val converters = Converters()

    // ── Agents ───────────────────────────────────────────────────────────────

    override fun observeAgents(): Flow<List<AgentProfile>> =
        agentDao.observeAgents().map { rows -> rows.map { it.toDomain() } }

    override fun observeMyAgentProfile(): Flow<AgentProfile?> =
        currentEmailFlow().flatMapLatest { email ->
            if (email.isEmpty()) flowOf(null)
            else agentDao.observeAgentProfile(email).map { entity ->
                entity?.let {
                    AgentProfile(
                        email = it.email,
                        fullName = "",
                        agencyName = it.agencyName,
                        city = it.city,
                        specialities = it.specialities
                    )
                }
            }
        }

    override suspend fun saveMyAgentProfile(
        agencyName: String,
        city: String,
        specialities: List<String>
    ): Result<Unit> = runCatching {
        val email = requireEmail()
        require(agencyName.isNotBlank()) { "Agency name is required." }
        agentDao.upsertAgentProfile(
            AgentProfileEntity(
                email = email,
                agencyName = agencyName.trim(),
                city = city.trim(),
                specialities = specialities.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            )
        )
    }

    // ── Traveller side ───────────────────────────────────────────────────────

    override suspend fun shareTrip(
        tripId: Long,
        agentEmails: List<String>,
        message: String
    ): Result<Int> = runCatching {
        val traveller = requireEmail()
        val trip = requireNotNull(tripDao.getTripById(tripId)) { "Trip not found." }
        require(trip.ownerEmail == traveller) { "You can only share your own trips." }

        val targets = agentEmails.map { it.normalized() }.filter { it.isNotEmpty() }.distinct()
        require(targets.isNotEmpty()) { "Choose at least one agent." }

        val now = System.currentTimeMillis()
        database.withTransaction {
            targets.count { agentEmail ->
                val inserted = agentDao.insertShare(
                    TripShareEntity(
                        tripId = tripId,
                        travellerEmail = traveller,
                        agentEmail = agentEmail,
                        message = message.trim(),
                        sentAt = now,
                        updatedAt = now
                    )
                )
                inserted != -1L ||
                    agentDao.resendWithdrawnShare(tripId, agentEmail, message.trim(), now) > 0
            }
        }
    }

    override fun observeSharesForTrip(tripId: Long): Flow<List<TripShare>> =
        agentDao.observeSharesForTrip(tripId).map { list -> list.map { it.toDomain() } }

    override suspend fun withdrawShare(shareId: Long): Result<Unit> = runCatching {
        val share = requireShare(shareId)
        require(share.travellerEmail == requireEmail()) { "Only the traveller can withdraw this." }
        agentDao.updateShareStatus(shareId, ShareStatus.WITHDRAWN.name, System.currentTimeMillis())
    }

    override suspend fun respondToFeedback(
        feedbackId: Long,
        response: SuggestionResponse
    ): Result<Unit> = runCatching {
        agentDao.updateFeedbackResponse(feedbackId, response.name, System.currentTimeMillis())
    }

    // ── Agent side ───────────────────────────────────────────────────────────

    override fun observeIncomingShares(): Flow<List<IncomingShare>> =
        currentEmailFlow().flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyList())
            else agentDao.observeSharesForAgent(email).map { rows -> rows.map { it.toDomain() } }
        }

    override suspend fun markViewed(shareId: Long): Result<Unit> = runCatching {
        val share = requireShareForCurrentAgent(shareId)
        if (ShareStatus.fromStorage(share.status) == ShareStatus.SENT) {
            agentDao.updateShareStatus(shareId, ShareStatus.VIEWED.name, System.currentTimeMillis())
        }
    }

    override suspend fun submitReview(
        shareId: Long,
        review: AgentReviewSubmission
    ): Result<Unit> = runCatching {
        requireShareForCurrentAgent(shareId)
        val problems = AgentReviewValidator.validate(review)
        require(problems.isEmpty()) { problems.joinToString("\n") }

        val now = System.currentTimeMillis()
        database.withTransaction {
            agentDao.deleteFeedbackForShare(shareId)
            agentDao.insertFeedback(
                review.activities.map { input ->
                    ActivityFeedbackEntity(
                        shareId = shareId,
                        dayNumber = input.dayNumber,
                        activityIndex = input.activityIndex,
                        activityTitle = input.activityTitle,
                        verdict = input.verdict.name,
                        comment = input.comment.trim(),
                        suggestion = input.suggestion.trim(),
                        updatedAt = now
                    )
                }
            )
            agentDao.saveReviewSummary(
                shareId = shareId,
                decision = review.decision.name,
                overallComment = review.overallComment.trim(),
                priceEstimate = review.priceEstimate,
                now = now
            )
        }
    }

    // ── Both sides ───────────────────────────────────────────────────────────

    override fun observeFeedback(shareId: Long): Flow<List<ActivityFeedback>> =
        agentDao.observeFeedbackForShare(shareId).map { list -> list.map { it.toDomain() } }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun currentEmailFlow(): Flow<String> =
        authRepository.currentUserEmail.map { it.normalized() }.distinctUntilChanged()

    private suspend fun requireEmail(): String {
        val email = authRepository.currentUserEmail.first().normalized()
        require(email.isNotEmpty()) { "Please log in first." }
        return email
    }

    private suspend fun requireShare(shareId: Long): TripShareEntity =
        requireNotNull(agentDao.getShare(shareId)) { "This trip is no longer shared." }

    private suspend fun requireShareForCurrentAgent(shareId: Long): TripShareEntity {
        val share = requireShare(shareId)
        require(userRoleRepository.currentUserRole.first() == UserRole.AGENT) {
            "Only agent accounts can review trips."
        }
        require(share.agentEmail == requireEmail()) { "This trip was sent to a different agent." }
        require(ShareStatus.fromStorage(share.status) != ShareStatus.WITHDRAWN) {
            "The traveller withdrew this trip."
        }
        return share
    }

    private fun String?.normalized(): String = this?.trim()?.lowercase().orEmpty()

    private fun AgentListRow.toDomain() = AgentProfile(
        email = email.normalized(),
        fullName = fullName,
        agencyName = agencyName.orEmpty(),
        city = city.orEmpty(),
        specialities = specialitiesJson?.let { converters.toStringList(it) }.orEmpty()
    )

    private fun TripShareEntity.toDomain() = TripShare(
        id = id,
        tripId = tripId,
        travellerEmail = travellerEmail,
        agentEmail = agentEmail,
        message = message,
        status = ShareStatus.fromStorage(status),
        decision = ReviewDecision.fromStorage(decision),
        overallComment = overallComment,
        priceEstimate = priceEstimate,
        sentAt = sentAt,
        updatedAt = updatedAt
    )

    private fun TripShareWithTripRow.toDomain() = IncomingShare(
        share = share.toDomain(),
        tripTitle = tripTitle,
        tripDestination = tripDestination,
        tripStartDate = tripStartDate,
        tripEndDate = tripEndDate,
        travellerName = travellerName ?: share.travellerEmail
    )

    private fun ActivityFeedbackEntity.toDomain() = ActivityFeedback(
        id = id,
        shareId = shareId,
        dayNumber = dayNumber,
        activityIndex = activityIndex,
        activityTitle = activityTitle,
        verdict = ActivityVerdict.fromStorage(verdict),
        comment = comment,
        suggestion = suggestion,
        response = SuggestionResponse.fromStorage(response)
    )
}
