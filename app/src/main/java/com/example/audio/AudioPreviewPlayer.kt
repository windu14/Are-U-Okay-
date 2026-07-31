package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AudioPlayerState(
    val currentPreviewUrl: String? = null,
    val trackTitle: String? = null,
    val artistName: String? = null,
    val artworkUrl: String? = null,
    val activeCardId: String? = null, // e.g. "note_12", "top_101", "search_202"
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val currentPositionMs: Int = 0,
    val durationMs: Int = 30000, // standard 30s iTunes preview
    val errorMessage: String? = null
)

class AudioPreviewPlayer(private val context: Context) {

    private val appContext: Context = context.applicationContext

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun playPreview(previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String? = null) {
        if (previewUrl.isBlank()) return

        // If clicking same preview URL AND same cardId (or no cardId specified), toggle pause/play
        if (_playerState.value.currentPreviewUrl == previewUrl &&
            (_playerState.value.activeCardId == cardId || cardId == null) &&
            mediaPlayer != null) {
            if (_playerState.value.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        // Release existing player
        resetPlayer()

        _playerState.update {
            AudioPlayerState(
                currentPreviewUrl = previewUrl,
                trackTitle = title,
                artistName = artist,
                artworkUrl = artworkUrl,
                activeCardId = cardId,
                isPlaying = false,
                isBuffering = true
            )
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(appContext, android.net.Uri.parse(previewUrl))
                setOnPreparedListener { mp ->
                    _playerState.update { it.copy(isBuffering = false, isPlaying = true, durationMs = mp.duration) }
                    mp.start()
                    startProgressTracker()
                }
                setOnCompletionListener {
                    _playerState.update { it.copy(isPlaying = false, progress = 0f, currentPositionMs = 0) }
                    stopProgressTracker()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPreviewPlayer", "MediaPlayer Error: $what, $extra")
                    _playerState.update { 
                        it.copy(
                            isPlaying = false, 
                            isBuffering = false, 
                            errorMessage = "Gagal memutar preview lagu"
                        ) 
                    }
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e("AudioPreviewPlayer", "Error initializing player", e)
            _playerState.update {
                it.copy(isPlaying = false, isBuffering = false, errorMessage = "Tidak dapat memuat audio")
            }
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _playerState.update { state -> state.copy(isPlaying = false) }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPreviewPlayer", "Error pausing player", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.let {
                if (!it.isPlaying) {
                    it.start()
                    _playerState.update { state -> state.copy(isPlaying = true) }
                    startProgressTracker()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPreviewPlayer", "Error resuming player", e)
        }
    }

    fun stop() {
        resetPlayer()
        _playerState.value = AudioPlayerState()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = scope.launch {
            while (mediaPlayer != null && _playerState.value.isPlaying) {
                mediaPlayer?.let { mp ->
                    try {
                        val current = mp.currentPosition
                        val total = mp.duration.coerceAtLeast(1)
                        val prog = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        _playerState.update { 
                            it.copy(currentPositionMs = current, durationMs = total, progress = prog) 
                        }
                    } catch (e: Exception) {
                        // Player state change race condition
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun resetPlayer() {
        stopProgressTracker()
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
                reset()
                release()
            } catch (e: Exception) {
                Log.e("AudioPreviewPlayer", "Error resetting player", e)
            }
        }
        mediaPlayer = null
    }

    fun release() {
        resetPlayer()
    }
}
