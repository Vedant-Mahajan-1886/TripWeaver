package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A trip sent by a traveller to one agent for review.
 * Deleting the trip deletes its shares (and their feedback).
 */
@Entity(
    tableName = "trip_shares",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["agentEmail"]),
        Index(value = ["tripId", "agentEmail"], unique = true)
    ]
)
data class TripShareEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val travellerEmail: String,
    val agentEmail: String,
    val message: String = "",
    /** ShareStatus name: SENT, VIEWED, RESPONDED, WITHDRAWN */
    val status: String = "SENT",
    /** ReviewDecision name (APPROVED / CHANGES_REQUESTED), null until the agent responds */
    val decision: String? = null,
    val overallComment: String = "",
    val priceEstimate: Double? = null,
    val sentAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Set when the traveller chose one of this agent's ready-made packages (v10) */
    val packageId: Long? = null
)
