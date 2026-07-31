package com.example.data.remote

data class YouTubeVideo(
    val id: String = "",
    val title: String = "",
    val youtubeUrl: String = "",
    val description: String = "",
    val category: String = "Refleksi & Curhat",
    val addedAt: Long = System.currentTimeMillis()
) {
    val videoId: String
        get() = extractYouTubeVideoId(youtubeUrl)

    val thumbnailUrl: String
        get() {
            val vid = videoId
            return if (vid.isNotBlank()) "https://img.youtube.com/vi/$vid/hqdefault.jpg" else ""
        }

    val embedUrl: String
        get() {
            val vid = videoId
            return if (vid.isNotBlank()) "https://www.youtube.com/embed/$vid?autoplay=1&rel=0&modestbranding=1" else ""
        }

    companion object {
        fun extractYouTubeVideoId(url: String): String {
            if (url.isBlank()) return ""
            return when {
                url.contains("youtu.be/") -> {
                    url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").trim()
                }
                url.contains("youtube.com/watch?v=") -> {
                    url.substringAfter("watch?v=").substringBefore("&").substringBefore("?").trim()
                }
                url.contains("youtube.com/shorts/") -> {
                    url.substringAfter("shorts/").substringBefore("?").substringBefore("&").trim()
                }
                url.contains("youtube.com/embed/") -> {
                    url.substringAfter("embed/").substringBefore("?").substringBefore("&").trim()
                }
                else -> url.trim()
            }
        }
    }
}
