package com.example.smarttravelplanner.ui.screens.tripedit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgentEditNoticeTest {

    @Test
    fun noAgentsNoNotice() {
        assertNull(agentEditNoticeFor(null))
    }

    @Test
    fun chosenAgentIsNamed() {
        assertEquals(
            "You're going ahead with Wanderlust Journeys. They'll see what you change here.",
            agentEditNoticeFor("Going ahead: Wanderlust Journeys")
        )
    }

    @Test
    fun otherStatusesGetTheGeneralNotice() {
        listOf("With agents", "Agent feedback", "Approved by agent").forEach {
            assertEquals("Agents reviewing this trip will see what you change here.", agentEditNoticeFor(it))
        }
    }

    @Test
    fun bookedTripsWarnAboutReconfirmation() {
        assertEquals(
            "Your trip is booked with Wanderlust. After you change it, Wanderlust will need to re-confirm the bookings.",
            agentEditNoticeFor("Booked: Wanderlust")
        )
        assertNull(agentEditNoticeFor("Trip completed"))
    }
}
