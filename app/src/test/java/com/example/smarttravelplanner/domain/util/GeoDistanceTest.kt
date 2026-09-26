package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GeoDistanceTest {

    private val delhi = GeoPoint(28.6139, 77.2090)
    private val mumbai = GeoPoint(19.0760, 72.8777)
    private val agra = GeoPoint(27.1767, 78.0081)
    private val goa = GeoPoint(15.2993, 74.1240)

    private fun dest(name: String, location: GeoPoint?) = ExploreDestination(
        name = name,
        stateCountry = "",
        idealDuration = "",
        bestSeason = "",
        tag = "",
        tagCategory = "",
        imageRes = 0,
        location = location
    )

    @Test
    fun samePointIsZeroKm() {
        assertEquals(0.0, GeoDistance.haversineKm(delhi, delhi), 0.001)
    }

    @Test
    fun delhiToMumbaiIsAbout1150Km() {
        // Known straight-line distance is ~1,150 km
        assertEquals(1150.0, GeoDistance.haversineKm(delhi, mumbai), 20.0)
    }

    @Test
    fun distanceIsSymmetric() {
        assertEquals(
            GeoDistance.haversineKm(delhi, goa),
            GeoDistance.haversineKm(goa, delhi),
            0.001
        )
    }

    @Test
    fun distancesFromSkipsDestinationsWithoutLocation() {
        val result = GeoDistance.distancesFrom(
            delhi,
            listOf(dest("Agra", agra), dest("Unknown", null))
        )
        assertFalse(result.containsKey("Unknown"))
        assertEquals(setOf("Agra"), result.keys)
    }

    @Test
    fun sortByDistancePutsNearestFirstAndUnknownLast() {
        val list = listOf(dest("Goa", goa), dest("Unknown", null), dest("Agra", agra))
        val distances = GeoDistance.distancesFrom(delhi, list)

        val sorted = GeoDistance.sortByDistance(list, distances)

        assertEquals(listOf("Agra", "Goa", "Unknown"), sorted.map { it.name })
    }

    @Test
    fun formatKmUsesThousandsSeparator() {
        assertEquals("1,150 km", GeoDistance.formatKm(1150))
        assertEquals("180 km", GeoDistance.formatKm(180))
        assertEquals("< 1 km", GeoDistance.formatKm(0))
    }
}
