package com.example.smarttravelplanner.domain.usecase

import com.example.smarttravelplanner.data.repository.PlacesRepository
import com.example.smarttravelplanner.data.repository.WeatherRepository
import com.example.smarttravelplanner.domain.model.DayWeather
import java.time.LocalDate
import javax.inject.Inject

class GetWeatherForTripUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val placesRepository: PlacesRepository
) {
    /**
     * Fetches weather for given coordinates and date range strings.
     * Lat/lng defaults are for New Delhi (used as fallback before Track B geocoding).
     */
    suspend operator fun invoke(
        lat: Double,
        lng: Double,
        startDate: String,
        endDate: String
    ): Result<List<DayWeather>> = weatherRepository.getForecast(lat, lng, startDate, endDate)

    /**
     * Resolves coordinates for [destination] and fetches forecast for [startDate]..[endDate].
     * Clamps dates to Open-Meteo's forecast availability window (today to +15 days).
     * If dates are outside the forecast window or network fails, returns an empty list gracefully.
     */
    suspend fun getForDestination(
        destination: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DayWeather>> = runCatching {
        val today = LocalDate.now()
        val maxForecastDate = today.plusDays(15)

        // If trip starts after the forecast horizon or ended before today, no forecast is available
        if (startDate.isAfter(maxForecastDate) || endDate.isBefore(today)) {
            return@runCatching emptyList()
        }

        // Clamp the query window to valid Open-Meteo range
        val queryStart = if (startDate.isBefore(today)) today else startDate
        val queryEnd = if (endDate.isAfter(maxForecastDate)) maxForecastDate else endDate

        val (lat, lng) = placesRepository.geocodeDestination(destination)
            .getOrDefault(Pair(28.6139, 77.2090))

        weatherRepository.getForecast(
            lat = lat,
            lng = lng,
            startDate = queryStart.toString(),
            endDate = queryEnd.toString()
        ).getOrDefault(emptyList())
    }
}

