package com.example.smarttravelplanner.data.remote.api

import com.example.smarttravelplanner.data.remote.dto.WikipediaSummaryDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface WikipediaApi {

    @Headers("User-Agent: SmartTravelPlanner/1.0 (Android travel planner app)")
    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getPageSummary(
        @Path(value = "title", encoded = true) title: String
    ): Response<WikipediaSummaryDto>
}
