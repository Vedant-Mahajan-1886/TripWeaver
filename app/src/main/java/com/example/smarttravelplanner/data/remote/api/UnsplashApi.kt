package com.example.smarttravelplanner.data.remote.api

import com.example.smarttravelplanner.data.remote.dto.UnsplashResponseDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface UnsplashApi {
    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("orientation") orientation: String = "landscape",
        @Header("Authorization") authorization: String
    ): UnsplashResponseDto
}
