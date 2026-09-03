package com.example.smarttravelplanner.domain.model

data class Activity(
    val id: Long = 0,
    val itineraryId: Long,
    val title: String,
    val description: String = "",
    val location: String = "",
    val lat: Double? = null,          // From Google Places / AI JSON output
    val lng: Double? = null,          // From Google Places / AI JSON output
    val startTime: String = "",
    val endTime: String = "",
    val category: ActivityCategory = ActivityCategory.ATTRACTION,
    val estimatedCost: Double = 0.0,
    val placeId: String = "",         // Google Places ID for map integration
    val weatherNote: String? = null   // e.g. "Light rain expected — carry umbrella"
)
