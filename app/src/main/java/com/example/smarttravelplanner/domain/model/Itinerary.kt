package com.example.smarttravelplanner.domain.model

data class Itinerary(
    val id: Long = 0,
    val tripId: Long,
    val dayNumber: Int,
    val date: String,
    val activities: List<Activity> = emptyList()
)
