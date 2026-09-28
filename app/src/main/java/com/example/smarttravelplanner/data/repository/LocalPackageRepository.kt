package com.example.smarttravelplanner.data.repository

import androidx.room.withTransaction
import com.example.smarttravelplanner.data.local.dao.AgentDao
import com.example.smarttravelplanner.data.local.dao.PackageDao
import com.example.smarttravelplanner.data.local.dao.PackageWithDays
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.local.entity.AgentPackageEntity
import com.example.smarttravelplanner.data.local.entity.PackageDayEntity
import com.example.smarttravelplanner.data.local.entity.TripShareEntity
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.Trip
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.domain.model.agent.AgentPackage
import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.PackageDay
import com.example.smarttravelplanner.domain.model.agent.PackageListing
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.util.PackageRules
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Room-backed [PackageRepository] (agents and travellers on the same phone). */
class LocalPackageRepository @Inject constructor(
    private val database: TravelDatabase,
    private val packageDao: PackageDao,
    private val agentDao: AgentDao,
    private val agentRepository: AgentRepository,
    private val bookingRepository: BookingRepository,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository,
    private val gson: Gson
) : PackageRepository {

    private val activityListType = object : TypeToken<List<Activity>>() {}.type

    // ── Agent ────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyPackages(): Flow<List<AgentPackage>> =
        authRepository.currentUserEmail
            .map { it.normalized() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(emptyList())
                else packageDao.observePackagesForAgent(email).map { list -> list.map { it.toDomain() } }
            }

    override suspend fun savePackage(pkg: AgentPackage): Result<Long> = runCatching {
        val me = requireAgent()
        if (pkg.id != 0L) requireOwnPackage(pkg.id, me)
        val problems = PackageRules.validate(pkg, forPublishing = pkg.isPublished)
        require(problems.isEmpty()) { problems.joinToString("\n") }

        val now = System.currentTimeMillis()
        database.withTransaction {
            val existing = if (pkg.id != 0L) packageDao.getPackageRow(pkg.id) else null
            val id = packageDao.upsertPackage(
                AgentPackageEntity(
                    id = pkg.id,
                    agentEmail = me,
                    title = pkg.title.trim(),
                    destination = pkg.destination.trim(),
                    pricePerPerson = pkg.pricePerPerson,
                    description = pkg.description.trim(),
                    highlights = pkg.highlights.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
                    includes = pkg.includes.trim(),
                    excludes = pkg.excludes.trim(),
                    bestSeason = pkg.bestSeason.trim(),
                    isPublished = pkg.isPublished,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            ).let { if (pkg.id != 0L) pkg.id else it }
            packageDao.deleteDays(id)
            packageDao.insertDays(
                pkg.days.sortedBy { it.dayNumber }.map { day ->
                    PackageDayEntity(
                        packageId = id,
                        dayNumber = day.dayNumber,
                        title = day.title.trim(),
                        activitiesJson = gson.toJson(day.activities)
                    )
                }
            )
            id
        }
    }

    override suspend fun setPublished(packageId: Long, published: Boolean): Result<Unit> = runCatching {
        val me = requireAgent()
        requireOwnPackage(packageId, me)
        if (published) {
            val pkg = requireNotNull(packageDao.getPackage(packageId)).toDomain()
            val problems = PackageRules.validate(pkg, forPublishing = true)
            require(problems.isEmpty()) { problems.joinToString("\n") }
        }
        packageDao.setPublished(packageId, published, System.currentTimeMillis())
    }

    override suspend fun deletePackage(packageId: Long): Result<Unit> = runCatching {
        val me = requireAgent()
        requireOwnPackage(packageId, me)
        // Trips already made from it are the travellers' own copies, so they stay.
        packageDao.deletePackage(packageId)
    }

    // ── Everyone ─────────────────────────────────────────────────────────────

    override fun observePackage(packageId: Long): Flow<AgentPackage?> =
        packageDao.observePackage(packageId).map { it?.toDomain() }

    // ── Traveller ────────────────────────────────────────────────────────────

    override fun observePublishedPackages(query: String): Flow<List<PackageListing>> =
        combine(
            packageDao.observePublishedPackages(),
            agentRepository.observeAgents(),
            bookingRepository.observeRatings()
        ) { packages, agents, ratings ->
            val byEmail = agents.associateBy { it.email }
            packages.map { it.toDomain() }
                .filter { PackageRules.matches(it, query) }
                .map { pkg ->
                    PackageListing(
                        pkg = pkg,
                        agent = byEmail[pkg.agentEmail] ?: AgentProfile(email = pkg.agentEmail, fullName = pkg.agentEmail),
                        rating = ratings[pkg.agentEmail]
                    )
                }
        }

    override suspend fun choosePackage(
        packageId: Long,
        startDate: LocalDate,
        adults: Int,
        kids: Int
    ): Result<Long> = runCatching {
        val me = authRepository.currentUserEmail.first().normalized()
        require(me.isNotEmpty()) { "Please log in first." }
        require(userRoleRepository.currentUserRole.first() == UserRole.TRAVELLER) { "Only travellers can choose packages." }
        require(adults >= 1 && kids >= 0) { "At least one adult is needed." }

        val pkg = requireNotNull(packageDao.getPackage(packageId)) { "This package no longer exists." }.toDomain()
        require(pkg.isPublished) { "This package isn't available any more." }
        require(pkg.days.isNotEmpty()) { "This package has no itinerary." }

        val agencyName = agentRepository.observeAgents().first()
            .firstOrNull { it.email == pkg.agentEmail }
            ?.let { it.agencyName.ifBlank { it.fullName } } ?: "the agent"
        val groupPrice = PackageRules.groupPrice(pkg.pricePerPerson, adults, kids)
        val now = System.currentTimeMillis()

        database.withTransaction {
            val tripId = tripRepository.saveTrip(
                Trip(
                    title = pkg.title,
                    destination = pkg.destination,
                    startDate = startDate,
                    endDate = PackageRules.endDate(pkg, startDate),
                    budget = groupPrice,
                    adults = adults,
                    kids = kids,
                    interests = pkg.highlights.take(5),
                    notes = "From $agencyName's package \u201C${pkg.title}\u201D."
                )
            )
            itineraryRepository.saveItineraries(PackageRules.toItinerary(pkg, tripId, startDate))
            // Linked to the agent at "Chosen", so the agent's next step is sending a quote
            agentDao.insertShare(
                TripShareEntity(
                    tripId = tripId,
                    travellerEmail = me,
                    agentEmail = pkg.agentEmail,
                    message = "I'd like to book your package \u201C${pkg.title}\u201D.",
                    status = ShareStatus.CONFIRMED.name,
                    decision = ReviewDecision.APPROVED.name,
                    overallComment = "Ready-made package",
                    priceEstimate = groupPrice,
                    sentAt = now,
                    updatedAt = now,
                    packageId = pkg.id
                )
            )
            tripId
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun requireAgent(): String {
        val email = authRepository.currentUserEmail.first().normalized()
        require(email.isNotEmpty()) { "Please log in first." }
        require(userRoleRepository.currentUserRole.first() == UserRole.AGENT) { "Only agent accounts can manage packages." }
        return email
    }

    private suspend fun requireOwnPackage(packageId: Long, agentEmail: String) {
        val row = requireNotNull(packageDao.getPackageRow(packageId)) { "This package no longer exists." }
        require(row.agentEmail == agentEmail) { "You can only change your own packages." }
    }

    private fun String?.normalized(): String = this?.trim()?.lowercase().orEmpty()

    private fun PackageWithDays.toDomain() = AgentPackage(
        id = pkg.id,
        agentEmail = pkg.agentEmail,
        title = pkg.title,
        destination = pkg.destination,
        pricePerPerson = pkg.pricePerPerson,
        description = pkg.description,
        highlights = pkg.highlights,
        includes = pkg.includes,
        excludes = pkg.excludes,
        bestSeason = pkg.bestSeason,
        isPublished = pkg.isPublished,
        updatedAt = pkg.updatedAt,
        days = days.sortedBy { it.dayNumber }.map { d ->
            PackageDay(
                dayNumber = d.dayNumber,
                title = d.title,
                activities = runCatching { gson.fromJson<List<Activity>>(d.activitiesJson, activityListType) }
                    .getOrNull().orEmpty()
            )
        }
    )
}
