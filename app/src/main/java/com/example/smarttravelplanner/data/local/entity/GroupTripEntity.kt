package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A fixed-date trip an agent runs for several travellers who don't know each other
 * (mainly solo travellers). Travellers join a seat; see [GroupMemberEntity].
 */
@Entity(
    tableName = "group_trips",
    indices = [Index(value = ["agentEmail"]), Index(value = ["startDate"])]
)
data class GroupTripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Lower-case email of the agent who runs the group */
    val agentEmail: String,
    val title: String,
    val destination: String,
    /** ISO dates (yyyy-MM-dd) */
    val startDate: String,
    val endDate: String,
    val pricePerPerson: Double,
    /** How many travellers can join */
    val totalSeats: Int,
    val meetingPoint: String = "",
    val description: String = "",
    /** Optional: the agent's package this group follows (day-by-day plan) */
    val packageId: Long? = null,
    /** Only published groups are visible to travellers */
    val isPublished: Boolean = false,
    /** Cancelled by the agent; members can see it but nobody can join */
    val isCancelled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** A traveller who joined a group trip. One row per traveller per group. */
@Entity(
    tableName = "group_members",
    foreignKeys = [
        ForeignKey(
            entity = GroupTripEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId", "travellerEmail"], unique = true),
        Index(value = ["travellerEmail"])
    ]
)
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    /** Lower-case email of the traveller */
    val travellerEmail: String,
    val travellerName: String,
    val joinedAt: Long = System.currentTimeMillis()
)
