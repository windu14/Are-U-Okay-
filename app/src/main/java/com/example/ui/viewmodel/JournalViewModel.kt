package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPreviewPlayer
import com.example.audio.AudioPlayerState
import com.example.data.local.AppDatabase
import com.example.data.local.JournalNote
import com.example.data.local.SongFrequency
import com.example.data.remote.ITunesApiService
import com.example.data.remote.ITunesTrack
import com.example.data.repository.JournalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.local.AiChatMessage
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GeminiMessage

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JournalRepository
    private val geminiApiService = GeminiApiService()
    val audioPlayer: AudioPreviewPlayer

    init {
        val db = AppDatabase.getDatabase(application)
        val api = ITunesApiService.create()
        repository = JournalRepository(db.journalNoteDao(), api)
        audioPlayer = AudioPreviewPlayer(application)

        // Clear dummy seed data once so DB starts clean
        val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("dummy_data_cleared_v2", false)) {
            viewModelScope.launch {
                repository.clearAllNotes()
                prefs.edit().putBoolean("dummy_data_cleared_v2", true).apply()
            }
        }
    }

    val playerState: StateFlow<AudioPlayerState> = audioPlayer.playerState

    val allNotes: StateFlow<List<JournalNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<JournalNote>> = repository.getRecentNotes(4)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topSongs: StateFlow<List<SongFrequency>> = repository.getTopAttachedSongs(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            val result = repository.searchSongs(query)
            _isSearching.value = false
            result.onSuccess { tracks ->
                _searchResults.value = tracks
            }.onFailure { err ->
                _searchError.value = "Gagal memuat lagu dari iTunes. Coba kata kunci lain."
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
            // Default initial search suggestions for Gen Z vibes
            updateSearchQuery("Hindia")
        }
    }

    fun dismissSearchMusicSheet() {
        _showSearchMusicSheet.value = false
    }

    fun playTrackPreview(previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String? = null) {
        audioPlayer.playPreview(previewUrl, title, artist, artworkUrl, cardId)
    }

    fun addNote(content: String, category: String, moodEmoji: String) {
        viewModelScope.launch {
            repository.addNote(
                content = content,
                category = category,
                moodEmoji = moodEmoji,
                selectedTrack = _selectedTrack.value
            )
            dismissAddNoteDialog()
            dismissSearchMusicSheet()
        }
    }

    // Gemini AI Chat State
    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _aiErrorMessage = MutableStateFlow<String?>(null)
    val aiErrorMessage: StateFlow<String?> = _aiErrorMessage.asStateFlow()

    private val _customApiKey = MutableStateFlow<String?>(null)

    fun saveCustomApiKey(key: String) {
        _customApiKey.value = key
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
            val minThinkingDelay = async { delay(3000L) } // 3 detik AI berpikir

            // Build GeminiMessage list for API history
            val history = _aiMessages.value.dropLast(1).map {
                GeminiMessage(role = if (it.isUser) "user" else "model", text = it.text)
            }

            val result = geminiApiService.sendMessage(
                chatHistory = history,
                userPrompt = prompt,
                apiKeyOverride = _customApiKey.value
            )

            minThinkingDelay.await() // Pastikan durasi berpikir minimal 3 detik
            _isAiThinking.value = false

            result.onSuccess { responseText ->
                val aiMsg = AiChatMessage(text = responseText, isUser = false)
                _aiMessages.value = _aiMessages.value + aiMsg
            }.onFailure { err ->
                _aiErrorMessage.value = err.message ?: "Terjadi kesalahan saat menghubungi Teman AI."
            }
        }
    }

    fun clearAiChat() {
        _aiMessages.value = emptyList()
        _aiErrorMessage.value = null
    }

    fun saveAiResponseToJournal(content: String, category: String = "Perjalanan Jati Diri", moodEmoji: String = "✨") {
        viewModelScope.launch {
            repository.addNote(
                content = content,
                category = category,
                moodEmoji = moodEmoji,
                selectedTrack = _selectedTrack.value
            )
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
