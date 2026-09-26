package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.smarttravelplanner.data.local.db.Converters

@Entity(
    tableName = "trips",
    indices = [Index(value = ["ownerEmail"])]
)
@TypeConverters(Converters::class)
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val startDate: String,       // ISO-8601 string
    val endDate: String,         // ISO-8601 string
    val budget: Double,
    val adults: Int = 1,
    val kids: Int = 0,
    val interests: List<String> = emptyList(),
    val notes: String = "",
    val coverImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * Email of the account that owns this trip (lower-case).
     * Empty for trips created before ownership existed; those are claimed by
     * the next user who logs in (see TripRepositoryImpl).
     */
    val ownerEmail: String = ""
)
