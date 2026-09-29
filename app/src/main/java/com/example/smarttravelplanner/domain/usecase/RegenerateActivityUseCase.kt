package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.remote.dto.ActivityDto
import com.example.smarttravelplanner.data.repository.AiRepository
import com.example.smarttravelplanner.data.repository.UserPreferencesRepository
import com.example.smarttravelplanner.domain.model.Activity
import com.example.smarttravelplanner.domain.model.ActivityCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Uses Gemini / Local Ollama AI to regenerate or suggest a tailored alternative
 * for a specific destination/activity within a trip day.
 * Respects user's custom instructions or edited details.
 */
class RegenerateActivityUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(
        destination: String,
        dayNumber: Int,
        itineraryId: Long,
        currentActivity: Activity,
        otherActivities: List<Activity>,
        instructions: String = "",
        tripInterests: List<String> = emptyList()
    ): Result<Activity> = withContext(Dispatchers.IO) {
        runCatching {
            val prefs = userPreferencesRepository.userPreferences.first()
            val model = if (prefs.aiProvider.equals("LOCAL", ignoreCase = true)) {
                prefs.ollamaModel.ifBlank { "qwen3.5:4b" }
            } else {
                prefs.cloudModel.ifBlank { "gemini-3.1-flash-lite" }
            }

            val dto = aiRepository.regenerateActivity(
                destination = destination,
                dayNumber = dayNumber,
                currentActivity = currentActivity,
                otherActivities = otherActivities,
                instructions = instructions,
                model = model,
                tripInterests = tripInterests
            ).getOrThrow()

            dto.toActivity(itineraryId = itineraryId, currentActivityId = currentActivity.id)
        }
    }

    private fun ActivityDto.toActivity(itineraryId: Long, currentActivityId: Long) = Activity(
        id = currentActivityId,
        itineraryId = itineraryId,
        title = title.ifBlank { placeName },
        description = description,
        location = address.ifBlank { placeName },
        lat = lat.takeIf { it != 0.0 },
        lng = lng.takeIf { it != 0.0 },
        startTime = startTime,
        endTime = endTime,
        category = runCatching { ActivityCategory.valueOf(category) }.getOrDefault(ActivityCategory.ATTRACTION),
        estimatedCost = estimatedCostInr,
        placeId = placeId,
        weatherNote = weatherNote
    )
}
