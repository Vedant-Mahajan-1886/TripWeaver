package com.example.smarttravelplanner.data.repository

import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor() : CurrencyRepository {

    // Default static fallback rates against INR
    private val staticRatesFromInr = mapOf(
        "INR" to 1.0,
        "USD" to 0.012,
        "EUR" to 0.011,
        "GBP" to 0.0095,
        "JPY" to 1.82,
        "AUD" to 0.018
    )

    override suspend fun convert(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Result<Double> = runCatching {
        val from = fromCurrency.uppercase()
        val to = toCurrency.uppercase()
        if (from == to) return@runCatching amount

        val fromRate = staticRatesFromInr[from] ?: 1.0
        val toRate = staticRatesFromInr[to] ?: 1.0

        // convert from base INR
        val inInr = if (from == "INR") amount else amount / fromRate
        if (to == "INR") inInr else inInr * toRate
    }
}
