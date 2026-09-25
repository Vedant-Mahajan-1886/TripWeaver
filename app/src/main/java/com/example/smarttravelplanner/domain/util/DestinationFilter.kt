package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination

/**
 * Pure search + category filtering for the Explore screen.
 * Kept free of Android/Compose so it can be unit tested directly.
 */
object DestinationFilter {

    /** Category id that disables category filtering. */
    const val ALL = "all"

    fun apply(
        destinations: List<ExploreDestination>,
        query: String,
        category: String
    ): List<ExploreDestination> {
        val normalizedQuery = query.trim().lowercase()
        val normalizedCategory = category.trim().lowercase()

        return destinations.filter { dest ->
            matchesCategory(dest, normalizedCategory) && matchesQuery(dest, normalizedQuery)
        }
    }

    private fun matchesCategory(dest: ExploreDestination, category: String): Boolean =
        category.isEmpty() ||
            category == ALL ||
            dest.tagCategory.equals(category, ignoreCase = true) ||
            dest.tag.lowercase().contains(category)

    private fun matchesQuery(dest: ExploreDestination, query: String): Boolean =
        query.isEmpty() ||
            dest.name.lowercase().contains(query) ||
            dest.stateCountry.lowercase().contains(query) ||
            dest.tag.lowercase().contains(query) ||
            dest.tagCategory.lowercase().contains(query) ||
            dest.idealDuration.lowercase().contains(query) ||
            dest.bestSeason.lowercase().contains(query)
}
