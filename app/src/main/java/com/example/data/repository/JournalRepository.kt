package com.example.data.repository

import com.example.data.local.JournalNote
import com.example.data.local.JournalNoteDao
import com.example.data.local.SongFrequency
import com.example.data.remote.ITunesApiService
import com.example.data.remote.ITunesTrack
import kotlinx.coroutines.flow.Flow

class JournalRepository(
    private val journalNoteDao: JournalNoteDao,
    private val iTunesApiService: ITunesApiService
) {

    val allNotes: Flow<List<JournalNote>> = journalNoteDao.getAllNotes()

    fun getRecentNotes(limit: Int = 2): Flow<List<JournalNote>> {
        return journalNoteDao.getRecentNotes(limit)
    }

    fun getTopAttachedSongs(limit: Int = 3): Flow<List<SongFrequency>> {
        return journalNoteDao.getTopAttachedSongs(limit)
    }

    suspend fun searchSongs(query: String): Result<List<ITunesTrack>> {
        return try {
            val response = iTunesApiService.searchSongs(term = query)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addNote(
        content: String,
        category: String,
        moodEmoji: String,
        selectedTrack: ITunesTrack?
    ) {
        val note = JournalNote(
            content = content,
            category = category,
            moodEmoji = moodEmoji,
            timestamp = System.currentTimeMillis(),
            trackId = selectedTrack?.trackId,
            trackName = selectedTrack?.trackName,
            artistName = selectedTrack?.artistName,
            artworkUrl = selectedTrack?.highResArtworkUrl ?: selectedTrack?.artworkUrl100,
            previewUrl = selectedTrack?.previewUrl
        )
        journalNoteDao.insertNote(note)
    }

    suspend fun deleteNote(id: Int) {
        journalNoteDao.deleteNoteById(id)
    }

    suspend fun clearAllNotes() {
        journalNoteDao.clearAllNotes()
    }
}
