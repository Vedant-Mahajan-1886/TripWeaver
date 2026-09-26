package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.GeoPoint
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Straight-line ("as the crow flies") distances between places.
 * Pure Kotlin so it can be unit tested without Android.
 */
object GeoDistance {

    private const val EARTH_RADIUS_KM = 6371.0

    /** Great-circle distance between [from] and [to] in kilometres (haversine formula). */
    fun haversineKm(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a))
    }

    /**
     * Distance in whole kilometres from [origin] to each destination, keyed by name.
     * Destinations without coordinates are left out.
     */
    fun distancesFrom(
        origin: GeoPoint,
        destinations: List<ExploreDestination>
    ): Map<String, Int> =
        destinations
            .mapNotNull { dest -> dest.location?.let { dest.name to haversineKm(origin, it).roundToInt() } }
            .toMap()

    /**
     * Sorts nearest first. Destinations with no known distance keep their
     * relative order and go to the end.
     */
    fun sortByDistance(
        destinations: List<ExploreDestination>,
        distancesKm: Map<String, Int>
    ): List<ExploreDestination> =
        destinations.sortedBy { distancesKm[it.name] ?: Int.MAX_VALUE }

    /** "1,050 km" style label; values under 1 km show as "< 1 km". */
    fun formatKm(km: Int): String =
        if (km < 1) "< 1 km" else String.format(Locale.US, "%,d km", km)
}
