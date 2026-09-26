package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.ActivityVerdict
import com.example.smarttravelplanner.domain.model.agent.AgentReviewSubmission
import com.example.smarttravelplanner.domain.model.agent.ReviewDecision

/**
 * Checks an agent's review before it is saved. Returns a list of problems
 * (empty = valid). Pure Kotlin so it can be unit tested.
 */
object AgentReviewValidator {

    fun validate(review: AgentReviewSubmission): List<String> {
        val problems = mutableListOf<String>()

        review.priceEstimate?.let { price ->
            if (price < 0 || price.isNaN() || price.isInfinite()) {
                problems += "Price estimate must be zero or more."
            }
        }

        review.activities
            .filter { it.verdict == ActivityVerdict.SUGGEST_CHANGE && it.suggestion.isBlank() }
            .forEach { problems += "Day ${it.dayNumber}: add a suggestion for \"${it.activityTitle}\"." }

        val hasIssues = review.activities.any { it.verdict != ActivityVerdict.AVAILABLE }
        if (review.decision == ReviewDecision.CHANGES_REQUESTED &&
            !hasIssues && review.overallComment.isBlank()
        ) {
            problems += "Explain what should change: mark an activity or add a comment."
        }

        val duplicates = review.activities
            .groupBy { it.dayNumber to it.activityIndex }
            .filterValues { it.size > 1 }
        if (duplicates.isNotEmpty()) {
            problems += "Each activity can only be reviewed once."
        }

        return problems
    }
}
