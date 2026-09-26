package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.domain.model.DepartureCity
import com.example.smarttravelplanner.domain.model.ExploreDestination

/**
 * Source of destinations for the Explore screen.
 *
 * Today this is a curated local catalog. Because the ViewModel only depends on
 * this interface, it can later be backed by Google Places without touching the UI.
 */
interface DestinationRepository {

    /** All destinations available to explore. */
    suspend fun getDestinations(): Result<List<ExploreDestination>>

    /** Cities the user can pick in the "From" selector, with their coordinates. */
    fun getDepartureCities(): List<DepartureCity>
}
