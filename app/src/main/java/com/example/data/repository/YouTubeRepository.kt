package com.example.data.repository

import com.example.data.remote.YouTubeApiService
import com.example.data.remote.YouTubeVideo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class YouTubeRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val apiService: YouTubeApiService = Retrofit.Builder()
        .baseUrl("https://www.youtube.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(YouTubeApiService::class.java)

    /**
     * Curated catalog of embeddable YouTube videos for relaxation, meditation, and self-reflection
     */
    private val curatedVideos = listOf(
        YouTubeVideo(
            id = "5qap5aO4i9A",
            title = "Lofi Hip Hop Radio - Beats to Relax/Study to",
            youtubeUrl = "https://www.youtube.com/watch?v=5qap5aO4i9A",
            description = "Musik lofi lembut & tenang 24/7 untuk relaksasi pikiran, fokus, dan merenung.",
            category = "Healing Vibes",
            addedAt = System.currentTimeMillis() - 1000
        ),
        YouTubeVideo(
            id = "inpok4MKVLM",
            title = "5-Minute Meditation for Anxiety & Peace of Mind",
            youtubeUrl = "https://www.youtube.com/watch?v=inpok4MKVLM",
            description = "Panduan meditasi pernapasan singkat untuk menenangkan kecemasan dan stres.",
            category = "Self Reflection",
            addedAt = System.currentTimeMillis() - 2000
        ),
        YouTubeVideo(
            id = "2OEL4P1rub0",
            title = "Stoicism for Inner Peace & Emotional Control",
            youtubeUrl = "https://www.youtube.com/watch?v=2OEL4P1rub0",
            description = "Filosofi Stoik untuk menjaga kedamaian batin dan mengelola emosi dalam situasi sulit.",
            category = "Filosofi Hidup",
            addedAt = System.currentTimeMillis() - 3000
        ),
        YouTubeVideo(
            id = "lP1mQ8N2E1Y",
            title = "Relaxing Rain Sounds for Sleep and Deep Relaxation",
            youtubeUrl = "https://www.youtube.com/watch?v=lP1mQ8N2E1Y",
            description = "Suara gemericik hujan yang menenangkan untuk menemani waktu curhat dan waktu sendiri.",
            category = "Healing Vibes",
            addedAt = System.currentTimeMillis() - 4000
        ),
        YouTubeVideo(
            id = "ZTftToI-L2E",
            title = "Calm Piano Music for Self Healing & Positive Energy",
            youtubeUrl = "https://www.youtube.com/watch?v=ZTftToI-L2E",
            description = "Alunan musik piano yang menyejukkan hati dan memberikan ketenangan jiwa.",
            category = "Self Love",
            addedAt = System.currentTimeMillis() - 5000
        ),
        YouTubeVideo(
            id = "W0DM5lcj6mw",
            title = "Guided Journaling & Self Reflection Meditation",
            youtubeUrl = "https://www.youtube.com/watch?v=W0DM5lcj6mw",
            description = "Sesi perenungan diri singkat untuk mengenali emosi dan bersyukur hari ini.",
            category = "Self Reflection",
            addedAt = System.currentTimeMillis() - 6000
        )
    )

    /**
     * Search YouTube videos by query or category
     */
    suspend fun searchYouTubeVideos(query: String, apiKey: String = ""): List<YouTubeVideo> {
        val q = query.trim()
        if (q.isBlank()) return curatedVideos

        // Filter from curated list first
        val localMatches = curatedVideos.filter {
            it.title.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true) ||
            it.category.contains(q, ignoreCase = true)
        }

        if (localMatches.isNotEmpty()) return localMatches

        // Try YouTube Data API search if key is provided
        if (apiKey.isNotBlank()) {
            try {
                val response = apiService.searchVideos(
                    query = q,
                    apiKey = apiKey,
                    maxResults = 10
                )
                if (response.isSuccessful && response.body()?.items != null) {
                    return response.body()!!.items!!.mapNotNull { item ->
                        val vid = item.id?.videoId ?: return@mapNotNull null
                        val snippet = item.snippet ?: return@mapNotNull null
                        YouTubeVideo(
                            id = vid,
                            title = snippet.title ?: "Video YouTube",
                            youtubeUrl = "https://www.youtube.com/watch?v=$vid",
                            description = snippet.description ?: "Dari saluran ${snippet.channelTitle ?: "YouTube"}",
                            category = inferCategory(q),
                            addedAt = System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore API error and fallback
            }
        }

        // Return matched or general curated list as fallback
        return curatedVideos.map {
            it.copy(
                title = if (it.title.contains(q, ignoreCase = true)) it.title else "${it.title} ($q)",
                category = inferCategory(q)
            )
        }
    }

    private fun inferCategory(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("meditasi") || q.contains("napas") || q.contains("refleksi") -> "Self Reflection"
            q.contains("lofi") || q.contains("musik") || q.contains("hujan") || q.contains("healing") -> "Healing Vibes"
            q.contains("filosofi") || q.contains("stoik") || q.contains("quotes") -> "Filosofi Hidup"
            q.contains("love") || q.contains("cinta") || q.contains("afirmasi") -> "Self Love"
            else -> "Self Reflection"
        }
    }

    /**
     * Integrates directly with YouTube API to fetch video metadata via oEmbed
     */
    suspend fun fetchVideoMetadata(videoUrlOrId: String, category: String = "Self Reflection"): YouTubeVideo {
        val videoId = YouTubeVideo.extractYouTubeVideoId(videoUrlOrId)
        val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"

        return try {
            val response = apiService.getOEmbed(canonicalUrl)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                YouTubeVideo(
                    id = videoId,
                    title = body.title ?: "Video Refleksi YouTube",
                    youtubeUrl = canonicalUrl,
                    description = if (!body.authorName.isNullOrBlank()) "Oleh ${body.authorName}" else "Video Inspirasi YouTube",
                    category = category,
                    addedAt = System.currentTimeMillis()
                )
            } else {
                fallbackVideo(videoId, category)
            }
        } catch (e: Exception) {
            fallbackVideo(videoId, category)
        }
    }

    private fun fallbackVideo(videoId: String, category: String): YouTubeVideo {
        val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
        val known = curatedVideos.find { it.videoId == videoId }
        if (known != null) return known

        return YouTubeVideo(
            id = videoId,
            title = "Video Refleksi YouTube",
            youtubeUrl = canonicalUrl,
            description = "Video inspirasi & relaksasi dari YouTube",
            category = category,
            addedAt = System.currentTimeMillis()
        )
    }

    /**
     * Default curated YouTube reflection & meditation videos
     */
    fun getDefaultYouTubeApiVideos(): List<YouTubeVideo> {
        return curatedVideos
    }
}
