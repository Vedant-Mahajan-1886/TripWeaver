package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.local.dao.GroupDao
import com.example.smarttravelplanner.data.local.dao.GroupTripWithCount
import com.example.smarttravelplanner.data.local.dao.PackageDao
import com.example.smarttravelplanner.data.local.dao.UserDao
import com.example.smarttravelplanner.data.local.db.TravelDatabase
import com.example.smarttravelplanner.data.local.entity.GroupMemberEntity
import com.example.smarttravelplanner.data.local.entity.GroupTripEntity
import com.example.smarttravelplanner.domain.model.UserRole
import com.example.smarttravelplanner.domain.model.agent.GroupMember
import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import com.example.smarttravelplanner.domain.model.agent.GroupTripListing
import com.example.smarttravelplanner.domain.util.GroupRules
import com.example.smarttravelplanner.domain.util.JoinCheck
import androidx.room.withTransaction
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

/** Room-backed [GroupRepository] (agents and travellers on the same phone). */
class LocalGroupRepository @Inject constructor(
    private val database: TravelDatabase,
    private val groupDao: GroupDao,
    private val packageDao: PackageDao,
    private val userDao: UserDao,
    private val agentRepository: AgentRepository,
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository
) : GroupRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyGroups(): Flow<List<GroupTrip>> =
        authRepository.currentUserEmail
            .map { it.normalized() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(emptyList())
                else groupDao.observeGroupsForAgent(email).map { rows -> rows.map { it.toDomain() } }
            }

    override fun observeGroup(groupId: Long): Flow<GroupTrip?> =
        groupDao.observeGroup(groupId).map { it?.toDomain() }

    override fun observeMembers(groupId: Long): Flow<List<GroupMember>> =
        groupDao.observeMembers(groupId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun saveGroup(group: GroupTrip): Result<Long> = runCatching {
        val me = requireAgent()
        val existing = if (group.id != 0L) requireOwnGroup(group.id, me) else null
        require(existing?.isCancelled != true) { "A cancelled group can't be changed." }

        // Check against the real number of people who joined, not what the screen remembers.
        val taken = if (existing != null) groupDao.countMembers(existing.id) else 0
        val problems = GroupRules.validate(group.copy(seatsTaken = taken), LocalDate.now())
        require(problems.isEmpty()) { problems.joinToString("\n") }

        group.packageId?.let { packageId ->
            val pkg = requireNotNull(packageDao.getPackageRow(packageId)) { "That package no longer exists." }
            require(pkg.agentEmail == me) { "You can only link your own packages." }
        }

        val now = System.currentTimeMillis()
        if (existing == null) {
            groupDao.insertGroup(group.toEntity(agentEmail = me, createdAt = now, updatedAt = now))
        } else {
            groupDao.updateGroup(
                group.toEntity(agentEmail = me, createdAt = existing.createdAt, updatedAt = now)
                    .copy(isCancelled = existing.isCancelled)
            )
            existing.id
        }
    }

    override suspend fun setPublished(groupId: Long, published: Boolean): Result<Unit> = runCatching {
        val me = requireAgent()
        val row = requireOwnGroup(groupId, me)
        require(!row.isCancelled) { "A cancelled group can't be published." }
        if (published) {
            val taken = groupDao.countMembers(groupId)
            val problems = GroupRules.validate(row.toDomain(taken), LocalDate.now())
            require(problems.isEmpty()) { problems.joinToString("\n") }
        }
        groupDao.updateGroup(row.copy(isPublished = published, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun cancelGroup(groupId: Long): Result<Unit> = runCatching {
        val me = requireAgent()
        val row = requireOwnGroup(groupId, me)
        groupDao.updateGroup(row.copy(isCancelled = true, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteGroup(groupId: Long): Result<Unit> = runCatching {
        val me = requireAgent()
        requireOwnGroup(groupId, me)
        require(groupDao.countMembers(groupId) == 0) {
            "Travellers have joined this group. Cancel it instead so they know."
        }
        groupDao.deleteGroup(groupId)
    }

    // -- Traveller side ---------------------------------------------------------

    override fun observeOpenGroups(): Flow<List<GroupTripListing>> =
        combine(
            groupDao.observeOpenGroups(LocalDate.now().toString()),
            agentRepository.observeAgents(),
            bookingRepository.observeRatings(),
            observeMyJoinedIds()
        ) { rows, agents, ratings, joinedIds ->
            val byEmail = agents.associateBy { it.email }
            rows.map { row ->
                val group = row.toDomain()
                GroupTripListing(
                    group = group,
                    agent = byEmail[group.agentEmail],
                    rating = ratings[group.agentEmail],
                    joinedByMe = group.id in joinedIds
                )
            }
        }

    override fun observeListing(groupId: Long): Flow<GroupTripListing?> =
        combine(
            groupDao.observeGroup(groupId),
            agentRepository.observeAgents(),
            bookingRepository.observeRatings(),
            observeMyJoinedIds()
        ) { row, agents, ratings, joinedIds ->
            val group = row?.toDomain() ?: return@combine null
            val joined = group.id in joinedIds
            // Drafts are private to the agent; only people already in the group still see it.
            if (!group.isPublished && !joined) return@combine null
            GroupTripListing(
                group = group,
                agent = agents.firstOrNull { it.email == group.agentEmail },
                rating = ratings[group.agentEmail],
                joinedByMe = joined
            )
        }

    override suspend fun joinGroup(groupId: Long): Result<Unit> = runCatching {
        val (email, name) = requireTraveller()
        // One transaction: count the seats and take one without anybody slipping in between.
        database.withTransaction {
            val row = requireNotNull(groupDao.getGroup(groupId)) { "This group no longer exists." }
            val alreadyJoined = groupDao.observeJoinedGroupIds(email).first().contains(groupId)
            val group = row.toDomain(groupDao.countMembers(groupId))
            val check = GroupRules.canJoin(group, alreadyJoined, LocalDate.now())
            require(check == JoinCheck.OK) { check.message }
            groupDao.insertMember(GroupMemberEntity(groupId = groupId, travellerEmail = email, travellerName = name))
        }
        Unit
    }

    override suspend fun leaveGroup(groupId: Long): Result<Unit> = runCatching {
        val (email, _) = requireTraveller()
        database.withTransaction {
            val row = requireNotNull(groupDao.getGroup(groupId)) { "This group no longer exists." }
            val group = row.toDomain(groupDao.countMembers(groupId))
            require(GroupRules.canLeave(group, LocalDate.now())) { "You can only leave until the day before the trip starts." }
            val removed = groupDao.deleteMember(groupId, email)
            require(removed > 0) { "You're not in this group." }
        }
    }

    // -- Helpers --------------------------------------------------------------

    /** Ids of the groups the logged-in traveller joined. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMyJoinedIds(): Flow<List<Long>> =
        authRepository.currentUserEmail
            .map { it.normalized() }
            .distinctUntilChanged()
            .flatMapLatest { email ->
                if (email.isEmpty()) flowOf(emptyList()) else groupDao.observeJoinedGroupIds(email)
            }

    /** Email and display name of the logged-in traveller. */
    private suspend fun requireTraveller(): Pair<String, String> {
        val email = authRepository.currentUserEmail.first().normalized()
        require(email.isNotEmpty()) { "Please log in first." }
        require(userRoleRepository.currentUserRole.first() == UserRole.TRAVELLER) { "Only traveller accounts can join group trips." }
        val name = userDao.getUserByEmail(email)?.fullName?.trim().orEmpty().ifBlank { email }
        return email to name
    }

    private suspend fun requireAgent(): String {
        val email = authRepository.currentUserEmail.first().normalized()
        require(email.isNotEmpty()) { "Please log in first." }
        require(userRoleRepository.currentUserRole.first() == UserRole.AGENT) { "Only agent accounts can manage group trips." }
        return email
    }

    private suspend fun requireOwnGroup(groupId: Long, agentEmail: String): GroupTripEntity {
        val row = requireNotNull(groupDao.getGroup(groupId)) { "This group no longer exists." }
        require(row.agentEmail == agentEmail) { "You can only change your own groups." }
        return row
    }

    private fun String?.normalized(): String = this?.trim()?.lowercase().orEmpty()

    private fun GroupTrip.toEntity(agentEmail: String, createdAt: Long, updatedAt: Long) = GroupTripEntity(
        id = id,
        agentEmail = agentEmail,
        title = title.trim(),
        destination = destination.trim(),
        startDate = startDate.toString(),
        endDate = endDate.toString(),
        pricePerPerson = pricePerPerson,
        totalSeats = totalSeats,
        meetingPoint = meetingPoint.trim(),
        description = description.trim(),
        packageId = packageId,
        isPublished = isPublished,
        isCancelled = isCancelled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun GroupTripEntity.toDomain(seatsTaken: Int) = GroupTrip(
        id = id,
        agentEmail = agentEmail,
        title = title,
        destination = destination,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        pricePerPerson = pricePerPerson,
        totalSeats = totalSeats,
        meetingPoint = meetingPoint,
        description = description,
        packageId = packageId,
        isPublished = isPublished,
        isCancelled = isCancelled,
        seatsTaken = seatsTaken
    )

    private fun GroupTripWithCount.toDomain() = GroupTrip(
        id = id,
        agentEmail = agentEmail,
        title = title,
        destination = destination,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        pricePerPerson = pricePerPerson,
        totalSeats = totalSeats,
        meetingPoint = meetingPoint,
        description = description,
        packageId = packageId,
        isPublished = isPublished,
        isCancelled = isCancelled,
        seatsTaken = seatsTaken
    )

    private fun GroupMemberEntity.toDomain() = GroupMember(
        travellerEmail = travellerEmail,
        travellerName = travellerName,
        joinedAt = joinedAt
    )
}
