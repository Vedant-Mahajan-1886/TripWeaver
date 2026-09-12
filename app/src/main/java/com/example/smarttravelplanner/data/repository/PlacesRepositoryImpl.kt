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
        "alappuzha" to Pair(9.4981, 76.3388),
        "srinagar" to Pair(34.0837, 74.7973),
        "amritsar" to Pair(31.6340, 74.8723),
        "bangalore" to Pair(12.9716, 77.5946),
        "bengaluru" to Pair(12.9716, 77.5946),
        "hyderabad" to Pair(17.3850, 78.4867),
        "chennai" to Pair(13.0827, 80.2707),
        "kolkata" to Pair(22.5726, 88.3639),
        "shimla" to Pair(31.1048, 77.1734),
        "darjeeling" to Pair(27.0410, 88.2663),
        "ooty" to Pair(11.4102, 76.6950),
        "munnar" to Pair(10.0889, 77.0595),
        "kochi" to Pair(9.9312, 76.2673),
        "pondicherry" to Pair(11.9416, 79.8083),
        "puducherry" to Pair(11.9416, 79.8083),
        "leh" to Pair(34.1526, 77.5771),
        "ladakh" to Pair(34.1526, 77.5771),
        "hampi" to Pair(15.3350, 76.4600),
        "jodhpur" to Pair(26.2389, 73.0243),
        "jaisalmer" to Pair(26.9157, 70.9083),
        "mysore" to Pair(12.2958, 76.6394),
        "mysuru" to Pair(12.2958, 76.6394),
        "kashmir" to Pair(34.0837, 74.7973),
        "andaman" to Pair(11.6234, 92.7265),
        "port blair" to Pair(11.6234, 92.7265),
        "gangtok" to Pair(27.3389, 88.6065),
        "shillong" to Pair(25.5788, 91.8933)
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
