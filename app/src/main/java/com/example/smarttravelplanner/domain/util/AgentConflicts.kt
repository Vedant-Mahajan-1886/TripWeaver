package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AppliedTripChange
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.model.agent.TripFeedbackItem

/**
 * Rules for when several agents review the same trip.
 * Pure Kotlin so it can be unit tested.
 */
object AgentConflicts {

    /** An activity in a trip, identified the same way feedback identifies it. */
    data class ActivityRef(val dayNumber: Int, val activityTitle: String)

    private val ActivityFeedback.ref get() = ActivityRef(dayNumber, activityTitle)
    private val ActivityFeedback.isOpenSuggestion
        get() = verdict != ActivityVerdict.AVAILABLE && response == SuggestionResponse.PENDING

    /**
     * Other agents' still-pending suggestions for the activity that [applied] just
     * changed. Once the activity is replaced or removed they no longer make sense.
     */
    fun supersededBy(applied: ActivityFeedback, all: List<TripFeedbackItem>): List<ActivityFeedback> =
        all.map { it.feedback }
            .filter { it.shareId != applied.shareId && it.ref == applied.ref && it.isOpenSuggestion }

    /** Activities that more than one agent has an open suggestion for. */
    fun conflictingActivities(all: List<TripFeedbackItem>): Set<ActivityRef> =
        all.map { it.feedback }
            .filter { it.isOpenSuggestion }
            .groupBy { it.ref }
            .filterValues { list -> list.map { it.shareId }.distinct().size > 1 }
            .keys

    /**
     * What the traveller applied to the trip after an agent's review
     * (their own suggestions and other agents'), oldest first.
     */
    fun changesSince(
        reviewedAt: Long,
        myShareId: Long,
        all: List<TripFeedbackItem>
    ): List<AppliedTripChange> =
        all.map { it.feedback }
            .filter { it.response == SuggestionResponse.APPLIED && it.updatedAt > reviewedAt }
            .sortedBy { it.updatedAt }
            .map {
                AppliedTripChange(
                    dayNumber = it.dayNumber,
                    activityTitle = it.activityTitle,
                    verdict = it.verdict,
                    suggestion = it.suggestion,
                    suggestedByMe = it.shareId == myShareId,
                    appliedAt = it.updatedAt
                )
            }
}
