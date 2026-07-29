package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioPlayerState
import com.example.data.local.JournalNote
import com.example.ui.components.CategoryChip
import com.example.ui.components.ExpressiveFabMenu
import com.example.ui.components.GlobalCurhatSkeletonList
import com.example.ui.components.NoteCard
import com.example.ui.theme.PastelLavender

val GLOBAL_CATEGORIES = listOf(
    "Semuanya",
    "Asmara & Cinta",
    "Masalah Hidup",
    "Perjalanan Jati Diri",
    "Pendidikan & Sekolah"
)

@Composable
fun GlobalCurhatScreen(
    notes: List<JournalNote>,
    selectedCategory: String,
    playerState: AudioPlayerState,
    onCategorySelect: (String) -> Unit,
    onPlayTrackClick: (previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String?) -> Unit,
    onDeleteNoteClick: (Int) -> Unit,
    onOpenAddNote: () -> Unit,
    onOpenMusicSearch: () -> Unit,
    onCommentClick: ((JournalNote) -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var fabMenuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val todayCount = remember(notes) {
        val now = System.currentTimeMillis()
        val twentyFourHoursAgo = now - 24 * 60 * 60 * 1000L
        notes.count { it.timestamp >= twentyFourHoursAgo || DateUtils.isToday(it.timestamp) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Title
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
                Text(
                    text = "Global Curhat 💬",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PastelLavender
                )
                Text(
                    text = "Semua unek-unek & irama lagu yang menemani hari-harimu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Category Filter Chips Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(GLOBAL_CATEGORIES) { cat ->
                    CategoryChip(
                        category = cat,
                        isSelected = selectedCategory.equals(cat, ignoreCase = true),
                        onClick = { onCategorySelect(cat) }
                    )
                }
            }

            // Realtime New Curhatan Banner Card (Slim & Elongated)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (todayCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = PastelLavender,
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = "$todayCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF261833),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "curhatan baru nih hari ini !!! 💬",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "belum ada curhatan hari ini",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notes List or Skeleton Loading
            if (isLoading) {
                GlobalCurhatSkeletonList()
            } else if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum Ada Curhatan 💌",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PastelLavender
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gunakan tombol + di pojok kanan bawah untuk menulis catatan & attach lagu iTunes pertamamu!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }, contentType = { "note_card" }) { note ->
                        NoteCard(
                            note = note,
                            playerState = playerState,
                            onPlayTrackClick = onPlayTrackClick,
                            onDeleteClick = onDeleteNoteClick,
                            onCommentClick = onCommentClick
                        )
                    }
                }
            }
        }

        // M3 Expressive Floating Action Button Menu
        ExpressiveFabMenu(
            expanded = fabMenuExpanded,
            onExpandedChange = { fabMenuExpanded = it },
            onAddNoteClick = onOpenAddNote,
            onAddMusicClick = onOpenMusicSearch,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        )
    }
}
