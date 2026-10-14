package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.TripShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class AgentInsightsTest {

    private val utc: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 10, 14)

    private fun millis(date: String) = LocalDate.parse(date).atStartOfDay(utc).toInstant().toEpochMilli()

    private fun share(id: Long, status: ShareStatus) = IncomingShare(
        share = TripShare(
            id = id, tripId = id, travellerEmail = "t@x.com", agentEmail = "a@x.com", message = "",
            status = status, decision = null, overallComment = "", priceEstimate = null,
            sentAt = millis("2026-01-01"), updatedAt = millis("2026-01-01")
        ),
        tripTitle = "Trip $id", tripDestination = "Goa", tripStartDate = "2026-12-01",
        tripEndDate = "2026-12-05", travellerName = "Riya"
    )

    private fun booking(id: Long, total: Double, bookedOn: String? = null) = Booking(
        shareId = id, quoteTotal = total, quoteIncludes = "", quoteExcludes = "", paymentNote = "",
        quoteValidUntil = null, quoteSentAt = millis("2026-01-02"), quoteRespondedAt = null,
        bookingReference = "", bookingNotes = "", bookedAt = bookedOn?.let(::millis), reconfirmedAt = null,
        cancelledBy = null, cancelReason = "", cancelledAt = null
    )

    @Test
    fun noTrips_givesZerosAndNoRates() {
        val result = computeAgentInsights(emptyList(), emptyMap(), today, utc)
        assertEquals(0.0, result.earned, 0.0)
        assertNull(result.winRatePercent)
        assertNull(result.averageBooking)
        assertEquals(6, result.months.size)
        assertEquals(YearMonth.of(2026, 10), result.months.last().month)
        assertEquals(YearMonth.of(2026, 5), result.months.first().month)
    }

    @Test
    fun earned_countsBookedAndCompletedOnly() {
        val shares = listOf(
            share(1, ShareStatus.BOOKED),
            share(2, ShareStatus.COMPLETED),
            share(3, ShareStatus.QUOTE_SENT),
            share(4, ShareStatus.CANCELLED)
        )
        val bookings = mapOf(
            1L to booking(1, 20_000.0, "2026-10-03"),
            2L to booking(2, 30_000.0, "2026-09-10"),
            3L to booking(3, 15_000.0),
            4L to booking(4, 99_000.0)
        )
        val result = computeAgentInsights(shares, bookings, today, utc)
        assertEquals(50_000.0, result.earned, 0.0)
        assertEquals(2, result.bookedTrips)
        assertEquals(25_000.0, result.averageBooking!!, 0.0)
    }

    @Test
    fun pipeline_countsQuoteSentAndAccepted() {
        val shares = listOf(share(1, ShareStatus.QUOTE_SENT), share(2, ShareStatus.QUOTE_ACCEPTED), share(3, ShareStatus.CONFIRMED))
        val bookings = mapOf(1L to booking(1, 10_000.0), 2L to booking(2, 5_000.0))
        val result = computeAgentInsights(shares, bookings, today, utc)
        assertEquals(15_000.0, result.pipeline, 0.0)
        assertEquals(2, result.pipelineTrips)
    }

    @Test
    fun winRate_isChosenOverChosenPlusLost() {
        val shares = listOf(
            share(1, ShareStatus.BOOKED),
            share(2, ShareStatus.CONFIRMED),
            share(3, ShareStatus.CLOSED),
            share(4, ShareStatus.SENT) // undecided: not counted
        )
        val result = computeAgentInsights(shares, emptyMap(), today, utc)
        assertEquals(67, result.winRatePercent)
    }

    @Test
    fun months_groupByBookingMonth_andIgnoreOlderThanSixMonths() {
        val shares = listOf(share(1, ShareStatus.BOOKED), share(2, ShareStatus.BOOKED), share(3, ShareStatus.COMPLETED))
        val bookings = mapOf(
            1L to booking(1, 10_000.0, "2026-10-01"),
            2L to booking(2, 4_000.0, "2026-10-20"),
            3L to booking(3, 7_000.0, "2025-12-15") // too old for the chart
        )
        val result = computeAgentInsights(shares, bookings, today, utc)
        assertEquals(14_000.0, result.months.last().amount, 0.0)
        assertEquals(0.0, result.months.sumOf { it.amount } - 14_000.0, 0.0)
        assertEquals(21_000.0, result.earned, 0.0) // still counted in the total
    }
}
