package com.example.smarttravelplanner.domain.model

/** A latitude/longitude pair in decimal degrees. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/** A city the user can travel from, shown in Explore's "From" selector. */
data class DepartureCity(
    val name: String,
    val location: GeoPoint
)
