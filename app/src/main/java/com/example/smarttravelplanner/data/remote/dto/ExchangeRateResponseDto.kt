package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponseDto(
    @SerializedName("result") val result: String = "",
    @SerializedName("base_code") val baseCode: String = "INR",
    @SerializedName("conversion_rates") val conversionRates: Map<String, Double> = emptyMap()
)
