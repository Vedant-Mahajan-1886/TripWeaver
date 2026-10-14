package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.AgentProfile
import com.example.smarttravelplanner.domain.model.agent.GroupTrip
import com.example.smarttravelplanner.domain.model.agent.GroupTripListing
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GroupBrowseTest {

    private val today = LocalDate.of(2026, 10, 14)

    private fun listing(
        id: Long,
        title: String = "Trip $id",
        destination: String = "Goa",
        agency: String = "Wanderlust Journeys",
        seats: Int = 8,
        taken: Int = 2,
        joined: Boolean = false
    ) = GroupTripListing(
        group = GroupTrip(
            id = id, title = title, destination = destination,
            startDate = today.plusDays(id), endDate = today.plusDays(id + 3),
            pricePerPerson = 10_000.0, totalSeats = seats, seatsTaken = taken, isPublished = true
        ),
        agent = AgentProfile(email = "a$id@x.com", fullName = "Agent $id", agencyName = agency),
        rating = null,
        joinedByMe = joined
    )

    private fun ids(list: List<GroupTripListing>) = list.map { it.group.id }

    @Test
    fun emptyQuery_keepsEverythingInOrder() {
        val all = listOf(listing(1), listing(2), listing(3))
        assertEquals(listOf(1L, 2L, 3L), ids(GroupBrowse.filter(all, "", hideFull = false)))
    }

    @Test
    fun query_matchesTitleDestinationAndAgent_ignoringCase() {
        val all = listOf(
            listing(1, title = "Spiti circuit", destination = "Kaza"),
            listing(2, destination = "Goa"),
            listing(3, agency = "Himalayan High")
        )
        assertEquals(listOf(1L), ids(GroupBrowse.filter(all, "SPITI", false)))
        assertEquals(listOf(2L), ids(GroupBrowse.filter(all, "goa", false).filter { it.group.id == 2L }))
        assertEquals(listOf(3L), ids(GroupBrowse.filter(all, "himalayan", false)))
    }

    @Test
    fun query_withSpacesAround_isTrimmed() {
        val all = listOf(listing(1, destination = "Kaza"))
        assertEquals(listOf(1L), ids(GroupBrowse.filter(all, "  kaza ", false)))
    }

    @Test
    fun hideFull_removesFullGroups() {
        val all = listOf(listing(1, seats = 4, taken = 4), listing(2, seats = 4, taken = 3))
        assertEquals(listOf(2L), ids(GroupBrowse.filter(all, "", hideFull = true)))
    }

    @Test
    fun hideFull_keepsAFullGroupIJoined() {
        val all = listOf(listing(1, seats = 4, taken = 4, joined = true), listing(2, seats = 4, taken = 4))
        assertEquals(listOf(1L), ids(GroupBrowse.filter(all, "", hideFull = true)))
    }

    @Test
    fun joinedGroups_comeFirst_restKeepDateOrder() {
        val all = listOf(listing(1), listing(2, joined = true), listing(3), listing(4, joined = true))
        assertEquals(listOf(2L, 4L, 1L, 3L), ids(GroupBrowse.filter(all, "", false)))
    }

    @Test
    fun noMatch_givesEmptyList() {
        assertEquals(emptyList<Long>(), ids(GroupBrowse.filter(listOf(listing(1)), "paris", false)))
    }
}
