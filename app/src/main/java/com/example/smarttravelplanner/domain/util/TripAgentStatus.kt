package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.ShareStatus

/**
 * One short label summarising where a trip is with its agents, for the badge
 * on My Trips cards. Returns null if the trip has no open agent requests.
 * Pure function so it can be unit tested.
 */
fun tripAgentStatusLabel(
    statuses: List<Pair<ShareStatus, ReviewDecision?>>,
    chosenAgency: String?
): String? {
    val open = statuses.filter { it.first != ShareStatus.WITHDRAWN }
    if (open.isEmpty()) return null
    return when {
        open.any { it.first == ShareStatus.COMPLETED } -> "Trip completed"
        open.any { it.first == ShareStatus.BOOKED } ->
            if (chosenAgency.isNullOrBlank()) "Booked" else "Booked: $chosenAgency"
        open.any { it.first == ShareStatus.QUOTE_ACCEPTED } -> "Booking in progress"
        open.any { it.first == ShareStatus.QUOTE_SENT } -> "Quote received"
        open.any { it.first.isChosen } ->
            if (chosenAgency.isNullOrBlank()) "Agent chosen" else "Going ahead: $chosenAgency"
        open.any { it.first == ShareStatus.RESPONDED && it.second == ReviewDecision.APPROVED } -> "Approved by agent"
        open.any { it.first == ShareStatus.RESPONDED } -> "Agent feedback"
        else -> "With agents"
    }
}
