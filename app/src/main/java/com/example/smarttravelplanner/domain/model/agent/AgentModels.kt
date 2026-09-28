package com.example.smarttravelplanner.domain.model.agent

/** A travel agent the traveller can send trips to. */
data class AgentProfile(
    val email: String,
    val fullName: String,
    val agencyName: String = "",
    val city: String = "",
    val specialities: List<String> = emptyList()
) {
    /** False until the agent has filled in their agency details. */
    val hasProfile: Boolean get() = agencyName.isNotBlank()
}

/** Where a shared trip is in the review process. */
enum class ShareStatus(val label: String) {
    SENT("Sent"),
    VIEWED("Viewed"),
    RESPONDED("Feedback received"),
    WITHDRAWN("Withdrawn"),

    /** The traveller compared offers and chose this agent. */
    CONFIRMED("Chosen"),

    /** The traveller chose a different agent for this trip. */
    CLOSED("Closed"),

    // ── Booking (after the traveller chose this agent) ──────────────────────
    /** The agent sent a final quote (price, inclusions, how to pay). */
    QUOTE_SENT("Quote sent"),

    /** The traveller accepted the quote; the agent can now book. */
    QUOTE_ACCEPTED("Quote accepted"),

    /** The agent booked everything (booking reference given). */
    BOOKED("Booked"),

    /** The trip's end date has passed. */
    COMPLETED("Completed"),

    /** Cancelled by the traveller or the agent before it was booked. */
    CANCELLED("Cancelled");

    /** Finished reviewing: the agent can no longer change their review. */
    val isLocked: Boolean get() = this != SENT && this != VIEWED && this != RESPONDED

    /** This agent is the one the traveller is going ahead with (any booking stage). */
    val isChosen: Boolean
        get() = this == CONFIRMED || this == QUOTE_SENT || this == QUOTE_ACCEPTED ||
            this == BOOKED || this == COMPLETED

    /** No longer active for this trip. */
    val isEnded: Boolean get() = this == CLOSED || this == CANCELLED || this == WITHDRAWN

    companion object {
        fun fromStorage(value: String?): ShareStatus =
            entries.firstOrNull { it.name == value } ?: SENT
    }
}

/** The agent's overall answer. */
enum class ReviewDecision(val label: String) {
    APPROVED("Approved"),
    CHANGES_REQUESTED("Changes requested");

    companion object {
        fun fromStorage(value: String?): ReviewDecision? = entries.firstOrNull { it.name == value }
    }
}

/** The agent's verdict on a single activity. */
enum class ActivityVerdict(val label: String) {
    AVAILABLE("Available"),
    NOT_AVAILABLE("Not available"),
    SUGGEST_CHANGE("Suggest change");

    companion object {
        fun fromStorage(value: String?): ActivityVerdict =
            entries.firstOrNull { it.name == value } ?: AVAILABLE
    }
}

/** What the traveller did with an agent's suggestion. */
enum class SuggestionResponse(val label: String) {
    PENDING("Pending"),
    APPLIED("Applied"),
    DECLINED("Declined"),

    /** Another agent's suggestion for the same activity was applied first. */
    SUPERSEDED("Already changed");

    companion object {
        fun fromStorage(value: String?): SuggestionResponse =
            entries.firstOrNull { it.name == value } ?: PENDING
    }
}

/** One trip sent to one agent. */
data class TripShare(
    val id: Long,
    val tripId: Long,
    val travellerEmail: String,
    val agentEmail: String,
    val message: String,
    val status: ShareStatus,
    val decision: ReviewDecision?,
    val overallComment: String,
    val priceEstimate: Double?,
    val sentAt: Long,
    val updatedAt: Long
)

/** A share as the agent sees it on their dashboard. */
data class IncomingShare(
    val share: TripShare,
    val tripTitle: String,
    val tripDestination: String,
    val tripStartDate: String,
    val tripEndDate: String,
    val travellerName: String,
    /** True if the traveller applied changes to the trip after this agent's review. */
    val tripChangedSinceReview: Boolean = false
)

/** Stored feedback on one activity. */
data class ActivityFeedback(
    val id: Long,
    val shareId: Long,
    val dayNumber: Int,
    val activityIndex: Int,
    val activityTitle: String,
    val verdict: ActivityVerdict,
    val comment: String,
    val suggestion: String,
    val response: SuggestionResponse,
    /** When the feedback was saved or last responded to (epoch millis). */
    val updatedAt: Long = 0L
)

/** Feedback on a trip together with which share (agent) it came from. */
data class TripFeedbackItem(
    val feedback: ActivityFeedback,
    val agentEmail: String,
    val tripId: Long
)

/** A change the traveller applied to the trip, as shown to an agent. */
data class AppliedTripChange(
    val dayNumber: Int,
    val activityTitle: String,
    val verdict: ActivityVerdict,
    val suggestion: String,
    /** True if this agent suggested it; otherwise it came from another agent. */
    val suggestedByMe: Boolean,
    val appliedAt: Long
)

/** What the agent fills in for one activity before submitting. */
data class ActivityFeedbackInput(
    val dayNumber: Int,
    val activityIndex: Int,
    val activityTitle: String,
    val verdict: ActivityVerdict,
    val comment: String = "",
    val suggestion: String = ""
)

/** Everything the agent submits at the end of a review. */
data class AgentReviewSubmission(
    val decision: ReviewDecision,
    val overallComment: String = "",
    val priceEstimate: Double? = null,
    val activities: List<ActivityFeedbackInput> = emptyList()
)
