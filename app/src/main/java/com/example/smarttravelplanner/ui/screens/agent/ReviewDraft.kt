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
 */
internal fun initialDraft(share: TripShare, existing: List<ActivityFeedback>): ReviewDraft =
    ReviewDraft(
        activities = existing.associate { fb ->
            ActivityKey(fb.dayNumber, fb.activityIndex) to
                ActivityDraft(verdict = fb.verdict, comment = fb.comment, suggestion = fb.suggestion)
        },
        overallComment = share.overallComment,
        priceText = share.priceEstimate?.toLong()?.toString().orEmpty()
    )

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
