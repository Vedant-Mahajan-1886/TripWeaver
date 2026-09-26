package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict

/**
 * Applies one piece of agent feedback to a trip's itinerary.
 * Pure Kotlin (no Android, no database) so it can be unit tested.
 *
 *  - SUGGEST_CHANGE → the activity is replaced by the agent's suggestion
 *    (same time slot and category; place details are cleared because it's a new place).
 *  - NOT_AVAILABLE  → the activity is removed from that day.
 */
object AgentSuggestionApplier {

    /** Returns the changed day. Throws [IllegalStateException] with a readable message if it can't apply. */
    fun apply(days: List<Itinerary>, feedback: ActivityFeedback): Itinerary {
        val day = days.firstOrNull { it.dayNumber == feedback.dayNumber }
            ?: error("Day ${feedback.dayNumber} is no longer in this trip.")

        val index = findActivityIndex(day, feedback)
            ?: error("\"${feedback.activityTitle}\" is no longer in Day ${feedback.dayNumber}.")

        val updated = when (feedback.verdict) {
            ActivityVerdict.SUGGEST_CHANGE -> {
                check(feedback.suggestion.isNotBlank()) { "The agent didn't include a replacement." }
                val old = day.activities[index]
                day.activities.toMutableList().apply {
                    this[index] = old.copy(
                        title = feedback.suggestion.trim(),
                        description = feedback.comment.trim().ifEmpty { "Suggested by your travel agent" },
                        location = "",
                        lat = null,
                        lng = null,
                        placeId = ""
                    )
                }
            }
            ActivityVerdict.NOT_AVAILABLE -> day.activities.toMutableList().apply { removeAt(index) }
            ActivityVerdict.AVAILABLE -> error("Nothing to apply — the agent confirmed this activity.")
        }
        return day.copy(activities = updated)
    }

    /**
     * Uses the stored position when the title still matches there; otherwise
     * looks the activity up by title (positions shift if an earlier one was removed).
     */
    private fun findActivityIndex(day: Itinerary, feedback: ActivityFeedback): Int? {
        val atIndex = day.activities.getOrNull(feedback.activityIndex)
        if (atIndex != null && atIndex.title == feedback.activityTitle) return feedback.activityIndex
        return day.activities.indexOfFirst { it.title == feedback.activityTitle }.takeIf { it >= 0 }
    }
}
