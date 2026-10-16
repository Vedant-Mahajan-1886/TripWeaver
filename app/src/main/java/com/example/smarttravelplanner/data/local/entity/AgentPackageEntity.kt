package com.example.smarttravelplanner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A ready-made trip an agent offers to travellers ("package"). Only its agent can change it. */
@Entity(
    tableName = "agent_packages",
    indices = [
        Index(value = ["agentEmail"]),
        Index(value = ["destination"]),
        // v14: one local row per cloud package
        Index(value = ["cloudId"], unique = true)
    ]
)
data class AgentPackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Lower-case email of the agent who owns the package */
    val agentEmail: String,
    val title: String,
    val destination: String,
    val pricePerPerson: Double,
    val description: String = "",
    val highlights: List<String> = emptyList(),
    val includes: String = "",
    val excludes: String = "",
    val bestSeason: String = "",
    /** Only published packages are visible to travellers */
    val isPublished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** Firestore document id in "packages" (v14); null until first uploaded. */
    val cloudId: String? = null
)

/** One day of a package's itinerary. Deleted together with its package. */
@Entity(
    tableName = "package_days",
    foreignKeys = [
        ForeignKey(
            entity = AgentPackageEntity::class,
            parentColumns = ["id"],
            childColumns = ["packageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["packageId"])]
)
data class PackageDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageId: Long,
    val dayNumber: Int,
    /** Short title for the day, e.g. "Forts & bazaars" */
    val title: String = "",
    /** JSON list of activities (same format as itineraries) */
    val activitiesJson: String = "[]"
)
