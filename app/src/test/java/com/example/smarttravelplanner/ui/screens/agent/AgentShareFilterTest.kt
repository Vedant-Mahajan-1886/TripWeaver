package com.example.smarttravelplanner.ui.screens.agent

import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import com.example.smarttravelplanner.domain.model.agent.TripShare
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentShareFilterTest {

    private fun share(
        id: Long,
        destination: String,
        traveller: String,
        status: ShareStatus,
        sentAt: Long,
        start: String,
        message: String = ""
    ) = IncomingShare(
        share = TripShare(
            id = id, tripId = id, travellerEmail = "t$id@x.com", agentEmail = "a@x.com",
            message = message, status = status, decision = null, overallComment = "",
            priceEstimate = null, sentAt = sentAt, updatedAt = sentAt
        ),
        tripTitle = "$destination trip",
        tripDestination = destination,
        tripStartDate = start,
        tripEndDate = start,
        travellerName = traveller
    )

    private val goa = share(1, "Goa", "Riya Mehta", ShareStatus.SENT, sentAt = 100, start = "2026-12-20")
    private val jaipur = share(2, "Jaipur", "Karan Shah", ShareStatus.VIEWED, sentAt = 300, start = "2026-11-05",
        message = "Looking for heritage hotels")
    private val manali = share(3, "Manali", "Riya Mehta", ShareStatus.CLOSED, sentAt = 200, start = "not-a-date")
    private val all = listOf(goa, jaipur, manali)

    private fun ids(list: List<IncomingShare>) = list.map { it.share.id }

    @Test
    fun noFilter_newestFirst() {
        assertEquals(listOf(2L, 3L, 1L), ids(all.filterAndSort("", emptySet(), ShareSort.NEWEST)))
    }

    @Test
    fun oldestFirst() {
        assertEquals(listOf(1L, 3L, 2L), ids(all.filterAndSort("", emptySet(), ShareSort.OLDEST)))
    }

    @Test
    fun tripDate_soonestFirst_badDatesLast() {
        assertEquals(listOf(2L, 1L, 3L), ids(all.filterAndSort("", emptySet(), ShareSort.TRIP_DATE)))
    }

    @Test
    fun search_matchesDestination_caseInsensitive() {
        assertEquals(listOf(1L), ids(all.filterAndSort("  goa ", emptySet(), ShareSort.NEWEST)))
    }

    @Test
    fun search_matchesTravellerName() {
        assertEquals(listOf(3L, 1L), ids(all.filterAndSort("riya", emptySet(), ShareSort.NEWEST)))
    }

    @Test
    fun search_matchesMessage() {
        assertEquals(listOf(2L), ids(all.filterAndSort("heritage", emptySet(), ShareSort.NEWEST)))
    }

    @Test
    fun statusChip_limitsToThoseStatuses() {
        val closedChip = REQUEST_CHIPS.first { it.label == "Closed" }.statuses
        assertEquals(listOf(3L), ids(all.filterAndSort("", closedChip, ShareSort.NEWEST)))
    }

    @Test
    fun searchAndStatus_combine() {
        assertEquals(emptyList<Long>(), ids(all.filterAndSort("riya", setOf(ShareStatus.VIEWED), ShareSort.NEWEST)))
    }

    @Test
    fun firstChipOfEachTab_isAll() {
        assertEquals(emptySet<ShareStatus>(), REQUEST_CHIPS[0].statuses)
        assertEquals(emptySet<ShareStatus>(), BOOKING_CHIPS[0].statuses)
    }
}
