package com.example.smarttravelplanner.data.repository

interface PhotoRepository {
    /**
     * Retrieves a high quality photo URL for a destination or attraction.
     */
    suspend fun getDestinationPhotoUrl(destination: String): Result<String>
}
