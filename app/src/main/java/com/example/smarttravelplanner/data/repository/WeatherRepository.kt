package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.DayWeather

interface WeatherRepository {
    /**
     * Fetches a daily weather forecast for the given coordinates and date range.
     * Uses Open-Meteo — no API key required.
     *
     * @param lat       Destination latitude
     * @param lng       Destination longitude
     * @param startDate ISO-8601 date string e.g. "2024-10-15"
     * @param endDate   ISO-8601 date string e.g. "2024-10-20"
     */
    suspend fun getForecast(
        lat: Double,
        lng: Double,
        startDate: String,
        endDate: String
    ): Result<List<DayWeather>>
}
