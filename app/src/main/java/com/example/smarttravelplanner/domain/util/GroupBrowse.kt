package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.GroupTripListing

/** Search and filter for the traveller's list of group trips. Pure functions, unit tested. */
object GroupBrowse {

    /**
     * Keeps groups whose title, destination or agent name contains [query] (ignoring case),
     * and optionally hides full groups. Groups I already joined are never hidden.
     * Groups I joined come first; otherwise the order (soonest start) is kept.
     */
    fun filter(listings: List<GroupTripListing>, query: String, hideFull: Boolean): List<GroupTripListing> {
        val q = query.trim()
        return listings
            .filter { listing ->
                val g = listing.group
                val agentName = listing.agent?.let { it.agencyName.ifBlank { it.fullName } }.orEmpty()
                val matches = q.isEmpty() ||
                    g.title.contains(q, ignoreCase = true) ||
                    g.destination.contains(q, ignoreCase = true) ||
                    agentName.contains(q, ignoreCase = true)
                matches && (!hideFull || !g.isFull || listing.joinedByMe)
            }
            .sortedByDescending { it.joinedByMe } // stable: keeps the date order inside each part
    }
}
