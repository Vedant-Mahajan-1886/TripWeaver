package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.TripShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TripCountdownTest {

    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun beforeStart_isUpcoming() {
        assertEquals(TripTiming.Upcoming(6), tripTiming("2026-10-20", "2026-10-24", today))
    }

    @Test
    fun onStartDate_startsToday() {
        assertEquals(TripTiming.StartsToday, tripTiming("2026-10-14", "2026-10-18", today))
    }

    @Test
    fun duringTrip_isOngoingWithDayNumber() {
        assertEquals(TripTiming.Ongoing(day = 3, totalDays = 5), tripTiming("2026-10-12", "2026-10-16", today))
    }

    @Test
    fun lastDay_isStillOngoing() {
        assertEquals(TripTiming.Ongoing(day = 3, totalDays = 3), tripTiming("2026-10-12", "2026-10-14", today))
    }

    @Test
    fun afterEnd_isFinished() {
        assertEquals(TripTiming.Finished, tripTiming("2026-10-01", "2026-10-05", today))
    }

    @Test
    fun badDate_isUnknown() {
        assertEquals(TripTiming.Unknown, tripTiming("soon", "2026-10-05", today))
    }

    @Test
    fun labels() {
        assertEquals("Tomorrow", TripTiming.Upcoming(1).label())
        assertEquals("In 5 days", TripTiming.Upcoming(5).label())
        assertEquals("In 3 weeks", TripTiming.Upcoming(21).label())
        assertEquals("Starts today", TripTiming.StartsToday.label())
    }

    @Test
    fun soon_meansWithinAWeekOrOnTrip() {
        assertTrue(TripTiming.Upcoming(7).isSoon())
        assertFalse(TripTiming.Upcoming(8).isSoon())
        assertTrue(TripTiming.Ongoing(1, 3).isSoon())
    }

    private fun share(id: Long, status: ShareStatus, start: String, end: String) = IncomingShare(
        share = TripShare(
            id = id, tripId = id, travellerEmail = "t@x.com", agentEmail = "a@x.com", message = "",
            status = status, decision = null, overallComment = "", priceEstimate = null, sentAt = 0, updatedAt = 0
        ),
        tripTitle = "Trip $id", tripDestination = "Goa", tripStartDate = start, tripEndDate = end, travellerName = "Riya"
    )

    @Test
    fun upcomingBookedTrips_onlyBookedNotFinished_soonestFirst() {
        val list = listOf(
            share(1, ShareStatus.BOOKED, "2026-12-01", "2026-12-05"),
            share(2, ShareStatus.BOOKED, "2026-10-12", "2026-10-16"), // on now
            share(3, ShareStatus.BOOKED, "2026-09-01", "2026-09-03"), // finished
            share(4, ShareStatus.QUOTE_SENT, "2026-10-20", "2026-10-22"), // not booked
            share(5, ShareStatus.BOOKED, "bad", "bad") // unknown dates
        )
        assertEquals(listOf(2L, 1L), upcomingBookedTrips(list, today).map { it.share.id })
    }
}
