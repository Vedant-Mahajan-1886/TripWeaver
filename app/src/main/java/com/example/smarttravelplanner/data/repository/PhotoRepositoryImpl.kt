package com.example.smarttravelplanner.data.repository

import android.util.Log
import com.example.smarttravelplanner.data.remote.api.WikipediaApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

/**
 * Implementation of [PhotoRepository] that fetches high-resolution destination
 * cover photos for free via the Wikipedia REST API without requiring any API keys.
 */
class PhotoRepositoryImpl @Inject constructor(
    private val wikipediaApi: WikipediaApi
) : PhotoRepository {

    override suspend fun getDestinationPhotoUrl(destination: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val cleaned = destination.trim().split(",").first().trim()
            if (cleaned.isBlank()) return@withContext ""

            Log.d("PhotoRepository", "Resolving photo for destination: '$cleaned'")

            // 1. Try direct destination title (e.g. "Assam")
            val directUrl = fetchWikiImage(cleaned)
            if (!directUrl.isNullOrBlank()) {
                Log.d("PhotoRepository", "Found direct photo for '$cleaned': $directUrl")
                return@withContext directUrl
            }

            // 2. Try with tourism disambiguation if needed (e.g. "Tourism_in_Assam")
            val tourismUrl = fetchWikiImage("Tourism_in_$cleaned")
            if (!tourismUrl.isNullOrBlank()) {
                Log.d("PhotoRepository", "Found tourism photo for '$cleaned': $tourismUrl")
                return@withContext tourismUrl
            }

            val stateUrl = fetchWikiImage("${cleaned}_(state)")
            if (!stateUrl.isNullOrBlank()) {
                Log.d("PhotoRepository", "Found state photo for '$cleaned': $stateUrl")
                return@withContext stateUrl
            }

            Log.w("PhotoRepository", "No photo found on Wikipedia for '$cleaned'")
            ""
        }
    }

    private suspend fun fetchWikiImage(title: String): String? {
        return try {
            val formattedTitle = title.replace(" ", "_")
            val encoded = URLEncoder.encode(formattedTitle, StandardCharsets.UTF_8.toString())
                .replace("+", "_")
            val response = wikipediaApi.getPageSummary(encoded)
            if (response.isSuccessful) {
                val body = response.body()
                // Prefer full resolution original image, fallback to thumbnail
                body?.originalimage?.source ?: body?.thumbnail?.source
            } else {
                Log.d("PhotoRepository", "Wikipedia query for '$title' returned HTTP ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("PhotoRepository", "Error fetching Wikipedia summary for '$title'", e)
            null
        }
    }
}
