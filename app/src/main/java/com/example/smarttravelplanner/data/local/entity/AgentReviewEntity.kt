package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A traveller's rating of an agent after a completed trip.
 * Deliberately NOT linked by foreign key to the trip, so an agent keeps their
 * reputation even if the traveller later deletes the trip.
 */
@Entity(
    tableName = "agent_reviews",
    indices = [
        Index(value = ["shareId"], unique = true),
        Index(value = ["agentEmail"])
    ]
)
data class AgentReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shareId: Long,
    val agentEmail: String,
    val travellerEmail: String,
    val tripDestination: String,
    /** 1..5 */
    val stars: Int,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
