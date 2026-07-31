package com.example.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class YouTubePlayerManager {

    companion object {
        fun buildPlayerOptions(): IFramePlayerOptions {
            return IFramePlayerOptions.Builder()
                .controls(1)
                .autoplay(0)
                .rel(0)
                .ivLoadPolicy(3)
                .ccLoadPolicy(0)
                .origin("https://www.youtube.com")
                .build()
        }

        fun setupPlayer(
            youTubePlayerView: YouTubePlayerView,
            lifecycleOwner: LifecycleOwner,
            videoId: String,
            onError: (PlayerConstants.PlayerError) -> Unit = {}
        ) {
            youTubePlayerView.enableAutomaticInitialization = false

            val observer = object : DefaultLifecycleObserver {
                private var isInitialized = false

                override fun onStart(owner: LifecycleOwner) {
                    if (!isInitialized) {
                        isInitialized = true
                        val options = buildPlayerOptions()
                        youTubePlayerView.initialize(
                            object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    youTubePlayer.cueVideo(videoId, 0f)
                                }

                                override fun onError(
                                    youTubePlayer: YouTubePlayer,
                                    error: PlayerConstants.PlayerError
                                ) {
                                    onError(error)
                                }
                            },
                            options
                        )
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(youTubePlayerView)
            lifecycleOwner.lifecycle.addObserver(observer)

            // If lifecycle is already at least STARTED when setup is called
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                observer.onStart(lifecycleOwner)
            }
        }
    }
}
