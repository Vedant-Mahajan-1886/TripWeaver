package com.example.smarttravelplanner.domain.model

/** How the Explore list is ordered. */
enum class ExploreSortOrder(val label: String) {
    /** Curated catalog order. */
    RECOMMENDED("Recommended"),

    /** Closest to the selected "From" city first. */
    NEAREST("Nearest")
}
