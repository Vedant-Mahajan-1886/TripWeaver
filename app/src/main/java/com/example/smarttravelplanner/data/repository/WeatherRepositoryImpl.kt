package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.data.remote.api.WeatherApi
import com.example.smarttravelplanner.domain.model.DayWeather
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val weatherApi: WeatherApi
) : WeatherRepository {

    override suspend fun getForecast(
        lat: Double,
        lng: Double,
        startDate: String,
        endDate: String
    ): Result<List<DayWeather>> = runCatching {
        val response = weatherApi.getDailyForecast(
            latitude  = lat,
            longitude = lng,
            startDate = startDate,
            endDate   = endDate
        )

        val daily = response.daily ?: return@runCatching emptyList()

        daily.time.mapIndexed { i, date ->
            val code = daily.weatherCode.getOrElse(i) { 0 }
            DayWeather(
                date           = date,
                maxTempC       = daily.maxTemp.getOrElse(i) { 0.0 },
                minTempC       = daily.minTemp.getOrElse(i) { 0.0 },
                precipitationMm = daily.precipitation.getOrElse(i) { 0.0 },
                weatherCode    = code,
                description    = wmoDescription(code),
                iconEmoji      = wmoEmoji(code)
            )
        }
    }

    // WMO weather code → human-readable string
    private fun wmoDescription(code: Int): String = when (code) {
        0              -> "Clear sky"
        1              -> "Mainly clear"
        2              -> "Partly cloudy"
        3              -> "Overcast"
        45, 48         -> "Foggy"
        51, 53, 55     -> "Drizzle"
        61, 63, 65     -> "Rain"
        66, 67         -> "Freezing rain"
        71, 73, 75     -> "Snow"
        77             -> "Snow grains"
        80, 81, 82     -> "Rain showers"
        85, 86         -> "Snow showers"
        95             -> "Thunderstorm"
        96, 99         -> "Thunderstorm with hail"
        else           -> "Unknown"
    }

    // WMO weather code → emoji icon
    private fun wmoEmoji(code: Int): String = when (code) {
        0              -> "☀️"
        1              -> "🌤️"
        2              -> "⛅"
        3              -> "☁️"
        45, 48         -> "🌫️"
        in 51..55      -> "🌦️"
        in 61..67      -> "🌧️"
        in 71..77      -> "❄️"
        in 80..82      -> "🌧️"
        in 85..86      -> "🌨️"
        in 95..99      -> "⛈️"
        else           -> "🌡️"
    }
}
