package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Open-Meteo daily forecast response. No API key required. */
data class WeatherResponseDto(
    @SerializedName("latitude")  val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("timezone")  val timezone: String = "",
    @SerializedName("daily")     val daily: DailyDataDto? = null
)

data class DailyDataDto(
    @SerializedName("time")                  val time: List<String> = emptyList(),
    @SerializedName("temperature_2m_max")    val maxTemp: List<Double> = emptyList(),
    @SerializedName("temperature_2m_min")    val minTemp: List<Double> = emptyList(),
    @SerializedName("precipitation_sum")     val precipitation: List<Double> = emptyList(),
    @SerializedName("weathercode")           val weatherCode: List<Int> = emptyList()
)
