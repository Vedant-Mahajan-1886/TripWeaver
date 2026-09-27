package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.Itinerary
import com.example.smarttravelplanner.domain.model.Trip
import kotlinx.coroutines.flow.Flow

/** A change the traveller made by hand, for the edit log. */
data class TripEdit(val summary: String, val editedAt: Long)

/** Saves hand-made edits to a trip and keeps a log of them. */
interface TripEditRepository {

    /**
     * Saves the trip details and its days in one transaction.
     *
     * @param removedDayIds itinerary ids of days to delete (trip got shorter)
     * @param summaries     what changed, in words (stored in the edit log)
     */
    suspend fun saveEdits(
        trip: Trip,
        days: List<Itinerary>,
        removedDayIds: List<Long> = emptyList(),
        summaries: List<String>
    ): Result<Unit>

    /** Manual edits of one trip, oldest first. */
    fun observeEdits(tripId: Long): Flow<List<TripEdit>>
}
