package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.ITunesTrack
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose

val CATEGORIES = listOf(
    "Asmara & Cinta",
    "Masalah Hidup",
    "Perjalanan Jati Diri",
    "Pendidikan & Sekolah"
)

val MOOD_EMOJIS = listOf("💔", "🥹", "🌧️", "✨", "❤️‍🔥", "🌿", "🎓", "😴", "🫠", "🥳", "🩹", "🌊", "🧸", "☕", "🕊️")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNoteDialog(
    selectedTrack: ITunesTrack?,
    onDismiss: () -> Unit,
    onSaveNote: (content: String, category: String, moodEmoji: String) -> Unit,
    onOpenMusicSearch: () -> Unit,
    onRemoveTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var contentText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CATEGORIES.first()) }
    var selectedEmoji by remember { mutableStateOf(MOOD_EMOJIS.first()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Are you okay?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PastelLavender
                        )
                        Text(
                            text = "Ceritakan apa yang sedang terasa...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mood Emoji Picker
                Text(
                    text = "Perasaan Saat Ini:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MOOD_EMOJIS) { emoji ->
                        val isSelected = selectedEmoji == emoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) PastelLavender.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) PastelLavender else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text(
                    text = "Kategori Curhatan:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORIES.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Note Content Input
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = {
                        Text(
                            "Tuliskan masalah hidup, asmara, pendidikan, atau unek-unekmu di sini...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PastelLavender,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Music Attachment Area
                Text(
                    text = "Lagu Pengiring (iTunes):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedTrack != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = PastelLavender)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedTrack.trackName ?: "Unknown Track",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedTrack.artistName ?: "Unknown Artist",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onRemoveTrack) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus Lagu", tint = PastelRose)
                            }
                        }
                    }
                } else {
                    Surface(
                        onClick = onOpenMusicSearch,
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PastelLavender.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = PastelLavender)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cari & Attach Lagu iTunes",
                                color = PastelLavender,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (contentText.isNotBlank()) {
                            onSaveNote(contentText.trim(), selectedCategory, selectedEmoji)
                        }
                    },
                    enabled = contentText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PastelLavender,
                        contentColor = Color(0xFF261833)
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Curhatan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
