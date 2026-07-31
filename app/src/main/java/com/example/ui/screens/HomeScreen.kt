package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import com.example.R
import com.example.audio.AudioPlayerState
import com.example.data.local.JournalNote
import com.example.data.local.SongFrequency
import com.example.ui.components.HomeCarouselSkeletonRow
import com.example.ui.components.NoteCard
import com.example.ui.components.SongCard
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PlayfairBoldFamily
import com.example.ui.theme.PlayfairMediumItalicFamily
import com.example.ui.theme.PlayfairRegularFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BannerVideoPlayer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val rawUri = remember(context) {
        Uri.parse("android.resource://${context.packageName}/${R.raw.banner_animasi}")
    }

    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    try {
                        if (mediaPlayerRef?.isPlaying == true) {
                            mediaPlayerRef?.pause()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    try {
                        if (mediaPlayerRef != null && mediaPlayerRef?.isPlaying == false) {
                            mediaPlayerRef?.start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                    try {
                        mediaPlayerRef?.stop()
                        mediaPlayerRef?.release()
                        mediaPlayerRef = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                mediaPlayerRef?.stop()
                mediaPlayerRef?.release()
                mediaPlayerRef = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1B28))
    ) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            try {
                                mediaPlayerRef?.release()
                                mediaPlayerRef = MediaPlayer().apply {
                                    setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .build()
                                    )
                                    setDataSource(ctx, rawUri)
                                    setSurface(Surface(surface))
                                    isLooping = true
                                    setVolume(0f, 0f)
                                    setOnPreparedListener { mp -> mp.start() }
                                    prepareAsync()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            try {
                                mediaPlayerRef?.stop()
                                mediaPlayerRef?.release()
                                mediaPlayerRef = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    topSongs: List<SongFrequency>,
    recentNotes: List<JournalNote>,
    playerState: AudioPlayerState,
    onPlayTrackClick: (previewUrl: String, title: String, artist: String, artworkUrl: String?, cardId: String?) -> Unit,
    onDeleteNoteClick: (Int) -> Unit,
    onOpenAddNote: () -> Unit,
    onOpenMusicSearch: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenVideoScreen: () -> Unit = {},
    onCommentClick: ((JournalNote) -> Unit)? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Banner Animated Video Card
        item(key = "banner_video", contentType = "banner_video") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                BannerVideoPlayer()
            }
        }

        // Header Card Banner
        item(key = "header_banner", contentType = "header_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF2E243A),
                                Color(0xFF1E1B28)
                            )
                        )
                    )
            ) {
                // Semi-transparent supergraphic illustration in top left
                AsyncImage(
                    model = "file:///android_asset/bg_cards_a.svg",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(160.dp)
                        .alpha(0.35f)
                )

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PastelLavender.copy(alpha = 0.2f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = PastelRose,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PastelMint.copy(alpha = 0.2f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = PastelMint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("iTunes Powered", color = PastelMint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "are you okay? 💜",
                        fontFamily = PlayfairBoldFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PastelLavender
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nggak apa-apa kalau hari ini terasa berat. Tempat amanmu untuk tumpahkan rasa & dengar lagu impian.",
                        fontFamily = PlayfairMediumItalicFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onOpenMusicSearch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelRose.copy(alpha = 0.3f),
                            contentColor = PastelRose
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cari Lagu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Teman AI Card Banner (Clickable header_ai.svg banner directly below are you okay card)
        item(key = "ai_banner", contentType = "ai_card") {
            Card(
                onClick = onOpenAiChat,
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2138)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    AsyncImage(
                        model = "file:///android_asset/header_ai.svg",
                        contentDescription = "Teman Curhat AI",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Section 1: Top 3 Frequently Attached Songs
        item(key = "top_songs_header", contentType = "section_header") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PastelRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top 3 Lagu Sering Di-Attach",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (topSongs.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada lagu favorit yang di-attach. Mulai tulis catatan & attach lagu iTunes-mu!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        itemsIndexed(
            topSongs,
            key = { _, item -> "top_song_${item.trackId}" },
            contentType = { _, _ -> "top_song_card" }
        ) { index, song ->
            val topCardId = "top_${song.trackId}"
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SongCard(
                    trackName = song.trackName,
                    artistName = song.artistName,
                    artworkUrl = song.artworkUrl,
                    previewUrl = song.previewUrl,
                    playerState = playerState,
                    cardId = topCardId,
                    rankIndex = index + 1,
                    attachCount = song.frequency,
                    onPlayClick = {
                        onPlayTrackClick(
                            song.previewUrl,
                            song.trackName,
                            song.artistName,
                            song.artworkUrl,
                            topCardId
                        )
                    }
                )
            }
        }

        // Section 2: Top 4 Recent Curhatan / Notes (M3 Expressive Multi-Browse Carousel)
        item(key = "recent_notes_section", contentType = "carousel_section") {
            Spacer(modifier = Modifier.height(20.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = PastelLavender,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Top 4 Catatan Terbaru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    HomeCarouselSkeletonRow()
                } else if (recentNotes.isEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada catatan. Ceritakan perasaanmu sekarang!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentNotes.take(4), key = { it.id }) { note ->
                            HomeNoteCarouselCard(
                                note = note,
                                onCommentClick = onCommentClick
                            )
                        }
                    }
                }

                // Banner Video Card (Clicks to VideoScreen)
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    onClick = onOpenVideoScreen,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(130.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.banner_video),
                            contentDescription = "Banner Video Curhat",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Overlay gradient & badge for video banner
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.55f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PastelLavender.copy(alpha = 0.9f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = Color(0xFF1E1B28),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Galeri GDrive",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E1B28)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Galeri Foto GDrive 📸",
                                    fontFamily = PlayfairBoldFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Simpan & Berbagi Foto via Google Drive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Banner Kolaborasi Card
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.banner_kolaborasi),
                        contentDescription = "Banner Kolaborasi",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Mascot Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.maskot_2),
                        contentDescription = "Mascot",
                        modifier = Modifier
                            .height(100.dp)
                            .wrapContentHeight(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun HomeNoteCarouselCard(
    note: JournalNote,
    onCommentClick: ((JournalNote) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (note.category) {
        "Asmara & Cinta" -> PastelRose
        "Pendidikan & Sekolah" -> PastelMint
        "Perjalanan Jati Diri" -> PastelLavender
        else -> MaterialTheme.colorScheme.tertiary
    }

    val formattedDate = remember(note.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id", "ID"))
        sdf.format(Date(note.timestamp))
    }

    Card(
        modifier = modifier
            .width(280.dp)
            .height(190.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Mood + Category Tag + Author Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = note.moodEmoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoryColor.copy(alpha = 0.2f),
                        modifier = Modifier.widthIn(max = 120.dp)
                    ) {
                        Text(
                            text = note.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Author Badge Capsule
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 3.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PastelLavender, PastelRose))),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.maskot_1),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(15.dp).clip(CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (note.username.isNotBlank()) note.username else "Remaja Ceria",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Middle: Note Content (Larger text for full, dense aesthetic)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = note.content,
                    fontFamily = PlayfairRegularFamily,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Row: Minimal Song Title + Date
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!note.trackName.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PastelRose.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = PastelRose,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${note.trackName}${if (!note.artistName.isNullOrEmpty()) " • ${note.artistName}" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PastelRose,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )

                    Surface(
                        onClick = { onCommentClick?.invoke(note) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, PastelLavender.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Komentar",
                                tint = PastelLavender,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (note.commentCount > 0) "${note.commentCount}" else "0",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
