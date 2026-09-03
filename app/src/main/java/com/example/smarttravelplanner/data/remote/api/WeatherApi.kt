package com.example.smarttravelplanner.data.remote.api

import com.example.smarttravelplanner.data.remote.dto.WeatherResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo weather forecast API.
 * Base URL: https://api.open-meteo.com/
 * No API key required — completely free for non-commercial use.
 */
interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getDailyForecast(
        @Query("latitude")           latitude: Double,
        @Query("longitude")          longitude: Double,
        @Query("daily")              daily: String = "temperature_2m_max,temperature_2m_min,precipitation_sum,weathercode",
        @Query("timezone")           timezone: String = "auto",
        @Query("start_date")         startDate: String,
        @Query("end_date")           endDate: String,
        @Query("forecast_days")      forecastDays: Int = 16
    ): WeatherResponseDto
}
