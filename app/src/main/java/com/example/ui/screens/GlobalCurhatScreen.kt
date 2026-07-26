package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AudioPlayerState
import com.example.data.local.JournalNote
import com.example.ui.components.CategoryChip
import com.example.ui.components.ExpressiveFabMenu
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
    modifier: Modifier = Modifier
) {
    var fabMenuExpanded by remember { mutableStateOf(false) }

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

            Spacer(modifier = Modifier.height(4.dp))

            // Notes List
            if (notes.isEmpty()) {
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            playerState = playerState,
                            onPlayTrackClick = onPlayTrackClick,
                            onDeleteClick = onDeleteNoteClick
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
