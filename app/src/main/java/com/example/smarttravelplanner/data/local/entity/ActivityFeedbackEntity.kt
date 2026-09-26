package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An agent's verdict on one activity of a shared trip.
 * The activity is identified by day number + position in that day, and its
 * title is copied so the feedback still reads well if the trip changes later.
 */
@Entity(
    tableName = "activity_feedback",
    foreignKeys = [
        ForeignKey(
            entity = TripShareEntity::class,
            parentColumns = ["id"],
            childColumns = ["shareId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["shareId"])]
)
data class ActivityFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shareId: Long,
    val dayNumber: Int,
    val activityIndex: Int,
    val activityTitle: String,
    /** ActivityVerdict name: AVAILABLE, NOT_AVAILABLE, SUGGEST_CHANGE */
    val verdict: String,
    val comment: String = "",
    /** Replacement the agent proposes (used with SUGGEST_CHANGE) */
    val suggestion: String = "",
    /** SuggestionResponse name: PENDING, APPLIED, DECLINED */
    val response: String = "PENDING",
    val updatedAt: Long = System.currentTimeMillis()
)
