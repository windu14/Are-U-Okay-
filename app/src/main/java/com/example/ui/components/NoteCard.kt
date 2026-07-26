package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.data.local.JournalNote
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(
    note: JournalNote,
    playerState: AudioPlayerState,
    onPlayTrackClick: (previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String?) -> Unit,
    onDeleteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (note.category) {
        "Asmara & Cinta" -> PastelRose
        "Pendidikan & Sekolah" -> PastelMint
        "Perjalanan Jati Diri" -> PastelLavender
        else -> MaterialTheme.colorScheme.tertiary
    }

    val noteCardId = "note_${note.id}"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge + Mood Emoji + Timestamp + Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.moodEmoji,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = note.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimestamp(note.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onDeleteClick(note.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Curhatan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Curhat Content
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = categoryColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Attached iTunes Song
            if (note.trackName != null && note.artistName != null && note.previewUrl != null) {
                Spacer(modifier = Modifier.height(14.dp))
                SongCard(
                    trackName = note.trackName,
                    artistName = note.artistName,
                    artworkUrl = note.artworkUrl,
                    previewUrl = note.previewUrl,
                    playerState = playerState,
                    cardId = noteCardId,
                    onPlayClick = {
                        onPlayTrackClick(
                            note.previewUrl,
                            note.trackName,
                            note.artistName,
                            note.artworkUrl,
                            noteCardId
                        )
                    }
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return if (diff < DateUtils.DAY_IN_MILLIS) {
        DateUtils.getRelativeTimeSpanString(
            timestamp,
            now,
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))
        sdf.format(Date(timestamp))
    }
}
