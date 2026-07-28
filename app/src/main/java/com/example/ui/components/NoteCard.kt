package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.AudioPlayerState
import com.example.data.local.JournalNote
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PlayfairRegularFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(
    note: JournalNote,
    playerState: AudioPlayerState,
    onPlayTrackClick: (previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String?) -> Unit,
    onDeleteClick: (Int) -> Unit,
    onCommentClick: ((JournalNote) -> Unit)? = null,
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
            // Header Row: Author Capsule/Badge (Avatar + Username) + Timestamp & Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Author Capsule Badge (M3 Expressive)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(PastelLavender, PastelRose)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.maskot_1),
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (note.username.isNotBlank()) note.username else "Remaja Ceria",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

            Spacer(modifier = Modifier.height(10.dp))

            // Sub Header Row: Category Badge + Mood Emoji
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = note.moodEmoji,
                    fontSize = 20.sp,
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
                    fontFamily = PlayfairRegularFamily,
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

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Comment Action Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = { onCommentClick?.invoke(note) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Komentar",
                            tint = PastelLavender,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (note.commentCount > 0) "${note.commentCount} Komentar" else "Komentar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
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
