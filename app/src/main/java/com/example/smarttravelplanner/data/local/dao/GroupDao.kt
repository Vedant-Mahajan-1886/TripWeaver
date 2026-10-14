package com.example.smarttravelplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smarttravelplanner.data.local.entity.GroupMemberEntity
import com.example.smarttravelplanner.data.local.entity.GroupTripEntity
import kotlinx.coroutines.flow.Flow

/** A group trip together with how many seats are taken. */
data class GroupTripWithCount(
    val id: Long,
    val agentEmail: String,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val pricePerPerson: Double,
    val totalSeats: Int,
    val meetingPoint: String,
    val description: String,
    val packageId: Long?,
    val isPublished: Boolean,
    val isCancelled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val seatsTaken: Int
)

@Dao
interface GroupDao {

    // -- Groups (agent) -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(group: GroupTripEntity): Long

    @Update
    suspend fun updateGroup(group: GroupTripEntity)

    @Query("SELECT * FROM group_trips WHERE id = :groupId LIMIT 1")
    suspend fun getGroup(groupId: Long): GroupTripEntity?

    @Query("DELETE FROM group_trips WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query(
        """
        SELECT g.*, (SELECT COUNT(*) FROM group_members m WHERE m.groupId = g.id) AS seatsTaken
        FROM group_trips g WHERE g.agentEmail = :agentEmail ORDER BY g.startDate ASC
        """
    )
    fun observeGroupsForAgent(agentEmail: String): Flow<List<GroupTripWithCount>>

    // -- Groups (travellers) --------------------------------------------------

    /** Published, not cancelled, starting on or after [fromDate]. */
    @Query(
        """
        SELECT g.*, (SELECT COUNT(*) FROM group_members m WHERE m.groupId = g.id) AS seatsTaken
        FROM group_trips g
        WHERE g.isPublished = 1 AND g.isCancelled = 0 AND g.startDate >= :fromDate
        ORDER BY g.startDate ASC
        """
    )
    fun observeOpenGroups(fromDate: String): Flow<List<GroupTripWithCount>>

    @Query(
        """
        SELECT g.*, (SELECT COUNT(*) FROM group_members m WHERE m.groupId = g.id) AS seatsTaken
        FROM group_trips g WHERE g.id = :groupId LIMIT 1
        """
    )
    fun observeGroup(groupId: Long): Flow<GroupTripWithCount?>

    // -- Members --------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMember(member: GroupMemberEntity): Long

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND travellerEmail = :travellerEmail")
    suspend fun deleteMember(groupId: Long, travellerEmail: String): Int

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId")
    suspend fun countMembers(groupId: Long): Int

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    fun observeMembers(groupId: Long): Flow<List<GroupMemberEntity>>

    @Query("SELECT groupId FROM group_members WHERE travellerEmail = :travellerEmail")
    fun observeJoinedGroupIds(travellerEmail: String): Flow<List<Long>>
}
