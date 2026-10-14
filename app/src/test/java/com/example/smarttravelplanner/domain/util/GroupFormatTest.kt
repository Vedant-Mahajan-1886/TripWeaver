package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GroupFormatTest {

    private val today = LocalDate.of(2026, 10, 14)

    private fun group(
        start: LocalDate = today.plusDays(10),
        end: LocalDate = today.plusDays(14),
        seats: Int = 8,
        taken: Int = 3,
        published: Boolean = true,
        cancelled: Boolean = false
    ) = GroupTrip(
        title = "Spiti circuit", destination = "Kaza", startDate = start, endDate = end,
        pricePerPerson = 12_500.0, totalSeats = seats, seatsTaken = taken,
        isPublished = published, isCancelled = cancelled
    )

    // -- Status ---------------------------------------------------------------

    @Test
    fun status_draftWhenNotPublished() {
        assertEquals(GroupStatus.DRAFT, GroupStatus.of(group(published = false), today))
    }

    @Test
    fun status_openWhenSeatsLeft() {
        assertEquals(GroupStatus.OPEN, GroupStatus.of(group(), today))
    }

    @Test
    fun status_fullWhenNoSeatsLeft() {
        assertEquals(GroupStatus.FULL, GroupStatus.of(group(seats = 8, taken = 8), today))
    }

    @Test
    fun status_cancelledWinsOverEverything() {
        assertEquals(GroupStatus.CANCELLED, GroupStatus.of(group(cancelled = true, taken = 8), today))
    }

    @Test
    fun status_ongoingBetweenStartAndEnd() {
        val g = group(start = today.minusDays(1), end = today.plusDays(2))
        assertEquals(GroupStatus.ONGOING, GroupStatus.of(g, today))
    }

    @Test
    fun status_completedAfterEndDate() {
        val g = group(start = today.minusDays(6), end = today.minusDays(1))
        assertEquals(GroupStatus.COMPLETED, GroupStatus.of(g, today))
    }

    // -- Text -----------------------------------------------------------------

    @Test
    fun dateRange_sameMonth() {
        assertEquals("12 \u2013 16 Nov 2026", GroupFormat.dateRange(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun dateRange_acrossMonths() {
        assertEquals("28 Nov \u2013 2 Dec 2026", GroupFormat.dateRange(LocalDate.of(2026, 11, 28), LocalDate.of(2026, 12, 2)))
    }

    @Test
    fun dateRange_acrossYears() {
        assertEquals("30 Dec 2026 \u2013 3 Jan 2027", GroupFormat.dateRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 3)))
    }

    @Test
    fun dateRange_singleDay() {
        val d = LocalDate.of(2026, 11, 12)
        assertEquals("12 Nov 2026", GroupFormat.dateRange(d, d))
    }

    @Test
    fun price_hasThousandsSeparator() {
        assertEquals("\u20B912,500 / person", GroupFormat.price(12_500.0))
    }

    @Test
    fun duration_nightsAndDays() {
        assertEquals("4 nights / 5 days", GroupFormat.duration(group()))
        assertEquals("1 night / 2 days", GroupFormat.duration(group(end = today.plusDays(11))))
        assertEquals("Day trip", GroupFormat.duration(group(end = today.plusDays(10))))
    }

    @Test
    fun seatsTaken_text() {
        assertEquals("3 of 8 seats taken", GroupFormat.seatsTaken(group()))
    }
}
