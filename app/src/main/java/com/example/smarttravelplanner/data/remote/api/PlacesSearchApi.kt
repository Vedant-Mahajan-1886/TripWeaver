package com.example.smarttravelplanner.data.remote.api

import com.example.smarttravelplanner.data.remote.dto.PlaceResultDto

/**
 * Interface abstraction for Google Places search.
 * When the Google API key is configured (Track B), this wraps the PlacesClient.
 * Provides fallback mock places for development when the key is absent.
 */
interface PlacesSearchApi {
    suspend fun searchPlaces(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): List<PlaceResultDto>
}
