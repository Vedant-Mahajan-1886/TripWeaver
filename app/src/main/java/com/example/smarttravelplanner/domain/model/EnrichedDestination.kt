package com.example.smarttravelplanner.domain.model

/**
 * A destination card shown on the Explore screen.
 * AI suggests the destination; Unsplash provides the photo.
 */
data class EnrichedDestination(
    val name: String,
    val country: String,
    val tagline: String,           // AI-generated one-liner e.g. "Forts, deserts & royal cuisine"
    val photoUrl: String = "",     // Unsplash URL (filled after Track B)
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val bestMonths: String = "",   // e.g. "Oct – Mar"
    val averageBudgetInr: Double = 0.0,
    val tags: List<String> = emptyList() // e.g. ["Heritage", "Adventure"]
)
