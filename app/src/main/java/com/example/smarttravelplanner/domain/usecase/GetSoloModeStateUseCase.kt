package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.repository.SoloModeRepository
import com.example.smarttravelplanner.domain.util.SoloDetector
import com.example.smarttravelplanner.domain.util.SoloModeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * The one place that decides if Solo mode is on: the traveller's saved choice wins,
 * otherwise Solo mode stays off (and we only nudge if their trips look solo).
 * Used by Home, Plan, Packages and Explore so they always agree.
 */
class GetSoloModeStateUseCase @Inject constructor(
    private val getTripsUseCase: GetTripsUseCase,
    private val soloModeRepository: SoloModeRepository
) {
    operator fun invoke(): Flow<SoloModeState> = combine(
        getTripsUseCase(),
        soloModeRepository.observeSoloMode()
    ) { trips, savedChoice -> SoloDetector.state(savedChoice, trips) }
}
