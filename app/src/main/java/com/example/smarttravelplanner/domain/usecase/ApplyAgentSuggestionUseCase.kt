package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.repository.AgentRepository
import com.example.smarttravelplanner.data.repository.ItineraryRepository
import com.example.smarttravelplanner.domain.model.agent.ActivityFeedback
import com.example.smarttravelplanner.domain.model.agent.SuggestionResponse
import com.example.smarttravelplanner.domain.util.AgentSuggestionApplier
import javax.inject.Inject

/**
 * Traveller accepts an agent's suggestion: updates the itinerary, then marks
 * the feedback as Applied. Trip Detail refreshes automatically because it
 * observes the itinerary.
 */
class ApplyAgentSuggestionUseCase @Inject constructor(
    private val itineraryRepository: ItineraryRepository,
    private val agentRepository: AgentRepository
) {
    suspend operator fun invoke(tripId: Long, feedback: ActivityFeedback): Result<Unit> = runCatching {
        val days = itineraryRepository.getItinerariesForTrip(tripId)
        val changedDay = AgentSuggestionApplier.apply(days, feedback)
        itineraryRepository.saveItineraries(listOf(changedDay))
        agentRepository.respondToFeedback(feedback.id, SuggestionResponse.APPLIED).getOrThrow()
    }
}
