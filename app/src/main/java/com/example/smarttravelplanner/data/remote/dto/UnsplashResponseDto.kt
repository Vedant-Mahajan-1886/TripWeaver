package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UnsplashResponseDto(
    @SerializedName("results") val results: List<UnsplashPhotoDto> = emptyList()
)

data class UnsplashPhotoDto(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String?,
    @SerializedName("urls") val urls: UnsplashUrlsDto?
)

data class UnsplashUrlsDto(
    @SerializedName("raw") val raw: String?,
    @SerializedName("full") val full: String?,
    @SerializedName("regular") val regular: String?,
    @SerializedName("small") val small: String?
)
