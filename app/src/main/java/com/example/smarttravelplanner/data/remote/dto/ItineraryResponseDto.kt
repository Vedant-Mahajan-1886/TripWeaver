package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Strict JSON schema that the AI MUST return.
 * All place data (names, coordinates, placeId) will be real
 * once Track B (Google Places context injection) is wired in.
 * Until then, the AI fills them with its best knowledge.
 */
data class GeneratedItineraryDto(
    @SerializedName("days") val days: List<DayDto> = emptyList()
)

data class DayDto(
    @SerializedName("day_number")  val dayNumber: Int = 1,
    @SerializedName("date")        val date: String = "",         // "2024-10-15"
    @SerializedName("theme")       val theme: String = "",        // e.g. "Heritage & Culture"
    @SerializedName("activities")  val activities: List<ActivityDto> = emptyList()
)

data class ActivityDto(
    @SerializedName("title")               val title: String = "",
    @SerializedName("place_name")          val placeName: String = "",
    @SerializedName("place_id")            val placeId: String = "",   // Google Place ID (Track B)
    @SerializedName("address")             val address: String = "",
    @SerializedName("lat")                 val lat: Double = 0.0,
    @SerializedName("lng")                 val lng: Double = 0.0,
    @SerializedName("start_time")          val startTime: String = "", // "09:00"
    @SerializedName("end_time")            val endTime: String = "",   // "11:00"
    @SerializedName("category")            val category: String = "ATTRACTION",
    @SerializedName("description")         val description: String = "",
    @SerializedName("estimated_cost_inr")  val estimatedCostInr: Double = 0.0,
    @SerializedName("weather_note")        val weatherNote: String? = null
)
