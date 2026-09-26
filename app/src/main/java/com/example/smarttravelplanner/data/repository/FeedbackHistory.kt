package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.local.entity.ActivityFeedbackEntity

/**
 * When an agent updates their review, the new answers replace the old ones —
 * but the traveller's earlier decisions shouldn't be lost:
 *
 *  - An unchanged answer (same activity, verdict and suggestion) keeps the
 *    traveller's response (Applied / Declined / Already changed) and its time.
 *  - Suggestions the traveller already APPLIED are kept as history even though
 *    the activity is gone, so "Trip changed" notices and offer counts stay right.
 *
 * Pure function (no database) so it can be unit tested.
 */
internal fun mergeWithHistory(
    old: List<ActivityFeedbackEntity>,
    new: List<ActivityFeedbackEntity>
): List<ActivityFeedbackEntity> {
    fun ActivityFeedbackEntity.sameAnswerAs(other: ActivityFeedbackEntity) =
        dayNumber == other.dayNumber &&
            activityTitle == other.activityTitle &&
            verdict == other.verdict &&
            suggestion.trim() == other.suggestion.trim()

    val answered = old.filter { it.response != PENDING }

    val merged = new.map { fresh ->
        val previous = answered.firstOrNull { it.sameAnswerAs(fresh) }
        if (previous != null) fresh.copy(response = previous.response, updatedAt = previous.updatedAt) else fresh
    }

    val keptHistory = old.filter { oldRow ->
        oldRow.response == APPLIED && merged.none { it.sameAnswerAs(oldRow) }
    }.map { it.copy(id = 0) }

    return merged + keptHistory
}

private const val PENDING = "PENDING"
private const val APPLIED = "APPLIED"
