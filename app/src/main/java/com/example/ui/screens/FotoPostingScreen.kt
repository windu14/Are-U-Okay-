package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.audio.AudioPlayerState
import com.example.data.remote.ITunesTrack
import com.example.data.repository.GoogleDriveRepository
import com.example.ui.components.SongCard
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PlayfairBoldFamily
import com.example.ui.theme.PlayfairRegularFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Custom extension to draw dashed ticket style border on photo upload container.
 */
fun Modifier.dashedBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color,
    cornerRadius: Dp = 16.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp
): Modifier = this.drawWithContent {
    drawContent()
    val strokeWidthPx = strokeWidth.toPx()
    val dashLengthPx = dashLength.toPx()
    val gapLengthPx = gapLength.toPx()
    val cornerRadiusPx = cornerRadius.toPx()

    val pathEffect = PathEffect.dashPathEffect(
        floatArrayOf(dashLengthPx, gapLengthPx), 0f
    )

    drawRoundRect(
        color = color,
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = pathEffect
        ),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun FotoPostingScreen(
    currentUsername: String,
    selectedTrack: ITunesTrack?,
    playerState: AudioPlayerState,
    onOpenMusicSearch: () -> Unit,
    onRemoveTrack: () -> Unit,
    onPlayTrackClick: (previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String?) -> Unit,
    onSavePhotoNote: (
        caption: String,
        category: String,
        moodEmoji: String,
        photoUrl1: String,
        photoUrl2: String?,
        trackToAttach: ITunesTrack?
    ) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveRepo = remember { GoogleDriveRepository() }

    val selectedUris = remember { mutableStateListOf<Uri>() }
    var captionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Foto & Galeri") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatusText by remember { mutableStateOf("") }

    val maxCharCount = 50
    val charCount = captionText.length
    val isCharLimitExceeded = charCount > maxCharCount

    // Multi Photo Picker Launcher (Allows 1 or 2 photos)
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSlots = 2 - selectedUris.size
            if (remainingSlots > 0) {
                val newUris = uris.take(remainingSlots)
                selectedUris.addAll(newUris)
                if (uris.size > remainingSlots) {
                    Toast.makeText(context, "Maksimal hanya bisa memilih 2 foto!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Maksimal sudah 2 foto terpilih!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categoriesList = listOf(
        "Foto & Galeri",
        "Asmara & Cinta",
        "Perjalanan Jati Diri",
        "Masalah Hidup",
        "Pendidikan & Sekolah"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Navigation Bar (Compact & Precision Top Header like WriteCurhatScreen)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Posting Foto Curhat 📸",
                        fontFamily = PlayfairBoldFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelLavender
                    )
                    Text(
                        text = "Simpan foto ke Google Drive & bagikan di feed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Banner Tips Ukuran Foto
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PastelLavender.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PastelLavender.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PastelLavender,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tips Unggah Foto",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kamu bisa mengunggah 1 atau 2 foto (maks 10MB/foto). Foto akan disimpan otomatis di Google Drive Cloud.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // 2. Photo Pick & Ticket Dashed Border Container
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = PastelRose,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upload Foto",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Badge count (e.g. 1/2 Foto)
                            Surface(
                                shape = CircleShape,
                                color = if (selectedUris.isNotEmpty()) PastelMint.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${selectedUris.size}/2 Foto",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedUris.isNotEmpty()) PastelMint else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Photo Thumbnails / Dashed Ticket Picker Box
                        if (selectedUris.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedUris.forEachIndexed { index, uri ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(4f / 3f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .dashedBorder(
                                                strokeWidth = 1.5.dp,
                                                color = PastelLavender.copy(alpha = 0.7f),
                                                cornerRadius = 14.dp
                                            )
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(uri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Preview Foto ${index + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Remove Button
                                        IconButton(
                                            onClick = { selectedUris.removeAt(index) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Hapus Foto",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Placeholder slot if only 1 photo is selected
                                if (selectedUris.size == 1) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(4f / 3f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .dashedBorder(
                                                strokeWidth = 1.5.dp,
                                                color = PastelLavender.copy(alpha = 0.6f),
                                                cornerRadius = 14.dp
                                            )
                                            .clickable { pickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = PastelLavender,
                                                modifier = Modifier.size(26.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "+ Foto ke-2",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty Dashed Ticket Pick Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(125.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .dashedBorder(
                                        strokeWidth = 1.5.dp,
                                        color = PastelLavender.copy(alpha = 0.7f),
                                        cornerRadius = 16.dp
                                    )
                                    .clickable { pickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Pilih Foto",
                                        tint = PastelRose,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Pilih 1 atau 2 Foto dari Galeri",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sentuh di sini untuk memilih foto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (selectedUris.size < 2 && selectedUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { pickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = PastelLavender.copy(alpha = 0.18f)),
                                border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = PastelLavender,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tambah Foto ke-2 (Opsional)",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Caption Field (Maximum 50 letters/characters)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Caption / Pesan Foto",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // 50 Character Limit Counter Badge
                            Surface(
                                shape = CircleShape,
                                color = if (isCharLimitExceeded) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "$charCount / $maxCharCount huruf",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCharLimitExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { input ->
                                if (input.length <= maxCharCount) {
                                    captionText = input
                                }
                            },
                            placeholder = {
                                Text(
                                    "Tuliskan caption singkat (maksimal 50 huruf)...",
                                    fontFamily = PlayfairRegularFamily,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isCharLimitExceeded) MaterialTheme.colorScheme.error else PastelLavender,
                                unfocusedBorderColor = if (isCharLimitExceeded) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            isError = isCharLimitExceeded
                        )

                        if (isCharLimitExceeded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Caption terlalu panjang! Maksimal $maxCharCount huruf.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. Category Selector
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Kategori Post",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoriesList.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PastelLavender.copy(alpha = 0.35f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }

                // 5. Attach iTunes Song Option
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PastelLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attach Lagu (Opsional)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (selectedTrack != null) {
                                IconButton(
                                    onClick = onRemoveTrack,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Lagu",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedTrack != null) {
                            val effectivePreviewUrl = selectedTrack.previewUrl ?: ""
                            SongCard(
                                trackName = selectedTrack.trackName ?: "",
                                artistName = selectedTrack.artistName ?: "",
                                artworkUrl = selectedTrack.highResArtworkUrl ?: selectedTrack.artworkUrl100,
                                previewUrl = effectivePreviewUrl,
                                playerState = playerState,
                                cardId = "preview_posting_photo",
                                onPlayClick = {
                                    onPlayTrackClick(
                                        effectivePreviewUrl,
                                        selectedTrack.trackName ?: "",
                                        selectedTrack.artistName ?: "",
                                        selectedTrack.highResArtworkUrl ?: selectedTrack.artworkUrl100,
                                        "preview_posting_photo"
                                    )
                                }
                            )
                        } else {
                            Button(
                                onClick = onOpenMusicSearch,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PastelLavender,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+ Cari & Attach Lagu iTunes",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 6. Submit Post Button (Spacious, High Precision, Un-truncated)
                val isSubmitEnabled = selectedUris.isNotEmpty() &&
                        captionText.isNotBlank() &&
                        captionText.length <= maxCharCount &&
                        !isUploading

                Button(
                    onClick = {
                        if (selectedUris.isEmpty()) {
                            Toast.makeText(context, "Pilih minimal 1 foto terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (captionText.isBlank()) {
                            Toast.makeText(context, "Masukkan caption foto terlebih dahulu!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isCharLimitExceeded) {
                            Toast.makeText(context, "Caption tidak boleh lebih dari 50 huruf!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Start upload coroutine
                        scope.launch {
                            isUploading = true
                            val uploadedUrls = mutableListOf<String>()
                            var hasError = false

                            for (i in selectedUris.indices) {
                                val uri = selectedUris[i]
                                uploadStatusText = "Meng-upload foto ${i + 1}/${selectedUris.size} ke GDrive Cloud..."

                                val base64Data = withContext(Dispatchers.IO) {
                                    uriToBase64(context, uri)
                                }

                                if (base64Data == null) {
                                    hasError = true
                                    break
                                }

                                val fileName = "curhat_photo_${System.currentTimeMillis()}_${i + 1}.jpg"
                                val result = driveRepo.uploadPhotoToGDrive(
                                    webAppUrl = GoogleDriveRepository.DEFAULT_WEB_APP_URL,
                                    filename = fileName,
                                    mimeType = "image/jpeg",
                                    base64Data = base64Data,
                                    uploader = if (currentUsername.isNotBlank()) currentUsername else "Remaja Ceria"
                                )

                                result.onSuccess { photo ->
                                    uploadedUrls.add(photo.url)
                                }.onFailure { err ->
                                    android.util.Log.e("FotoPostingScreen", "Failed upload photo ${i + 1}", err)
                                    hasError = true
                                }

                                if (hasError) break
                            }

                            if (!hasError && uploadedUrls.isNotEmpty()) {
                                uploadStatusText = "Memposting ke Global Curhat..."
                                val photoUrl1 = uploadedUrls[0]
                                val photoUrl2 = uploadedUrls.getOrNull(1)

                                onSavePhotoNote(
                                    captionText,
                                    selectedCategory,
                                    "📸",
                                    photoUrl1,
                                    photoUrl2,
                                    selectedTrack
                                )

                                isUploading = false
                                Toast.makeText(context, "✨ Foto & Curhatan berhasil diposting!", Toast.LENGTH_LONG).show()
                                onBackClick()
                            } else {
                                isUploading = false
                                Toast.makeText(context, "❌ Gagal meng-upload foto ke Google Drive. Coba lagi.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = isSubmitEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PastelLavender,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = if (isSubmitEnabled) Color(0xFF261833) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unggah & Post ke Global Curhat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isSubmitEnabled) Color(0xFF261833) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }

            // Expressive Upload Loading Overlay
            if (isUploading) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularWavyProgressIndicator(
                            color = PastelLavender,
                            trackColor = PastelRose.copy(alpha = 0.4f),
                            modifier = Modifier.size(68.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = uploadStatusText.ifBlank { "Proses meng-upload foto ke Google Drive..." },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mohon tunggu sejenak, jangan tutup aplikasi",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// Helper method to convert URI to scaled Base64 JPEG string
private fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (originalBitmap == null) return null

        val maxDimension = 1280
        val width = originalBitmap.width
        val height = originalBitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (width >= height) maxDimension else (maxDimension * ratio).toInt()
            val newHeight = if (height > width) maxDimension else (maxDimension / ratio).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    } catch (e: Exception) {
        android.util.Log.e("FotoPostingScreen", "Error encoding uri to base64", e)
        null
    }
}
