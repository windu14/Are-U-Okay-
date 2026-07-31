package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.remote.YouTubeVideo
import com.example.data.repository.YouTubeRepository
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PlayfairBoldFamily
import com.example.ui.theme.PlayfairMediumItalicFamily
import com.example.util.YouTubePlayerManager
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(
    videos: List<YouTubeVideo>,
    onBackClick: () -> Unit,
    onAddVideo: (url: String, title: String, description: String, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val youtubeRepo = remember { YouTubeRepository() }

    var activeTab by remember { mutableStateOf(0) } // 0: Tersimpan, 1: Cari YouTube
    var selectedCategory by remember { mutableStateOf("Semua") }
    var playingVideoId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val categories = listOf("Semua", "Self Reflection", "Healing Vibes", "Filosofi Hidup", "Self Love")
    val quickSearchTopics = listOf("Meditasi Diri", "Relaksasi Pikiran", "Musik Lofi Chill", "Filosofi Stoik", "Suara Hujan & Alam", "Afirmasi Positif")

    // Trigger initial search / curated list
    LaunchedEffect(searchQuery) {
        isSearching = true
        searchResults = youtubeRepo.searchYouTubeVideos(searchQuery)
        isSearching = false
    }

    val savedFilteredVideos = remember(videos, selectedCategory) {
        if (selectedCategory == "Semua") {
            videos
        } else {
            videos.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Video Refleksi & Relaksasi 🎥",
                            fontFamily = PlayfairBoldFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pemutar & Pencarian Video YouTube In-App",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PastelLavender,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = PastelLavender
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Text(
                            "Koleksi Tersimpan (${videos.size})",
                            fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Text(
                            "🔍 Cari YouTube",
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            if (activeTab == 0) {
                // SAVED COLLECTION TAB
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PastelLavender.copy(alpha = 0.3f),
                                selectedLabelColor = PastelLavender,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                if (savedFilteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = PastelLavender,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum Ada Video Tersimpan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Buka tab 'Cari YouTube' untuk mencari dan memutar video inspiratif!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { activeTab = 1 },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PastelLavender,
                                    contentColor = Color(0xFF1E1B28)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cari Video Sekarang", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedFilteredVideos, key = { it.id }) { video ->
                            val isPlaying = playingVideoId == video.id
                            YouTubeVideoCard(
                                video = video,
                                isPlaying = isPlaying,
                                isSaved = true,
                                onPlayClick = {
                                    playingVideoId = if (isPlaying) null else video.id
                                },
                                onSaveClick = {},
                                onOpenExternalClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.youtubeUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Tidak dapat membuka link video", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // YOUTUBE SEARCH TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Ketik kata kunci (misal: meditasi, lofi, stoik)...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = PastelLavender)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PastelLavender,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Topics
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(quickSearchTopics) { topic ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (searchQuery.contains(topic, ignoreCase = true)) PastelLavender.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.2f)),
                                modifier = Modifier.clickable {
                                    searchQuery = topic
                                }
                            ) {
                                Text(
                                    text = topic,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (searchQuery.contains(topic, ignoreCase = true)) PastelLavender else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⏳ Mencari video di YouTube...", color = PastelLavender, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada video yang cocok dengan pencarian", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(searchResults, key = { it.videoId }) { video ->
                                val isPlaying = playingVideoId == video.videoId
                                val isAlreadySaved = videos.any { it.videoId == video.videoId }

                                YouTubeVideoCard(
                                    video = video,
                                    isPlaying = isPlaying,
                                    isSaved = isAlreadySaved,
                                    onPlayClick = {
                                        playingVideoId = if (isPlaying) null else video.videoId
                                    },
                                    onSaveClick = {
                                        if (!isAlreadySaved) {
                                            onAddVideo(video.youtubeUrl, video.title, video.description, video.category)
                                            Toast.makeText(context, "✨ Video disimpan ke koleksi!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onOpenExternalClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.youtubeUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Tidak dapat membuka link video", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeVideoCard(
    video: YouTubeVideo,
    isPlaying: Boolean,
    isSaved: Boolean,
    onPlayClick: () -> Unit,
    onSaveClick: () -> Unit,
    onOpenExternalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (video.category) {
        "Self Reflection" -> PastelLavender
        "Healing Vibes" -> PastelMint
        "Filosofi Hidup" -> PastelRose
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Media Container (Thumbnail or Embedded Player)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (isPlaying) {
                    YouTubeNativePlayerView(
                        videoId = video.videoId,
                        onOpenExternal = onOpenExternalClick,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onPlayClick() }
                    )

                    // Dark gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )

                    // Category Tag Top Left
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = video.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Play Button Overlay Center
                    Surface(
                        onClick = onPlayClick,
                        shape = CircleShape,
                        color = PastelLavender.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Putar Video",
                                tint = Color(0xFF1E1B28),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Info Section
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = video.title,
                    fontFamily = PlayfairBoldFamily,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (video.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.description,
                        fontFamily = PlayfairMediumItalicFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) PastelRose.copy(alpha = 0.3f) else PastelLavender.copy(alpha = 0.25f),
                            contentColor = if (isPlaying) PastelRose else PastelLavender
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Tutup Pemutar" else "Putar di Aplikasi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isSaved) {
                            IconButton(
                                onClick = onSaveClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Simpan ke Koleksi",
                                    tint = PastelLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdded,
                                contentDescription = "Tersimpan",
                                tint = PastelMint,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenExternalClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Buka di YouTube",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeNativePlayerView(
    videoId: String,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isError by remember(videoId) { mutableStateOf(false) }

    if (isError) {
        Box(
            modifier = modifier
                .background(Color(0xFF1E1B28))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Video ini tidak dapat diputar langsung di dalam aplikasi (Restricted by YouTube).",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenExternal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PastelLavender,
                        contentColor = Color(0xFF1E1B28)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka & Putar di App YouTube ↗", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    } else {
        AndroidView(
            factory = { context ->
                YouTubePlayerView(context).apply {
                    YouTubePlayerManager.setupPlayer(
                        youTubePlayerView = this,
                        lifecycleOwner = lifecycleOwner,
                        videoId = videoId,
                        onError = {
                            isError = true
                        }
                    )
                }
            },
            modifier = modifier
        )
    }
}

