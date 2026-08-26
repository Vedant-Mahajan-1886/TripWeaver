package com.example.smarttravelplanner.domain.model

data class Destination(
    val placeId: String,
    val name: String,
    val country: String,
    val description: String = "",
    val photoUrl: String = "",
    val rating: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val tags: List<String> = emptyList()
)
