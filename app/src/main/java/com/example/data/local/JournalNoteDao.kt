package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalNoteDao {

    @Query("SELECT * FROM journal_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<JournalNote>>

    @Query("SELECT * FROM journal_notes ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotes(limit: Int): Flow<List<JournalNote>>

    @Query("""
        SELECT trackId, trackName, artistName, artworkUrl, previewUrl, COUNT(*) as frequency 
        FROM journal_notes 
        WHERE trackId IS NOT NULL AND trackId > 0 AND trackName IS NOT NULL
        GROUP BY trackId 
        ORDER BY frequency DESC, MAX(timestamp) DESC 
        LIMIT :limit
    """)
    fun getTopAttachedSongs(limit: Int): Flow<List<SongFrequency>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: JournalNote)

    @Delete
    suspend fun deleteNote(note: JournalNote)

    @Query("DELETE FROM journal_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("DELETE FROM journal_notes")
    suspend fun clearAllNotes()
}
