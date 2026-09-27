package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One change the traveller made to a trip by hand (e.g. "Moved “Amber Fort” to Day 2").
 * Lets agents see what changed since their review. Deleted with the trip.
 */
@Entity(
    tableName = "trip_edits",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class TripEditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val summary: String,
    val editedAt: Long = System.currentTimeMillis()
)
