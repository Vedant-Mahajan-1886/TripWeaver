package com.example.smarttravelplanner.domain.model

import androidx.annotation.DrawableRes

/**
 * A curated destination shown on the Explore screen.
 *
 * [tagCategory] is one of the Explore category chip ids
 * ("religious", "cultural", "nature", "adventure").
 */
data class ExploreDestination(
    val name: String,
    /** One or two sentences shown in the destination details sheet. */
    val description: String = "",
    val stateCountry: String,
    val idealDuration: String,
    val bestSeason: String,
    val tag: String,
    val tagCategory: String,
    @DrawableRes val imageRes: Int,
    /** Approximate city/region centre, used for distance from the "From" city. */
    val location: GeoPoint? = null
)
