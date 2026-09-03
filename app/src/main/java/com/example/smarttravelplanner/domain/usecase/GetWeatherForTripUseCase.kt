package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.repository.WeatherRepository
import com.example.smarttravelplanner.domain.model.DayWeather
import javax.inject.Inject

class GetWeatherForTripUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    /**
     * Fetches weather for a trip destination.
     * Lat/lng defaults are for New Delhi (used as fallback before Track B geocoding).
     */
    suspend operator fun invoke(
        lat: Double,
        lng: Double,
        startDate: String,
        endDate: String
    ): Result<List<DayWeather>> = weatherRepository.getForecast(lat, lng, startDate, endDate)
}
