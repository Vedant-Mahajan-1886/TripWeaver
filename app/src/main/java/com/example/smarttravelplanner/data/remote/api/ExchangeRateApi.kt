package com.example.smarttravelplanner.data.remote.api

import com.example.smarttravelplanner.data.remote.dto.ExchangeRateResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ExchangeRateApi {
    @GET("v6/{apiKey}/latest/{baseCurrency}")
    suspend fun getLatestRates(
        @Path("apiKey") apiKey: String,
        @Path("baseCurrency") baseCurrency: String = "INR"
    ): ExchangeRateResponseDto
}
