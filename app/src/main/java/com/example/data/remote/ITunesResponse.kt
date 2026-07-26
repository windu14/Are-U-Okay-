package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ITunesSearchResponse(
    @Json(name = "resultCount") val resultCount: Int,
    @Json(name = "results") val results: List<ITunesTrack>
)

@JsonClass(generateAdapter = true)
data class ITunesTrack(
    @Json(name = "trackId") val trackId: Long = 0,
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "collectionName") val collectionName: String? = null,
    @Json(name = "artworkUrl100") val artworkUrl100: String? = null,
    @Json(name = "previewUrl") val previewUrl: String? = null,
    @Json(name = "primaryGenreName") val primaryGenreName: String? = null
) {
    // Helper to get higher resolution artwork
    val highResArtworkUrl: String?
        get() = artworkUrl100?.replace("100x100bb", "300x300bb") ?: artworkUrl100
}
