package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination

/**
 * Curated list of destinations that suit people travelling alone: hostels, easy
 * meet-ups with other travellers and safe, walkable areas. Pure functions, unit tested.
 */
object SoloFriendlyDestinations {

    private val NAMES: Set<String> = setOf(
        "goa", "rishikesh", "manali", "gokarna", "pondicherry",
        "varanasi", "jaipur", "udaipur", "leh", "spiti valley"
    )

    fun isSoloFriendly(name: String): Boolean = name.trim().lowercase() in NAMES

    /** Solo-friendly destinations first; the order inside each part is kept. */
    fun soloFirst(destinations: List<ExploreDestination>): List<ExploreDestination> =
        destinations.sortedByDescending { isSoloFriendly(it.name) }
}
