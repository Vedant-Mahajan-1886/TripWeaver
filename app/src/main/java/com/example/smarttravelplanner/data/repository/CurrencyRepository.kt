package com.example.smarttravelplanner.data.repository

interface CurrencyRepository {
    /**
     * Converts an amount from one currency to another.
     * Base rates default to INR.
     */
    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Result<Double>
}
