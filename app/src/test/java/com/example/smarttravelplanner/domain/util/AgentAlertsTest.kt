package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.TripShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AgentAlertsTest {

    private val today = LocalDate.of(2026, 10, 14)
    private val now = 10_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun share(
        id: Long,
        status: ShareStatus,
        start: String = "2026-12-01",
        end: String = "2026-12-05",
        sentAt: Long = now
    ) = IncomingShare(
        share = TripShare(
            id = id, tripId = id, travellerEmail = "t@x.com", agentEmail = "a@x.com", message = "",
            status = status, decision = null, overallComment = "", priceEstimate = null,
            sentAt = sentAt, updatedAt = sentAt
        ),
        tripTitle = "Trip $id", tripDestination = "Goa", tripStartDate = start, tripEndDate = end,
        travellerName = "Riya"
    )

    private fun quote(id: Long, validUntil: String?) = Booking(
        shareId = id, quoteTotal = 10_000.0, quoteIncludes = "", quoteExcludes = "", paymentNote = "",
        quoteValidUntil = validUntil?.let(LocalDate::parse), quoteSentAt = now, quoteRespondedAt = null,
        bookingReference = "", bookingNotes = "", bookedAt = null, reconfirmedAt = null,
        cancelledBy = null, cancelReason = "", cancelledAt = null
    )

    private fun alerts(shares: List<IncomingShare>, bookings: Map<Long, Booking> = emptyMap()) =
        computeAgentAlerts(shares, bookings, today, now)

    @Test
    fun nothingToDo_noAlerts() {
        assertTrue(alerts(listOf(share(1, ShareStatus.VIEWED), share(2, ShareStatus.COMPLETED))).isEmpty())
    }

    @Test
    fun chosenAndAccepted_areUrgent() {
        val result = alerts(listOf(share(1, ShareStatus.CONFIRMED), share(2, ShareStatus.QUOTE_ACCEPTED)))
        assertEquals(listOf(AlertLevel.URGENT, AlertLevel.URGENT), result.map { it.level })
        assertTrue(result[0].title.startsWith("Send a quote"))
        assertTrue(result[1].title.startsWith("Ready to book"))
    }

    @Test
    fun quoteExpiry_expiredTodayAndSoon() {
        val shares = listOf(share(1, ShareStatus.QUOTE_SENT), share(2, ShareStatus.QUOTE_SENT),
            share(3, ShareStatus.QUOTE_SENT), share(4, ShareStatus.QUOTE_SENT))
        val bookings = mapOf(
            1L to quote(1, "2026-10-10"), // expired
            2L to quote(2, "2026-10-14"), // today
            3L to quote(3, "2026-10-15"), // tomorrow
            4L to quote(4, "2026-10-30")  // far away: no alert
        )
        val result = alerts(shares, bookings)
        assertEquals(listOf(1L, 2L, 3L), result.map { it.shareId })
        assertEquals("Quote expires in 1 day \u00B7 Goa", result[2].title)
        assertEquals(AlertLevel.REMINDER, result[2].level)
    }

    @Test
    fun quoteWithoutExpiry_noAlert() {
        assertTrue(alerts(listOf(share(1, ShareStatus.QUOTE_SENT)), mapOf(1L to quote(1, null))).isEmpty())
    }

    @Test
    fun bookedTrip_startingWithinThreeDays() {
        val result = alerts(listOf(
            share(1, ShareStatus.BOOKED, start = "2026-10-15", end = "2026-10-18"),
            share(2, ShareStatus.BOOKED, start = "2026-10-14", end = "2026-10-16"),
            share(3, ShareStatus.BOOKED, start = "2026-10-25", end = "2026-10-28")
        ))
        assertEquals(listOf("Goa trip starts tomorrow", "Goa trip starts today"), result.map { it.title })
    }

    @Test
    fun staleRequests_combinedIntoOneReminder_pointingAtOldest() {
        val result = alerts(listOf(
            share(1, ShareStatus.SENT, sentAt = now - 3 * day),
            share(2, ShareStatus.SENT, sentAt = now - 5 * day),
            share(3, ShareStatus.SENT, sentAt = now - day) // only 1 day: not stale
        ))
        assertEquals(1, result.size)
        assertEquals("2 requests waiting over 2 days", result[0].title)
        assertEquals(2L, result[0].shareId)
    }

    @Test
    fun urgentAlertsComeFirst() {
        val result = alerts(listOf(
            share(1, ShareStatus.BOOKED, start = "2026-10-15", end = "2026-10-16"),
            share(2, ShareStatus.CONFIRMED)
        ))
        assertEquals(listOf(2L, 1L), result.map { it.shareId })
    }
}
