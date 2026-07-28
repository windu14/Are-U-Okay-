package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_notes")
data class JournalNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val docId: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String,
    val category: String, // e.g. "Asmara & Cinta", "Masalah Hidup", "Perjalanan Jati Diri", "Pendidikan & Sekolah"
    val moodEmoji: String, // e.g. "💔", "🥹", "🌧️", "✨", "❤️‍🔥", "🎓"
    val timestamp: Long = System.currentTimeMillis(),
    
    // Attached iTunes song details (optional)
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val artworkUrl: String? = null,
    val previewUrl: String? = null,
    val attachCount: Int = 1
)

data class SongFrequency(
    val trackId: Long,
    val trackName: String,
    val artistName: String,
    val artworkUrl: String,
    val previewUrl: String,
    val frequency: Int
)
