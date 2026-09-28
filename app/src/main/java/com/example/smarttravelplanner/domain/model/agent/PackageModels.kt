package com.example.smarttravelplanner.domain.model.agent

import com.example.smarttravelplanner.domain.model.Activity

/** One day of a ready-made package. */
data class PackageDay(
    val dayNumber: Int,
    val title: String = "",
    val activities: List<Activity> = emptyList()
)

/** A ready-made trip offered by an agent. */
data class AgentPackage(
    val id: Long = 0,
    val agentEmail: String = "",
    val title: String,
    val destination: String,
    val pricePerPerson: Double,
    val description: String = "",
    val highlights: List<String> = emptyList(),
    val includes: String = "",
    val excludes: String = "",
    val bestSeason: String = "",
    val isPublished: Boolean = false,
    val days: List<PackageDay> = emptyList(),
    val updatedAt: Long = 0
) {
    val dayCount: Int get() = days.size
    val activityCount: Int get() = days.sumOf { it.activities.size }
}

/** A published package as a traveller sees it: with the agent's name and rating. */
data class PackageListing(
    val pkg: AgentPackage,
    val agent: AgentProfile,
    val rating: AgentRating?
)
