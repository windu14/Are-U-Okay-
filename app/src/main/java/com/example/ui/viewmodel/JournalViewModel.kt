package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerState
import com.example.audio.AudioPreviewPlayer
import com.example.data.AppUpdateInfo
import com.example.data.local.AiChatMessage
import com.example.data.local.CommentItem
import com.example.data.local.JournalNote
import com.example.data.local.SongFrequency
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GeminiMessage
import com.example.data.remote.ITunesApiService
import com.example.data.remote.ITunesTrack
import com.example.data.remote.UserProfile
import com.example.data.remote.YouTubeVideo
import com.example.data.repository.FirebaseRepository
import com.example.data.repository.JournalRepository
import com.example.util.GitHubUpdateManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepository = FirebaseRepository()
    private val iTunesApiService = ITunesApiService.create()
    private val geminiApiService = GeminiApiService()
    val audioPlayer: AudioPreviewPlayer

    private val _customApiKey = MutableStateFlow<String?>(null)

    // Auth state
    val currentUser: StateFlow<FirebaseUser?> = firebaseRepository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseRepository.getCurrentUser())

    val userProfile: StateFlow<UserProfile?> = currentUser.flatMapLatest { user ->
        if (user != null) {
            firebaseRepository.getUserProfileFlow(user.uid)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val updateManager = GitHubUpdateManager()
    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val _isDownloadingUpdate = MutableStateFlow(false)
    val isDownloadingUpdate: StateFlow<Boolean> = _isDownloadingUpdate.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    init {
        audioPlayer = AudioPreviewPlayer(application)
        val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedApiKey = prefs.getString("custom_gemini_api_key", null)
        if (!savedApiKey.isNullOrBlank()) {
            _customApiKey.value = savedApiKey
        }
        checkAppUpdates()
    }

    val playerState: StateFlow<AudioPlayerState> = audioPlayer.playerState

    // Firestore Notes state
    private val _localCreatedNotes = MutableStateFlow<List<JournalNote>>(emptyList())
    private val _isNotesLoading = MutableStateFlow(true)
    val isNotesLoading: StateFlow<Boolean> = _isNotesLoading.asStateFlow()

    val allNotes: StateFlow<List<JournalNote>> = combine(
        firebaseRepository.getAllNotesFlow(),
        _localCreatedNotes
    ) { remoteNotes, localNotes ->
        _isNotesLoading.value = false
        val unpostedLocal = localNotes.filter { local ->
            remoteNotes.none { remote ->
                remote.content == local.content && Math.abs(remote.timestamp - local.timestamp) < 10000
            }
        }
        (unpostedLocal + remoteNotes).sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<JournalNote>> = allNotes.map { notes ->
        notes.take(4)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topSongs: StateFlow<List<SongFrequency>> = allNotes.map { notes ->
        notes.filter { it.trackId != null }
            .groupBy { it.trackId!! }
            .map { (trackId, group) ->
                val first = group.first()
                SongFrequency(
                    trackId = trackId,
                    trackName = first.trackName ?: "",
                    artistName = first.artistName ?: "",
                    artworkUrl = first.artworkUrl ?: "",
                    previewUrl = first.previewUrl ?: "",
                    frequency = group.size
                )
            }
            .sortedByDescending { it.frequency }
            .take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // YouTube Videos StateFlow from Firestore
    val youtubeVideos: StateFlow<List<YouTubeVideo>> = firebaseRepository.getYouTubeVideosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addYouTubeVideo(url: String, title: String, description: String = "", category: String = "Refleksi & Curhat") {
        if (url.isBlank()) return
        viewModelScope.launch {
            firebaseRepository.addYouTubeVideo(url, title, description, category)
        }
    }

    // Comments State
    private val _activeNoteForComments = MutableStateFlow<JournalNote?>(null)
    val activeNoteForComments: StateFlow<JournalNote?> = _activeNoteForComments.asStateFlow()

    val activeComments: StateFlow<List<CommentItem>> = _activeNoteForComments.flatMapLatest { note ->
        if (note != null && note.docId.isNotBlank()) {
            firebaseRepository.getCommentsFlow(note.docId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openCommentsForNote(note: JournalNote) {
        _activeNoteForComments.value = note
    }

    fun closeComments() {
        _activeNoteForComments.value = null
    }

    fun addComment(content: String, parentCommentId: String? = null) {
        val note = _activeNoteForComments.value ?: return
        if (content.isBlank() || note.docId.isBlank()) return

        val user = currentUser.value
        val profile = userProfile.value
        val uid = user?.uid ?: ""
        val username = profile?.username ?: user?.displayName ?: "Remaja Ceria"

        viewModelScope.launch {
            firebaseRepository.addComment(
                noteDocId = note.docId,
                parentCommentId = parentCommentId,
                content = content,
                uid = uid,
                username = username
            )
        }
    }

    // Category Filter for Global Curhat
    private val _selectedCategory = MutableStateFlow("Semuanya")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredNotes: StateFlow<List<JournalNote>> = combine(allNotes, selectedCategory) { notes, category ->
        if (category == "Semuanya") notes
        else notes.filter { it.category.equals(category, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // iTunes Music Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ITunesTrack>>(emptyList())
    val searchResults: StateFlow<List<ITunesTrack>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Sheet / Modal visibility states
    private val _showAddNoteDialog = MutableStateFlow(false)
    val showAddNoteDialog: StateFlow<Boolean> = _showAddNoteDialog.asStateFlow()

    private val _showSearchMusicSheet = MutableStateFlow(false)
    val showSearchMusicSheet: StateFlow<Boolean> = _showSearchMusicSheet.asStateFlow()

    // Currently selected track for new note creation
    private val _selectedTrack = MutableStateFlow<ITunesTrack?>(null)
    val selectedTrack: StateFlow<ITunesTrack?> = _selectedTrack.asStateFlow()

    private var searchJob: Job? = null

    // Authentication Actions
    fun login(emailOrUsername: String, pass: String, onSuccess: () -> Unit) {
        if (emailOrUsername.isBlank() || pass.isBlank()) {
            _authError.value = "Mohon isi semua bidang formulir!"
            return
        }
        _authLoading.value = true
        _authError.value = null

        viewModelScope.launch {
            val result = firebaseRepository.login(emailOrUsername, pass)
            _authLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Gagal masuk. Periksa kembali email/username & kata sandi."
            }
        }
    }

    fun signUp(username: String, email: String, pass: String, onSuccess: () -> Unit) {
        if (username.isBlank() || email.isBlank() || pass.isBlank()) {
            _authError.value = "Mohon isi semua bidang formulir!"
            return
        }
        if (pass.length < 6) {
            _authError.value = "Kata sandi minimal 6 karakter!"
            return
        }
        _authLoading.value = true
        _authError.value = null

        viewModelScope.launch {
            val result = firebaseRepository.signUp(username, email, pass)
            _authLoading.value = false
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Gagal mendaftar. Pastikan format email benar."
            }
        }
    }

    fun logout() {
        firebaseRepository.logout()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // Debounce typing
            _isSearching.value = true
            _searchError.value = null
            try {
                val response = iTunesApiService.searchSongs(term = query)
                _searchResults.value = response.results
            } catch (e: Exception) {
                _searchError.value = "Gagal memuat lagu dari iTunes. Coba kata kunci lain."
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun selectTrackForNote(track: ITunesTrack?) {
        _selectedTrack.value = track
    }

    fun openAddNoteDialog(preselectedTrack: ITunesTrack? = null) {
        if (preselectedTrack != null) {
            _selectedTrack.value = preselectedTrack
        }
        _showAddNoteDialog.value = true
    }

    fun dismissAddNoteDialog() {
        _showAddNoteDialog.value = false
        _selectedTrack.value = null
    }

    fun openSearchMusicSheet() {
        _showSearchMusicSheet.value = true
        if (_searchQuery.value.isBlank()) {
            updateSearchQuery("Hindia")
        }
    }

    fun dismissSearchMusicSheet() {
        _showSearchMusicSheet.value = false
    }

    fun playTrackPreview(previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String? = null) {
        audioPlayer.playPreview(previewUrl, title, artist, artworkUrl, cardId)
    }

    fun addNoteWithTrack(content: String, category: String, moodEmoji: String, trackToAttach: ITunesTrack? = null) {
        val user = firebaseRepository.getCurrentUser() ?: currentUser.value
        val profile = userProfile.value
        val uid = user?.uid ?: ""
        val username = profile?.username?.takeIf { it.isNotBlank() }
            ?: user?.displayName?.takeIf { it.isNotBlank() }
            ?: "Remaja Ceria"

        val now = System.currentTimeMillis()
        val newNote = JournalNote(
            id = (now % Int.MAX_VALUE).toInt(),
            docId = "",
            userId = uid,
            username = username,
            content = content,
            category = category,
            moodEmoji = moodEmoji,
            timestamp = now,
            trackId = trackToAttach?.trackId,
            trackName = trackToAttach?.trackName,
            artistName = trackToAttach?.artistName,
            artworkUrl = trackToAttach?.highResArtworkUrl ?: trackToAttach?.artworkUrl100,
            previewUrl = trackToAttach?.previewUrl
        )

        _localCreatedNotes.value = listOf(newNote) + _localCreatedNotes.value

        viewModelScope.launch {
            val result = firebaseRepository.addNote(
                content = content,
                category = category,
                moodEmoji = moodEmoji,
                selectedTrack = trackToAttach,
                uid = uid,
                username = username
            )
            result.onSuccess {
                _selectedTrack.value = null
            }.onFailure { err ->
                android.util.Log.e("JournalViewModel", "Error saving note to Firestore", err)
            }
            dismissAddNoteDialog()
            dismissSearchMusicSheet()
        }
    }

    fun addNote(content: String, category: String, moodEmoji: String) {
        addNoteWithTrack(content, category, moodEmoji, _selectedTrack.value)
    }

    fun addPhotoNote(
        caption: String,
        category: String,
        moodEmoji: String,
        photoUrl1: String,
        photoUrl2: String? = null,
        trackToAttach: ITunesTrack? = null,
        onComplete: (Boolean) -> Unit
    ) {
        val user = firebaseRepository.getCurrentUser() ?: currentUser.value
        val profile = userProfile.value
        val uid = user?.uid ?: ""
        val username = profile?.username?.takeIf { it.isNotBlank() }
            ?: user?.displayName?.takeIf { it.isNotBlank() }
            ?: "Remaja Ceria"

        val now = System.currentTimeMillis()
        val newNote = JournalNote(
            id = (now % Int.MAX_VALUE).toInt(),
            docId = "",
            userId = uid,
            username = username,
            content = caption,
            category = category,
            moodEmoji = moodEmoji,
            timestamp = now,
            trackId = trackToAttach?.trackId,
            trackName = trackToAttach?.trackName,
            artistName = trackToAttach?.artistName,
            artworkUrl = trackToAttach?.highResArtworkUrl ?: trackToAttach?.artworkUrl100,
            previewUrl = trackToAttach?.previewUrl,
            photoUrl1 = photoUrl1,
            photoUrl2 = photoUrl2
        )

        _localCreatedNotes.value = listOf(newNote) + _localCreatedNotes.value

        viewModelScope.launch {
            val result = firebaseRepository.addNote(
                content = caption,
                category = category,
                moodEmoji = moodEmoji,
                selectedTrack = trackToAttach,
                uid = uid,
                username = username,
                photoUrl1 = photoUrl1,
                photoUrl2 = photoUrl2
            )
            if (result.isSuccess) {
                _selectedTrack.value = null
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun deleteNote(id: Int) {
        val note = allNotes.value.find { it.id == id } ?: return
        _localCreatedNotes.value = _localCreatedNotes.value.filter { it.id != id }
        if (note.docId.isNotBlank()) {
            val user = currentUser.value
            viewModelScope.launch {
                firebaseRepository.deleteNote(note.docId, user?.uid ?: "")
            }
        }
    }

    // Gemini AI Chat State
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    fun saveCustomApiKey(key: String) {
        _customApiKey.value = key
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("custom_gemini_api_key", key).apply()
        _aiErrorMessage.value = null
        val lastUserMsg = _aiMessages.value.lastOrNull { it.isUser }
        if (lastUserMsg != null) {
            sendAiMessageInternal(lastUserMsg.text)
        }
    }

    fun sendAiMessage(prompt: String) {
        if (prompt.isBlank()) return
        val userMsg = AiChatMessage(text = prompt, isUser = true)
        val updatedList = _aiMessages.value + userMsg
        _aiMessages.value = updatedList
        sendAiMessageInternal(prompt)
    }

    private fun sendAiMessageInternal(prompt: String) {
        _isAiThinking.value = true
        _aiErrorMessage.value = null

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val minThinkingDelay = async { delay(2000L) }

            val history = _aiMessages.value.dropLast(1).map {
                GeminiMessage(role = if (it.isUser) "user" else "model", text = it.text)
            }

            // Collect app database & user context
            val profile = userProfile.value
            val user = currentUser.value
            val userNameStr = profile?.username?.takeIf { it.isNotBlank() }
                ?: user?.displayName?.takeIf { it.isNotBlank() }
                ?: "Sahabat"

            val notesList = allNotes.value
            val notesCountInt = notesList.size
            val recentNotesStr = notesList.take(3).joinToString("; ") {
                "${it.moodEmoji} [${it.category}]: \"${it.content.take(50)}\""
            }
            val topSongsStr = topSongs.value.take(3).joinToString(", ") {
                "${it.trackName} - ${it.artistName}"
            }

            val result = geminiApiService.sendMessage(
                chatHistory = history,
                userPrompt = prompt,
                apiKeyOverride = _customApiKey.value,
                userName = userNameStr,
                notesCount = notesCountInt,
                recentNotesSummary = recentNotesStr,
                topSongsSummary = topSongsStr
            )

            minThinkingDelay.await()
            val durationMs = System.currentTimeMillis() - startTime
            val durationSecStr = java.lang.String.format(java.util.Locale.US, "%.1f", durationMs / 1000f)

            _isAiThinking.value = false

            result.onSuccess { rawResponseText ->
                var cleanText = rawResponseText
                var actionSaved = false
                var attachedSongStr: String? = null

                // Detect and process automatic action tag: [ACTION_CREATE_NOTE: {...}]
                val tagStart = rawResponseText.indexOf("[ACTION_CREATE_NOTE:")
                if (tagStart != -1) {
                    val tagEnd = rawResponseText.indexOf("]", tagStart)
                    if (tagEnd != -1) {
                        val fullTag = rawResponseText.substring(tagStart, tagEnd + 1)
                        cleanText = rawResponseText.replace(fullTag, "").trim()
                        val jsonContent = fullTag.removePrefix("[ACTION_CREATE_NOTE:").removeSuffix("]").trim()

                        try {
                            val json = org.json.JSONObject(jsonContent)
                            val noteContent = json.optString("content", "").ifBlank { cleanText }
                            val category = json.optString("category", "Perjalanan Jati Diri")
                            val moodEmoji = json.optString("moodEmoji", "✨")
                            val songTitle = json.optString("songTitle", "")
                            val artistName = json.optString("artistName", "")

                            if (songTitle.isNotBlank()) {
                                attachedSongStr = if (artistName.isNotBlank()) "$songTitle - $artistName" else songTitle
                                val query = if (artistName.isNotBlank()) "$songTitle $artistName" else songTitle
                                var foundTrack: ITunesTrack? = null
                                try {
                                    val searchResp = iTunesApiService.searchSongs(term = query, limit = 3)
                                    foundTrack = searchResp.results.firstOrNull()
                                    if (foundTrack == null) {
                                        val fallbackResp = iTunesApiService.searchSongs(term = songTitle, limit = 3)
                                        foundTrack = fallbackResp.results.firstOrNull()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("JournalViewModel", "Failed iTunes search for $query", e)
                                }

                                if (foundTrack != null) {
                                    addNoteWithTrack(noteContent, category, moodEmoji, foundTrack)
                                } else {
                                    val fallbackTrack = ITunesTrack(
                                        trackId = System.currentTimeMillis(),
                                        trackName = songTitle,
                                        artistName = artistName.ifBlank { "Populer" },
                                        artworkUrl100 = "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/bf/16/23/bf162391-7f9b-16ef-8d6e-f78f85f3e970/cover.jpg/100x100bb.jpg",
                                        previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview112/v4/4a/c0/86/4ac08600-4b06-dfd5-89f3-8f0a1d48c89a/mzaf_13508688463567758778.plus.aac.p.m4a"
                                    )
                                    addNoteWithTrack(noteContent, category, moodEmoji, fallbackTrack)
                                }
                            } else {
                                addNoteWithTrack(noteContent, category, moodEmoji, null)
                            }
                            actionSaved = true
                        } catch (e: Exception) {
                            android.util.Log.e("JournalViewModel", "Failed to parse action tag JSON", e)
                        }
                    }
                }

                val aiMsg = AiChatMessage(
                    text = cleanText,
                    isUser = false,
                    thinkingTimeSec = durationSecStr,
                    actionNoteSaved = actionSaved,
                    attachedSongName = attachedSongStr
                )
                _aiMessages.value = _aiMessages.value + aiMsg
            }.onFailure { err ->
                _aiErrorMessage.value = err.message ?: "Terjadi kesalahan saat menghubungi Mochibot."
            }
        }
    }

    fun clearAiChat() {
        _aiMessages.value = emptyList()
        _aiErrorMessage.value = null
    }

    fun saveAiResponseToJournal(content: String, category: String = "Perjalanan Jati Diri", moodEmoji: String = "✨") {
        addNote(content, category, moodEmoji)
    }

    fun checkAppUpdates(manual: Boolean = false, context: Context? = null) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = updateManager.checkForUpdatesDetailed()
            _isCheckingUpdate.value = false

            when (result) {
                is com.example.util.UpdateCheckResult.Success -> {
                    _appUpdateInfo.value = result.updateInfo
                    if (manual && context != null) {
                        Toast.makeText(context, "Pembaruan versi ${result.updateInfo.latestVersionName} ditemukan!", Toast.LENGTH_SHORT).show()
                    }
                }
                is com.example.util.UpdateCheckResult.UpToDate -> {
                    if (manual && context != null) {
                        Toast.makeText(context, "Aplikasi Anda sudah versi terbaru (v${com.example.BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                    }
                }
                is com.example.util.UpdateCheckResult.Error -> {
                    if (manual && context != null) {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun startUpdateDownload(context: Context) {
        val info = _appUpdateInfo.value ?: return
        viewModelScope.launch {
            _isDownloadingUpdate.value = true
            _downloadError.value = null
            _downloadProgress.value = 0f
            updateManager.downloadAndInstallApk(
                context = context,
                downloadUrl = info.downloadUrl,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                },
                onError = { error ->
                    _isDownloadingUpdate.value = false
                    _downloadError.value = error
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        _appUpdateInfo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
