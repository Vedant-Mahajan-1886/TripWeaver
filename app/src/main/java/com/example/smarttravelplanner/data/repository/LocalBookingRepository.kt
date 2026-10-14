package com.example.smarttravelplanner.data.repository

import androidx.room.withTransaction
import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.BookingDao
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.local.entity.AgentReviewEntity
import com.example.smarttravelplanner.data.local.entity.BookingEntity
import com.example.smarttravelplanner.data.local.entity.TripShareEntity
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.domain.model.agent.AgentRating
import com.example.smarttravelplanner.domain.model.agent.AgentReview
import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.BookingActor
import com.example.smarttravelplanner.domain.model.agent.QuoteInput
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.util.BookingRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Room-backed [BookingRepository] (traveller and agent on the same phone). */
class LocalBookingRepository @Inject constructor(
    private val database: TravelDatabase,
    private val bookingDao: BookingDao,
    private val agentDao: AgentDao,
    private val tripDao: TripDao,
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository
) : BookingRepository {

    override fun observeBooking(shareId: Long): Flow<Booking?> =
        bookingDao.observeBooking(shareId).map { it?.toDomain() }

    override fun observeBookingsForTrip(tripId: Long): Flow<Map<Long, Booking>> =
        bookingDao.observeBookingsForTrip(tripId).map { rows -> rows.associate { it.shareId to it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyBookings(): Flow<Map<Long, Booking>> =
        authRepository.currentUserEmail
            .map { it?.trim()?.lowercase().orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(emptyMap())
                else bookingDao.observeBookingsForAgent(email).map { rows -> rows.associate { it.shareId to it.toDomain() } }
            }

    // ── Agent ────────────────────────────────────────────────────────────────

    override suspend fun sendQuote(shareId: Long, quote: QuoteInput): Result<Unit> = runCatching {
        val share = requireShareForAgent(shareId)
        val status = ShareStatus.fromStorage(share.status)
        require(BookingRules.canSendQuote(status)) { "A quote can only be sent before the traveller answers it." }
        val problems = BookingRules.validateQuote(quote.total, quote.validUntil, LocalDate.now())
        require(problems.isEmpty()) { problems.joinToString("\n") }

        val now = System.currentTimeMillis()
        database.withTransaction {
            val current = bookingDao.getBooking(shareId) ?: BookingEntity(shareId = shareId)
            bookingDao.upsertBooking(
                current.copy(
                    quoteTotal = quote.total,
                    quoteIncludes = quote.includes.trim(),
                    quoteExcludes = quote.excludes.trim(),
                    paymentNote = quote.paymentNote.trim(),
                    quoteValidUntil = quote.validUntil?.toString(),
                    quoteSentAt = now,
                    quoteRespondedAt = null,
                    updatedAt = now
                )
            )
            agentDao.setShareStatusKeepingReviewTime(shareId, ShareStatus.QUOTE_SENT.name)
        }
    }

    override suspend fun markBooked(shareId: Long, reference: String, notes: String): Result<Unit> = runCatching {
        val share = requireShareForAgent(shareId)
        require(BookingRules.canMarkBooked(ShareStatus.fromStorage(share.status))) {
            "You can mark it booked after the traveller accepts your quote."
        }
        require(reference.isNotBlank()) { "Add a booking reference." }
        val now = System.currentTimeMillis()
        database.withTransaction {
            val current = bookingDao.getBooking(shareId) ?: BookingEntity(shareId = shareId)
            bookingDao.upsertBooking(
                current.copy(
                    bookingReference = reference.trim(),
                    bookingNotes = notes.trim(),
                    bookedAt = now,
                    updatedAt = now
                )
            )
            agentDao.setShareStatusKeepingReviewTime(shareId, ShareStatus.BOOKED.name)
        }
    }

    override suspend fun reconfirm(shareId: Long): Result<Unit> = runCatching {
        val share = requireShareForAgent(shareId)
        require(ShareStatus.fromStorage(share.status) == ShareStatus.BOOKED) { "Only booked trips can be re-confirmed." }
        val current = requireNotNull(bookingDao.getBooking(shareId)) { "No booking found." }
        val now = System.currentTimeMillis()
        bookingDao.upsertBooking(current.copy(reconfirmedAt = now, updatedAt = now))
    }

    // ── Traveller ────────────────────────────────────────────────────────────

    override suspend fun acceptQuote(shareId: Long): Result<Unit> = runCatching {
        val share = requireShareForTraveller(shareId)
        val booking = requireNotNull(bookingDao.getBooking(shareId)) { "There's no quote yet." }
        require(
            BookingRules.canRespondToQuote(
                ShareStatus.fromStorage(share.status),
                booking.quoteValidUntil?.let { LocalDate.parse(it) },
                LocalDate.now()
            )
        ) { "This quote can't be accepted (it may have expired — ask the agent for a new one)." }
        val now = System.currentTimeMillis()
        database.withTransaction {
            bookingDao.upsertBooking(booking.copy(quoteRespondedAt = now, updatedAt = now))
            agentDao.setShareStatusKeepingReviewTime(shareId, ShareStatus.QUOTE_ACCEPTED.name)
        }
    }

    override suspend fun declineQuote(shareId: Long, reason: String): Result<Unit> = runCatching {
        val share = requireShareForTraveller(shareId)
        require(ShareStatus.fromStorage(share.status) == ShareStatus.QUOTE_SENT) { "There's no open quote to decline." }
        cancelShare(share, BookingActor.TRAVELLER, "Quote declined" + reason.trim().let { if (it.isEmpty()) "" else ": $it" })
    }

    override suspend fun rateAgent(shareId: Long, stars: Int, comment: String): Result<Unit> = runCatching {
        val share = requireShareForTraveller(shareId)
        require(BookingRules.canRate(ShareStatus.fromStorage(share.status))) { "You can rate the agent after the trip." }
        val problems = BookingRules.validateRating(stars)
        require(problems.isEmpty()) { problems.joinToString("\n") }
        val trip = tripDao.getTripById(share.tripId)
        val existing = bookingDao.getReviewForShare(shareId)
        bookingDao.upsertReview(
            AgentReviewEntity(
                id = existing?.id ?: 0,
                shareId = shareId,
                agentEmail = share.agentEmail,
                travellerEmail = share.travellerEmail,
                tripDestination = trip?.destination.orEmpty(),
                stars = stars,
                comment = comment.trim().take(500),
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
    }

    // ── Either side ──────────────────────────────────────────────────────────

    override suspend fun cancel(shareId: Long, reason: String): Result<Unit> = runCatching {
        val share = requireShare(shareId)
        val me = currentEmail()
        val actor = when (me) {
            share.travellerEmail -> BookingActor.TRAVELLER
            share.agentEmail -> BookingActor.AGENT
            else -> error("Only the traveller or the agent can cancel this.")
        }
        require(BookingRules.canCancel(ShareStatus.fromStorage(share.status))) {
            "This can no longer be cancelled here (it's already booked or finished)."
        }
        require(reason.isNotBlank()) { "Please give a short reason." }
        cancelShare(share, actor, reason.trim())
    }

    override suspend fun completeFinishedTrips(): Int {
        val today = LocalDate.now()
        val finished = bookingDao.getBookedTrips().filter { row ->
            runCatching { BookingRules.isTripFinished(LocalDate.parse(row.endDate), today) }.getOrDefault(false)
        }
        finished.forEach { agentDao.setShareStatusKeepingReviewTime(it.shareId, ShareStatus.COMPLETED.name) }
        return finished.size
    }

    // ── Ratings ──────────────────────────────────────────────────────────────

    override fun observeReviewForShare(shareId: Long): Flow<AgentReview?> =
        bookingDao.observeReviewForShare(shareId).map { it?.toDomain() }

    override fun observeReviewsForAgent(agentEmail: String): Flow<List<AgentReview>> =
        bookingDao.observeReviewsForAgent(agentEmail.trim().lowercase()).map { list -> list.map { it.toDomain() } }

    override fun observeReviewsForTrip(tripId: Long): Flow<Map<Long, AgentReview>> =
        bookingDao.observeReviewsForTrip(tripId).map { list -> list.associate { it.shareId to it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyReviews(): Flow<List<AgentReview>> =
        authRepository.currentUserEmail
            .map { it?.trim()?.lowercase().orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(emptyList()) else observeReviewsForAgent(email)
            }

    override fun observeRatings(): Flow<Map<String, AgentRating>> =
        bookingDao.observeRatings().map { rows -> rows.associate { it.agentEmail to AgentRating(it.average, it.count) } }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Cancels the chosen booking and lets the traveller pick from the other agents again. */
    private suspend fun cancelShare(share: TripShareEntity, actor: BookingActor, reason: String) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val current = bookingDao.getBooking(share.id) ?: BookingEntity(shareId = share.id)
            bookingDao.upsertBooking(
                current.copy(cancelledBy = actor.name, cancelReason = reason, cancelledAt = now, updatedAt = now)
            )
            agentDao.setShareStatusKeepingReviewTime(share.id, ShareStatus.CANCELLED.name)
            bookingDao.reopenClosedShares(share.tripId, share.id)
        }
    }

    private suspend fun currentEmail(): String {
        val email = authRepository.currentUserEmail.first()?.trim()?.lowercase().orEmpty()
        require(email.isNotEmpty()) { "Please log in first." }
        return email
    }

    private suspend fun requireShare(shareId: Long): TripShareEntity =
        requireNotNull(agentDao.getShare(shareId)) { "This request no longer exists." }

    private suspend fun requireShareForAgent(shareId: Long): TripShareEntity {
        val share = requireShare(shareId)
        require(userRoleRepository.currentUserRole.first() == UserRole.AGENT) { "Only agent accounts can do this." }
        require(share.agentEmail == currentEmail()) { "This trip was sent to a different agent." }
        return share
    }

    private suspend fun requireShareForTraveller(shareId: Long): TripShareEntity {
        val share = requireShare(shareId)
        require(share.travellerEmail == currentEmail()) { "Only the traveller can do this." }
        return share
    }

    private fun BookingEntity.toDomain() = Booking(
        shareId = shareId,
        quoteTotal = quoteTotal,
        quoteIncludes = quoteIncludes,
        quoteExcludes = quoteExcludes,
        paymentNote = paymentNote,
        quoteValidUntil = quoteValidUntil?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        quoteSentAt = quoteSentAt,
        quoteRespondedAt = quoteRespondedAt,
        bookingReference = bookingReference,
        bookingNotes = bookingNotes,
        bookedAt = bookedAt,
        reconfirmedAt = reconfirmedAt,
        cancelledBy = cancelledBy?.let { v -> BookingActor.entries.firstOrNull { it.name == v } },
        cancelReason = cancelReason,
        cancelledAt = cancelledAt
    )

    private fun AgentReviewEntity.toDomain() = AgentReview(
        shareId = shareId,
        agentEmail = agentEmail,
        tripDestination = tripDestination,
        stars = stars,
        comment = comment,
        createdAt = createdAt
    )
}
