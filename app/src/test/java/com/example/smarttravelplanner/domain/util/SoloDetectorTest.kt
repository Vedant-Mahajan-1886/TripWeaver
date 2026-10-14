package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SoloDetectorTest {

    private var clock = 0L

    private fun trip(adults: Int, kids: Int = 0) = Trip(
        title = "Trip", destination = "Goa",
        startDate = LocalDate.of(2026, 1, 1), endDate = LocalDate.of(2026, 1, 3),
        budget = 10_000.0, adults = adults, kids = kids, createdAt = ++clock
    )

    @Test
    fun isSoloTrip_oneAdultNoKids() {
        assertTrue(SoloDetector.isSoloTrip(trip(1)))
        assertFalse(SoloDetector.isSoloTrip(trip(2)))
        assertFalse(SoloDetector.isSoloTrip(trip(1, kids = 1)))
    }

    @Test
    fun noTrips_noGuess() {
        assertFalse(SoloDetector.looksSolo(emptyList()))
    }

    @Test
    fun majorityOfRecentTrips_decides() {
        assertTrue(SoloDetector.looksSolo(listOf(trip(1))))                     // 1 of 1
        assertFalse(SoloDetector.looksSolo(listOf(trip(1), trip(2))))           // 1 of 2
        assertTrue(SoloDetector.looksSolo(listOf(trip(2), trip(1), trip(1))))   // 2 of 3
        assertFalse(SoloDetector.looksSolo(listOf(trip(1), trip(2), trip(2))))  // 1 of 3
    }

    @Test
    fun onlyTheLatestThreeTripsCount() {
        // Four old solo trips, then three newer family trips -> not solo any more
        val trips = listOf(trip(1), trip(1), trip(1), trip(1), trip(2), trip(2, 1), trip(3))
        assertFalse(SoloDetector.looksSolo(trips))
    }

    @Test
    fun state_nudgesOnlyWhenNoChoiceYet() {
        val soloTrips = listOf(trip(1), trip(1))
        assertEquals(SoloModeState(enabled = false, showNudge = true, detectedSolo = true), SoloDetector.state(null, soloTrips))
        assertEquals(SoloModeState(enabled = true, showNudge = false, detectedSolo = true), SoloDetector.state(true, soloTrips))
        assertEquals(SoloModeState(enabled = false, showNudge = false, detectedSolo = true), SoloDetector.state(false, soloTrips))
    }

    @Test
    fun state_choiceWinsEvenIfTripsAreNotSolo() {
        assertTrue(SoloDetector.state(true, listOf(trip(4))).enabled)
    }
}
