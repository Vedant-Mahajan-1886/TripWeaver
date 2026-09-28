package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripAgentStatusTest {

    @Test
    fun noOpenRequestsMeansNoBadge() {
        assertNull(tripAgentStatusLabel(emptyList(), null))
        assertNull(tripAgentStatusLabel(listOf(ShareStatus.WITHDRAWN to null), null))
    }

    @Test
    fun waitingForAgents() {
        assertEquals(
            "With agents",
            tripAgentStatusLabel(listOf(ShareStatus.SENT to null, ShareStatus.VIEWED to null), null)
        )
    }

    @Test
    fun approvalBeatsOtherFeedback() {
        assertEquals(
            "Approved by agent",
            tripAgentStatusLabel(
                listOf(
                    ShareStatus.RESPONDED to ReviewDecision.CHANGES_REQUESTED,
                    ShareStatus.RESPONDED to ReviewDecision.APPROVED
                ),
                null
            )
        )
        assertEquals(
            "Agent feedback",
            tripAgentStatusLabel(listOf(ShareStatus.RESPONDED to ReviewDecision.CHANGES_REQUESTED), null)
        )
    }

    @Test
    fun chosenAgentIsShownByName() {
        assertEquals(
            "Going ahead: Wanderlust Journeys",
            tripAgentStatusLabel(
                listOf(ShareStatus.CONFIRMED to ReviewDecision.APPROVED, ShareStatus.CLOSED to ReviewDecision.APPROVED),
                "Wanderlust Journeys"
            )
        )
    }

    @Test
    fun bookingStagesHaveTheirOwnBadge() {
        assertEquals("Quote received", tripAgentStatusLabel(listOf(ShareStatus.QUOTE_SENT to ReviewDecision.APPROVED), "W"))
        assertEquals("Booking in progress", tripAgentStatusLabel(listOf(ShareStatus.QUOTE_ACCEPTED to ReviewDecision.APPROVED), "W"))
        assertEquals("Booked: Wanderlust", tripAgentStatusLabel(listOf(ShareStatus.BOOKED to ReviewDecision.APPROVED), "Wanderlust"))
        assertEquals("Trip completed", tripAgentStatusLabel(listOf(ShareStatus.COMPLETED to ReviewDecision.APPROVED), "W"))
    }
}
