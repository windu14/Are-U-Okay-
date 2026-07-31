package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class YouTubeOEmbedResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "author_url") val authorUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "version") val version: String? = null,
    @Json(name = "html") val html: String? = null
)

data class YouTubeSearchResponse(
    @Json(name = "items") val items: List<YouTubeSearchItem>? = null
)

data class YouTubeSearchItem(
    @Json(name = "id") val id: YouTubeResourceId? = null,
    @Json(name = "snippet") val snippet: YouTubeSnippet? = null
)

data class YouTubeResourceId(
    @Json(name = "videoId") val videoId: String? = null
)

data class YouTubeSnippet(
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "channelTitle") val channelTitle: String? = null,
    @Json(name = "thumbnails") val thumbnails: YouTubeThumbnails? = null
)

data class YouTubeThumbnails(
    @Json(name = "high") val high: YouTubeThumbnailDetails? = null,
    @Json(name = "medium") val medium: YouTubeThumbnailDetails? = null,
    @Json(name = "default") val default: YouTubeThumbnailDetails? = null
)

data class YouTubeThumbnailDetails(
    @Json(name = "url") val url: String? = null
)

interface YouTubeApiService {

    @GET("oembed")
    suspend fun getOEmbed(
        @Query("url") url: String,
        @Query("format") format: String = "json"
    ): Response<YouTubeOEmbedResponse>

    @GET("https://www.googleapis.com/youtube/v3/search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 10,
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>
}
