package com.example.smarttravelplanner.domain.model

data class Activity(
    val id: Long = 0,
    val itineraryId: Long,
    val title: String,
    val description: String = "",
    val location: String = "",
    val latLng: Pair<Double, Double>? = null,
    val startTime: String = "",
    val endTime: String = "",
    val category: ActivityCategory = ActivityCategory.ATTRACTION,
    val estimatedCost: Double = 0.0,
    val placeId: String = ""         // Google Places ID for map integration
)
