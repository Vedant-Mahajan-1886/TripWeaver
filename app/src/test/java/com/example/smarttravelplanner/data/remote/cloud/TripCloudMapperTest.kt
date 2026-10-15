package com.example.smarttravelplanner.data.remote.cloud

import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import com.example.smarttravelplanner.data.local.entity.TripEntity
import com.example.smarttravelplanner.data.remote.cloud.TripCloudMapper.effectiveUpdatedAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCloudMapperTest {

    private val trip = TripEntity(
        id = 7, title = "Goa getaway", destination = "Goa", startDate = "2026-12-01", endDate = "2026-12-04",
        budget = 25_000.0, adults = 2, kids = 1, interests = listOf("Beaches", "Food"), notes = "Near Baga",
        coverImageUrl = "https://img", createdAt = 1_000, ownerEmail = "riya@x.com", cloudId = "abc", updatedAt = 5_000
    )
    private val days = listOf(
        ItineraryEntity(id = 2, tripId = 7, dayNumber = 2, date = "2026-12-02", activitiesJson = "[2]"),
        ItineraryEntity(id = 1, tripId = 7, dayNumber = 1, date = "2026-12-01", activitiesJson = "[1]")
    )

    @Test
    fun roundTrip_keepsEverything() {
        val map = TripCloudMapper.toMap(trip, days, ownerUid = "uid-1")
        assertEquals("uid-1", map["ownerUid"])
        val back = TripCloudMapper.fromMap("abc", map)!!
        assertEquals(trip.copy(id = 0), back.trip)
        assertEquals(listOf(1, 2), back.days.map { it.dayNumber }) // sorted by day
        assertEquals("[1]", back.days[0].activitiesJson)
        assertFalse(back.deleted)
    }

    @Test
    fun firestoreNumbers_areConverted() {
        // Firestore returns whole numbers as Long and decimals as Double
        val map = TripCloudMapper.toMap(trip, days, "uid").toMutableMap().apply {
            put("budget", 25_000L)
            put("adults", 2L)
        }
        val back = TripCloudMapper.fromMap("abc", map)!!
        assertEquals(25_000.0, back.trip.budget, 0.0)
        assertEquals(2, back.trip.adults)
    }

    @Test
    fun brokenDocument_isSkipped() {
        assertNull(TripCloudMapper.fromMap("x", mapOf("destination" to "Goa")))
    }

    @Test
    fun oldTrips_useCreatedAtAsUpdateTime() {
        assertEquals(1_000L, trip.copy(updatedAt = 0).effectiveUpdatedAt)
        assertEquals(5_000L, trip.effectiveUpdatedAt)
    }

    @Test
    fun decide_newerCopyWins() {
        assertEquals(SyncAction.DOWNLOAD, TripCloudMapper.decide(null, 10, false))
        assertEquals(SyncAction.DOWNLOAD, TripCloudMapper.decide(5, 10, false))
        assertEquals(SyncAction.UPLOAD, TripCloudMapper.decide(20, 10, false))
        assertEquals(SyncAction.NOTHING, TripCloudMapper.decide(10, 10, false))
    }

    @Test
    fun decide_deletes() {
        assertEquals(SyncAction.NOTHING, TripCloudMapper.decide(null, 10, true))      // already gone here
        assertEquals(SyncAction.DELETE_LOCAL, TripCloudMapper.decide(5, 10, true))    // deleted elsewhere later
        assertEquals(SyncAction.UPLOAD, TripCloudMapper.decide(20, 10, true))         // edited here after delete
        assertTrue(TripCloudMapper.decide(10, 10, true) == SyncAction.DELETE_LOCAL)
    }
}
