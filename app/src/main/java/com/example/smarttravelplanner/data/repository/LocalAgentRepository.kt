package com.example.smarttravelplanner.data.repository

import androidx.room.withTransaction
import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.AgentListRow
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.dao.TripEditDao
import com.example.smarttravelplanner.data.local.dao.TripFeedbackRow
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
import com.example.smarttravelplanner.domain.model.agent.TripFeedbackItem
import com.example.smarttravelplanner.domain.model.agent.TripShare
import com.example.smarttravelplanner.domain.util.AgentConflicts
import com.example.smarttravelplanner.domain.util.AgentReviewValidator
import com.example.smarttravelplanner.domain.util.tripAgentStatusLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val userRoleRepository: UserRoleRepository,
    private val tripEditDao: TripEditDao
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
        val alreadyChosen = agentDao.observeSharesForTrip(tripId).first()
            .any { ShareStatus.fromStorage(it.status).isChosen }
        require(!alreadyChosen) { "You've already chosen an agent for this trip." }

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

    override fun observeMyTripAgentStatus(): Flow<Map<Long, String>> =
        currentEmailFlow().flatMapLatest { email ->
            if (email.isEmpty()) flowOf(emptyMap())
            else agentDao.observeSharesForTraveller(email).map { rows ->
                rows.groupBy { it.share.tripId }.mapValues { (_, tripRows) ->
                    tripAgentStatusLabel(
                        statuses = tripRows.map { ShareStatus.fromStorage(it.share.status) to ReviewDecision.fromStorage(it.share.decision) },
                        chosenAgency = tripRows.firstOrNull {
                            ShareStatus.fromStorage(it.share.status).isChosen
                        }?.agencyName
                    )
                }.filterValues { it != null }.mapValues { it.value!! }
            }
        }

    override suspend fun chooseAgent(tripId: Long, shareId: Long): Result<Unit> = runCatching {
        val share = requireShare(shareId)
        require(share.tripId == tripId) { "This offer belongs to a different trip." }
        require(share.travellerEmail == requireEmail()) { "Only the traveller can choose an agent." }
        require(ShareStatus.fromStorage(share.status) == ShareStatus.RESPONDED) {
            "You can choose an agent once they've sent their review."
        }
        database.withTransaction {
            agentDao.setShareStatusKeepingReviewTime(shareId, ShareStatus.CONFIRMED.name)
            agentDao.closeOtherShares(tripId, shareId)
        }
    }

    override suspend fun withdrawShare(shareId: Long): Result<Unit> = runCatching {
        val share = requireShare(shareId)
        require(share.travellerEmail == requireEmail()) { "Only the traveller can withdraw this." }
        require(!ShareStatus.fromStorage(share.status).isLocked) { "This request is already closed." }
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
            else combine(
                agentDao.observeSharesForAgent(email),
                agentDao.observeAppliedFeedbackOnAgentTrips(email),
                tripEditDao.observeEditsOnAgentTrips(email)
            ) { rows, applied, edits ->
                rows.map { row ->
                    val reviewedAt = row.share.updatedAt
                    // Changed = an agent suggestion was applied OR the traveller edited the trip by hand
                    val changed = row.share.decision != null && (
                        applied.any { it.tripId == row.share.tripId && it.feedback.updatedAt > reviewedAt } ||
                            edits.any { it.tripId == row.share.tripId && it.editedAt > reviewedAt }
                        )
                    row.toDomain().copy(tripChangedSinceReview = changed)
                }
            }
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
        val share = requireShareForCurrentAgent(shareId)
        require(!ShareStatus.fromStorage(share.status).isLocked) {
            "The traveller has already made their choice, so this review can't be changed."
        }
        val problems = AgentReviewValidator.validate(review)
        require(problems.isEmpty()) { problems.joinToString("\n") }

        val now = System.currentTimeMillis()
        database.withTransaction {
            val previous = agentDao.observeFeedbackForShare(shareId).first()
            val fresh = review.activities.map { input ->
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
            agentDao.deleteFeedbackForShare(shareId)
            // Keep the traveller's earlier Applied/Declined answers (see FeedbackHistory.kt)
            agentDao.insertFeedback(mergeWithHistory(previous, fresh))
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

    override fun observeFeedbackForTrip(tripId: Long): Flow<List<TripFeedbackItem>> =
        agentDao.observeFeedbackForTrip(tripId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun supersedeConflicting(tripId: Long, applied: ActivityFeedback): Result<Int> =
        runCatching {
            val all = agentDao.observeFeedbackForTrip(tripId).first().map { it.toDomain() }
            val ids = AgentConflicts.supersededBy(applied, all).map { it.id }
            if (ids.isNotEmpty()) agentDao.markSuperseded(ids, System.currentTimeMillis())
            ids.size
        }

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
        response = SuggestionResponse.fromStorage(response),
        updatedAt = updatedAt
    )

    private fun TripFeedbackRow.toDomain() = TripFeedbackItem(
        feedback = feedback.toDomain(),
        agentEmail = agentEmail,
        tripId = tripId
    )
}
