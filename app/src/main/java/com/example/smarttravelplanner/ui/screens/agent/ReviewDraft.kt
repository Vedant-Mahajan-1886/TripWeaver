package com.example.smarttravelplanner.ui.screens.agent

import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedbackInput
import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AgentReviewSubmission
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision
import com.example.smarttravelplanner.domain.model.agent.TripShare

/** Identifies one activity inside a trip: day number + position in that day. */
data class ActivityKey(val dayNumber: Int, val activityIndex: Int)

/** What the agent has chosen so far for one activity. */
data class ActivityDraft(
    val verdict: ActivityVerdict = ActivityVerdict.AVAILABLE,
    val comment: String = "",
    val suggestion: String = ""
)

/** The agent's whole review while they are still editing it. */
data class ReviewDraft(
    val activities: Map<ActivityKey, ActivityDraft> = emptyMap(),
    val overallComment: String = "",
    /** Digits only, as typed; empty = no estimate */
    val priceText: String = "",
    val isSubmitting: Boolean = false,
    val problems: List<String> = emptyList()
) {
    fun draftFor(key: ActivityKey): ActivityDraft = activities[key] ?: ActivityDraft()

    val issueCount: Int get() = activities.values.count { it.verdict != ActivityVerdict.AVAILABLE }
}

/**
 * Starts a draft from what the agent already submitted (if anything), so an
 * earlier review can be opened and updated. Activities default to Available.
 *
 * Earlier answers are matched to the CURRENT itinerary by activity title
 * (position is only a hint), because the traveller may have removed or
 * replaced activities since. Answers for activities that no longer exist are dropped.
 */
internal fun initialDraft(
    share: TripShare,
    existing: List<ActivityFeedback>,
    days: List<Itinerary>
): ReviewDraft {
    val matched = mutableMapOf<ActivityKey, ActivityDraft>()
    existing.forEach { fb ->
        val day = days.firstOrNull { it.dayNumber == fb.dayNumber } ?: return@forEach
        val index = day.activities.getOrNull(fb.activityIndex)
            ?.takeIf { it.title == fb.activityTitle }
            ?.let { fb.activityIndex }
            ?: day.activities.indexOfFirst { it.title == fb.activityTitle }.takeIf { it >= 0 }
            ?: return@forEach
        val key = ActivityKey(fb.dayNumber, index)
        if (key !in matched) {
            matched[key] = ActivityDraft(verdict = fb.verdict, comment = fb.comment, suggestion = fb.suggestion)
        }
    }
    return ReviewDraft(
        activities = matched,
        overallComment = share.overallComment,
        priceText = share.priceEstimate?.toLong()?.toString().orEmpty()
    )
}

/**
 * Turns the draft into what gets saved. Every activity in the itinerary is
 * included (unchanged ones as Available) so the traveller sees a complete review.
 */
internal fun buildSubmission(
    days: List<Itinerary>,
    draft: ReviewDraft,
    decision: ReviewDecision
): AgentReviewSubmission =
    AgentReviewSubmission(
        decision = decision,
        overallComment = draft.overallComment,
        priceEstimate = draft.priceText.toDoubleOrNull(),
        activities = days.flatMap { day ->
            day.activities.mapIndexed { index, activity ->
                val choice = draft.draftFor(ActivityKey(day.dayNumber, index))
                ActivityFeedbackInput(
                    dayNumber = day.dayNumber,
                    activityIndex = index,
                    activityTitle = activity.title,
                    verdict = choice.verdict,
                    comment = choice.comment,
                    // A suggestion only makes sense for "Suggest change"
                    suggestion = if (choice.verdict == ActivityVerdict.SUGGEST_CHANGE) choice.suggestion else ""
                )
            }
        }
    )
