package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BookingRulesTest {

    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun quoteCanBeSentAfterChoosingAndUpdatedUntilAnswered() {
        assertTrue(BookingRules.canSendQuote(ShareStatus.CONFIRMED))
        assertTrue(BookingRules.canSendQuote(ShareStatus.QUOTE_SENT))
        assertFalse(BookingRules.canSendQuote(ShareStatus.QUOTE_ACCEPTED))
        assertFalse(BookingRules.canSendQuote(ShareStatus.RESPONDED))
    }

    @Test
    fun expiredQuoteCannotBeAccepted() {
        assertTrue(BookingRules.canRespondToQuote(ShareStatus.QUOTE_SENT, today, today))
        assertTrue(BookingRules.canRespondToQuote(ShareStatus.QUOTE_SENT, null, today))
        assertFalse(BookingRules.canRespondToQuote(ShareStatus.QUOTE_SENT, today.minusDays(1), today))
        assertFalse(BookingRules.canRespondToQuote(ShareStatus.CONFIRMED, null, today))
    }

    @Test
    fun bookingOnlyAfterAcceptance() {
        assertTrue(BookingRules.canMarkBooked(ShareStatus.QUOTE_ACCEPTED))
        assertFalse(BookingRules.canMarkBooked(ShareStatus.QUOTE_SENT))
    }

    @Test
    fun cancellingIsOnlyPossibleBeforeBooking() {
        listOf(ShareStatus.CONFIRMED, ShareStatus.QUOTE_SENT, ShareStatus.QUOTE_ACCEPTED)
            .forEach { assertTrue(it.name, BookingRules.canCancel(it)) }
        listOf(ShareStatus.BOOKED, ShareStatus.COMPLETED, ShareStatus.CANCELLED)
            .forEach { assertFalse(it.name, BookingRules.canCancel(it)) }
    }

    @Test
    fun tripCompletesTheDayAfterItEnds() {
        assertFalse(BookingRules.isTripFinished(today, today))
        assertTrue(BookingRules.isTripFinished(today.minusDays(1), today))
    }

    @Test
    fun onlyCompletedTripsCanBeRated() {
        assertTrue(BookingRules.canRate(ShareStatus.COMPLETED))
        assertFalse(BookingRules.canRate(ShareStatus.BOOKED))
        assertEquals(1, BookingRules.validateRating(0).size)
        assertTrue(BookingRules.validateRating(5).isEmpty())
    }

    @Test
    fun changesAfterBookingNeedReconfirmation() {
        assertTrue(BookingRules.needsReconfirm(ShareStatus.BOOKED, confirmedAt = 1_000, lastTripChangeAt = 2_000))
        assertFalse(BookingRules.needsReconfirm(ShareStatus.BOOKED, confirmedAt = 3_000, lastTripChangeAt = 2_000))
        assertFalse(BookingRules.needsReconfirm(ShareStatus.BOOKED, confirmedAt = 1_000, lastTripChangeAt = null))
        assertFalse(BookingRules.needsReconfirm(ShareStatus.COMPLETED, confirmedAt = 1_000, lastTripChangeAt = 2_000))
    }

    @Test
    fun quoteNeedsAPriceAndAFutureExpiry() {
        assertTrue(BookingRules.validateQuote(45_000.0, today.plusDays(7), today).isEmpty())
        assertEquals(2, BookingRules.validateQuote(null, today.minusDays(1), today).size)
    }

    @Test
    fun stageFlags() {
        assertTrue(ShareStatus.BOOKED.isChosen)
        assertTrue(ShareStatus.BOOKED.isLocked)
        assertFalse(ShareStatus.RESPONDED.isLocked)
        assertTrue(ShareStatus.CANCELLED.isEnded)
        assertFalse(ShareStatus.CANCELLED.isChosen)
    }
}
