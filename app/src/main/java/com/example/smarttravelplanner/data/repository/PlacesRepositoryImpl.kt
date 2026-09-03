package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.dto.PlaceResultDto
import javax.inject.Inject

class PlacesRepositoryImpl @Inject constructor() : PlacesRepository {

    // Default coordinates lookup for popular destinations until Geocoding API key is provided
    private val knownCoordinates = mapOf(
        "delhi" to Pair(28.6139, 77.2090),
        "new delhi" to Pair(28.6139, 77.2090),
        "jaipur" to Pair(26.9124, 75.7873),
        "udaipur" to Pair(24.5854, 73.7125),
        "agra" to Pair(27.1767, 78.0081),
        "mumbai" to Pair(19.0760, 72.8777),
        "goa" to Pair(15.2993, 74.1240),
        "varanasi" to Pair(25.3176, 82.9739),
        "manali" to Pair(32.2432, 77.1892),
        "rishikesh" to Pair(30.0869, 78.2676),
        "alleppey" to Pair(9.4981, 76.3388),
        "srinagar" to Pair(34.0837, 74.7973),
        "amritsar" to Pair(31.6340, 74.8723)
    )

    override suspend fun searchPlacesNearby(
        destination: String,
        latitude: Double?,
        longitude: Double?
    ): Result<List<PlaceResultDto>> = runCatching {
        // In Track B: PlacesClient.searchNearby() / TextSearch will be called here.
        // For now, return emptyList or mock spots so Ollama uses its internal knowledge
        emptyList()
    }

    override suspend fun geocodeDestination(destination: String): Result<Pair<Double, Double>> = runCatching {
        val normalized = destination.trim().lowercase()
        val found = knownCoordinates.entries.firstOrNull { normalized.contains(it.key) }?.value
        found ?: Pair(28.6139, 77.2090) // Default to New Delhi if unknown
    }
}
