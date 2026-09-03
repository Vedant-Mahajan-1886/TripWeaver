package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Verified place details fetched from Google Places SDK or Places API.
 * Injected as verified grounding context into Ollama prompt.
 */
data class PlaceResultDto(
    @SerializedName("place_id") val placeId: String,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String = "",
    @SerializedName("lat") val lat: Double = 0.0,
    @SerializedName("lng") val lng: Double = 0.0,
    @SerializedName("rating") val rating: Float? = null,
    @SerializedName("types") val types: List<String> = emptyList(),
    @SerializedName("photo_url") val photoUrl: String? = null
)
