package com.example.smarttravelplanner.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WikipediaSummaryDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("thumbnail") val thumbnail: WikiImageDto? = null,
    @SerializedName("originalimage") val originalimage: WikiImageDto? = null
)

data class WikiImageDto(
    @SerializedName("source") val source: String? = null,
    @SerializedName("width") val width: Int? = null,
    @SerializedName("height") val height: Int? = null
)
