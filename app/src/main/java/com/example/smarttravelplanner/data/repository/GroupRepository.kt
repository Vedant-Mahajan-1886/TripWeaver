package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.agent.GroupMember
import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import kotlinx.coroutines.flow.Flow

/**
 * Group trips: fixed-date trips an agent runs for several travellers (mainly solo travellers).
 * This part covers the agent's side; the traveller's side (browse, join, leave) comes later.
 */
interface GroupRepository {

    /** The logged-in agent's groups (drafts, published, cancelled), soonest first. */
    fun observeMyGroups(): Flow<List<GroupTrip>>

    /** One group with its seat count, or null if it no longer exists. */
    fun observeGroup(groupId: Long): Flow<GroupTrip?>

    /** Travellers who joined a group, in the order they joined. */
    fun observeMembers(groupId: Long): Flow<List<GroupMember>>

    /** Creates ([GroupTrip.id] = 0) or updates a group. Returns its id. */
    suspend fun saveGroup(group: GroupTrip): Result<Long>

    /** Shows or hides a group for travellers. Publishing checks the group is valid. */
    suspend fun setPublished(groupId: Long, published: Boolean): Result<Unit>

    /** The agent calls the trip off. Members keep seeing it, nobody can join. */
    suspend fun cancelGroup(groupId: Long): Result<Unit>

    /** Removes a group nobody has joined. A group with members must be cancelled instead. */
    suspend fun deleteGroup(groupId: Long): Result<Unit>
}
