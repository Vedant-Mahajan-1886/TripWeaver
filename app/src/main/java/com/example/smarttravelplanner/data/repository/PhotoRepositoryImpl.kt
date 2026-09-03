package com.example.smarttravelplanner.data.repository

import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor() : PhotoRepository {

    override suspend fun getDestinationPhotoUrl(destination: String): Result<String> = runCatching {
        // In Track B: If Unsplash access key is available, call UnsplashApi.
        // Otherwise, return empty or a curated Unsplash source image URL.
        ""
    }
}
