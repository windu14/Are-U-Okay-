package com.example.data.local

import java.util.UUID

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String? = null, // e.g. "Asmara & Cinta", "Pendidikan & Sekolah"
    val suggestedMood: String? = null, // e.g. "💔", "🥹", "✨"
    val thinkingTimeSec: String? = null, // e.g. "1.8"
    val actionNoteSaved: Boolean = false,
    val attachedSongName: String? = null
)
