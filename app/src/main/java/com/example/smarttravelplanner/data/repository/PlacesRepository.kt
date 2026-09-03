package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.dto.PlaceResultDto

interface PlacesRepository {
    /**
     * Searches for verified places nearby given coordinates or destination query.
     */
    suspend fun searchPlacesNearby(
        destination: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<List<PlaceResultDto>>

    /**
     * Resolves a destination name to approximate coordinates.
     */
    suspend fun geocodeDestination(destination: String): Result<Pair<Double, Double>>
}
