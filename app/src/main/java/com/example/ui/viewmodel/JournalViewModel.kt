package com.example.ui.viewmodel

import android.app.Application
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: JournalRepository
    val audioPlayer: AudioPreviewPlayer

    init {
        val db = AppDatabase.getDatabase(application)
        val api = ITunesApiService.create()
        repository = JournalRepository(db.journalNoteDao(), api)
        audioPlayer = AudioPreviewPlayer(application)

        // Pre-seed sample notes if DB is empty
        viewModelScope.launch {
            if (repository.allNotes.first().isEmpty()) {
                seedInitialNotes()
            }
        }
    }

    val playerState: StateFlow<AudioPlayerState> = audioPlayer.playerState

    val allNotes: StateFlow<List<JournalNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<JournalNote>> = repository.getRecentNotes(2)
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

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    private suspend fun seedInitialNotes() {
        // Gen Z & youth relatable initial curhat sample notes with popular songs
        val sample1 = ITunesTrack(
            trackId = 14352101,
            trackName = "Evaluasi",
            artistName = "Hindia",
            artworkUrl100 = "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/a5/d2/2b/a5d22bfb-090d-f215-d72b-8b548b2bf318/193483984185.jpg/100x100bb.jpg",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview115/v4/4a/01/fa/4a01fa4e-6e2c-3a2c-f608-410a56f642df/mzaf_10523214589094770258.plus.aac.p.m4a"
        )

        val sample2 = ITunesTrack(
            trackId = 15421902,
            trackName = "Secukupnya",
            artistName = "Hindia",
            artworkUrl100 = "https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/e2/0b/4f/e20b4f8a-98bb-7f8e-4a61-8418bf34a71a/193483984192.jpg/100x100bb.jpg",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview115/v4/a8/f5/83/a8f5835e-c045-8167-33f7-92e59e198b1b/mzaf_6285217482937402839.plus.aac.p.m4a"
        )

        val sample3 = ITunesTrack(
            trackId = 16210088,
            trackName = "Nanti Kita Terbit",
            artistName = "Sal Priadi",
            artworkUrl100 = "https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/80/7e/0b/807e0b11-5360-1e56-1188-4a112f43e110/198000109923.jpg/100x100bb.jpg",
            previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview116/v4/33/21/f8/3321f82b-8b41-9428-1b0b-932f91a18274/mzaf_1381273912048912837.plus.aac.p.m4a"
        )

        repository.addNote(
            content = "Lagi ngerasa overthinking banget soal masa depan & tugas kuliah yang gak selesai-selesai... Semoga besok hari bisa lebih bersahabat.",
            category = "Pendidikan & Sekolah",
            moodEmoji = "🥹",
            selectedTrack = sample1
        )

        repository.addNote(
            content = "Gak tau kenapa hari ini kangen sama sesosok orang yang udah gak pernah cerita lagi. Sedih tapi harus tetap jalanin hari.",
            category = "Asmara & Cinta",
            moodEmoji = "💔",
            selectedTrack = sample2
        )

        repository.addNote(
            content = "Lagi belajar untuk mencintai proses bertumbuh. Nggak apa-apa pelan-pelan, yang penting nggak nyerah sama keadaan.",
            category = "Perjalanan Jati Diri",
            moodEmoji = "✨",
            selectedTrack = sample1
        )

        repository.addNote(
            content = "Hari ini lumayan melelahkan, tapi dengerin lagu ini bikin suasana terasa lebih hangat.",
            category = "Masalah Hidup",
            moodEmoji = "🌧️",
            selectedTrack = sample3
        )
    }
}
