package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupRulesTest {

    private val today = LocalDate.of(2026, 10, 14)

    private fun group(
        seats: Int = 10,
        taken: Int = 0,
        start: LocalDate = today.plusDays(10),
        end: LocalDate = today.plusDays(14),
        published: Boolean = true,
        cancelled: Boolean = false,
        price: Double = 15_000.0,
        title: String = "Ladakh road trip"
    ) = GroupTrip(
        title = title, destination = "Leh", startDate = start, endDate = end,
        pricePerPerson = price, totalSeats = seats, seatsTaken = taken,
        isPublished = published, isCancelled = cancelled
    )

    @Test
    fun validGroup_hasNoProblems() {
        assertTrue(GroupRules.validate(group(), today).isEmpty())
    }

    @Test
    fun validate_catchesEachProblem() {
        val bad = group(seats = 1, start = today.minusDays(1), end = today.minusDays(3), price = 0.0, title = " ")
        assertEquals(5, GroupRules.validate(bad, today).size)
    }

    @Test
    fun validate_seatsCannotDropBelowMembers() {
        assertEquals(1, GroupRules.validate(group(seats = 4, taken = 6), today).size)
    }

    @Test
    fun canJoin_openGroup() {
        assertEquals(JoinCheck.OK, GroupRules.canJoin(group(), alreadyJoined = false, today = today))
    }

    @Test
    fun canJoin_reasons() {
        assertEquals(JoinCheck.ALREADY_JOINED, GroupRules.canJoin(group(), true, today))
        assertEquals(JoinCheck.FULL, GroupRules.canJoin(group(seats = 5, taken = 5), false, today))
        assertEquals(JoinCheck.CANCELLED, GroupRules.canJoin(group(cancelled = true), false, today))
        assertEquals(JoinCheck.NOT_PUBLISHED, GroupRules.canJoin(group(published = false), false, today))
        assertEquals(JoinCheck.STARTED, GroupRules.canJoin(group(start = today), false, today))
    }

    @Test
    fun canLeave_untilDayBeforeStart() {
        assertTrue(GroupRules.canLeave(group(start = today.plusDays(1)), today))
        assertFalse(GroupRules.canLeave(group(start = today), today))
    }

    @Test
    fun seatsLabel() {
        assertEquals("Full", GroupRules.seatsLabel(group(seats = 4, taken = 4)))
        assertEquals("Last seat!", GroupRules.seatsLabel(group(seats = 4, taken = 3)))
        assertEquals("3 seats left", GroupRules.seatsLabel(group(seats = 4, taken = 1)))
    }
}
