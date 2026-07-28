package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.example.audio.AudioPlayerState
import com.example.data.remote.UserProfile
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GlobalCurhatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TentangScreen
import com.example.ui.screens.WriteCurhatScreen
import com.example.ui.theme.AreYouOkayTheme
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelRose
import com.example.ui.viewmodel.JournalViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: JournalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (ignored: Exception) {
            android.util.Log.e("MainActivity", "FirebaseApp init exception", ignored)
        }
        enableEdgeToEdge()
        setContent {
            AreYouOkayTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: JournalViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    if (currentUser == null) {
        AuthScreen(
            isLoading = authLoading,
            errorMessage = authError,
            onLogin = { emailOrUsername, pass ->
                viewModel.login(emailOrUsername, pass) {}
            },
            onSignUp = { username, email, pass ->
                viewModel.signUp(username, email, pass) {}
            },
            onClearError = { viewModel.clearAuthError() }
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Global Curhat, 2: Tentang, 3: Teman AI, 4: Write Curhat, 5: Profile Screen
    var isScreenLoading by remember { mutableStateOf(false) }

    val onSelectTab: (Int) -> Unit = { target ->
        if (selectedTab != target && !isScreenLoading) {
            selectedTab = target
            isScreenLoading = true
        }
    }

    LaunchedEffect(isScreenLoading) {
        if (isScreenLoading) {
            delay(1500L)
            isScreenLoading = false
        }
    }

    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val recentNotes by viewModel.recentNotes.collectAsStateWithLifecycle()
    val topSongs by viewModel.topSongs.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    val showAddNoteDialog by viewModel.showAddNoteDialog.collectAsStateWithLifecycle()
    val showSearchMusicSheet by viewModel.showSearchMusicSheet.collectAsStateWithLifecycle()
    val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()

    val aiMessages by viewModel.aiMessages.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val aiErrorMessage by viewModel.aiErrorMessage.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Persistent Player Bar when audio is loaded or playing
                if (playerState.currentPreviewUrl != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        onPlayPauseClick = {
                            if (playerState.isPlaying) {
                                viewModel.audioPlayer.pause()
                            } else {
                                viewModel.audioPlayer.resume()
                            }
                        },
                        onCloseClick = {
                            viewModel.audioPlayer.stop()
                        }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF261833),
                            selectedTextColor = PastelLavender,
                            indicatorColor = PastelLavender,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        icon = { Icon(Icons.Default.Forum, contentDescription = "Global Curhat") },
                        label = { Text("Global Curhat", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF261833),
                            selectedTextColor = PastelLavender,
                            indicatorColor = PastelLavender,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2 || selectedTab == 5,
                        onClick = { onSelectTab(2) },
                        icon = { Icon(Icons.Default.Info, contentDescription = "Tentang") },
                        label = { Text("Tentang", fontWeight = if (selectedTab == 2 || selectedTab == 5) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF261833),
                            selectedTextColor = PastelLavender,
                            indicatorColor = PastelLavender,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    topSongs = topSongs,
                    recentNotes = recentNotes,
                    playerState = playerState,
                    onPlayTrackClick = { url, title, artist, art, cardId ->
                        viewModel.playTrackPreview(url, title, artist, art, cardId)
                    },
                    onDeleteNoteClick = { id -> viewModel.deleteNote(id) },
                    onOpenAddNote = { onSelectTab(4) },
                    onOpenMusicSearch = { viewModel.openSearchMusicSheet() },
                    onOpenAiChat = { onSelectTab(3) }
                )

                1 -> GlobalCurhatScreen(
                    notes = filteredNotes,
                    selectedCategory = selectedCategory,
                    playerState = playerState,
                    onCategorySelect = { cat -> viewModel.setCategoryFilter(cat) },
                    onPlayTrackClick = { url, title, artist, art, cardId ->
                        viewModel.playTrackPreview(url, title, artist, art, cardId)
                    },
                    onDeleteNoteClick = { id -> viewModel.deleteNote(id) },
                    onOpenAddNote = { onSelectTab(4) },
                    onOpenMusicSearch = { viewModel.openSearchMusicSheet() }
                )

                2 -> TentangScreen(
                    onOpenProfile = { onSelectTab(5) }
                )

                3 -> AiChatScreen(
                    messages = aiMessages,
                    isThinking = isAiThinking,
                    errorMessage = aiErrorMessage,
                    onSendMessage = { prompt -> viewModel.sendAiMessage(prompt) },
                    onClearChat = { viewModel.clearAiChat() },
                    onSaveToNote = { content, category, moodEmoji ->
                        viewModel.saveAiResponseToJournal(content, category, moodEmoji)
                    },
                    onOpenMusicSearch = { viewModel.openSearchMusicSheet() },
                    onBackClick = { onSelectTab(0) },
                    onSaveApiKey = { key -> viewModel.saveCustomApiKey(key) }
                )

                4 -> WriteCurhatScreen(
                    selectedTrack = selectedTrack,
                    onSaveNote = { content, category, moodEmoji ->
                        viewModel.addNote(content, category, moodEmoji)
                    },
                    onOpenMusicSearch = { viewModel.openSearchMusicSheet() },
                    onRemoveTrack = { viewModel.selectTrackForNote(null) },
                    onBackClick = { onSelectTab(0) }
                )

                5 -> {
                    val currentUid = currentUser?.uid ?: ""
                    val userNoteCount = allNotes.count { note ->
                        currentUid.isEmpty() || note.userId == currentUid || note.userId.isEmpty() || note.userId.startsWith("anon_")
                    }
                    val totalCount = maxOf(userProfile?.totalCurhat ?: 0, userNoteCount)
                    val effectiveProfile = userProfile?.copy(totalCurhat = totalCount) ?: UserProfile(
                        uid = currentUid,
                        username = currentUser?.displayName ?: "Pengguna",
                        email = currentUser?.email ?: "",
                        totalCurhat = totalCount
                    )
                    ProfileScreen(
                        userProfile = effectiveProfile,
                        totalCurhatCount = totalCount,
                        onLogout = { viewModel.logout() },
                        onBackClick = { onSelectTab(2) }
                    )
                }
            }

            // Screen Transition Loading Overlay (1.5 Seconds)
            if (isScreenLoading) {
                ScreenTransitionLoadingOverlay()
            }

            // Music Search Sheet
            if (showSearchMusicSheet) {
                com.example.ui.components.SearchMusicSheet(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    isSearching = isSearching,
                    searchError = searchError,
                    playerState = playerState,
                    onQueryChange = { q -> viewModel.updateSearchQuery(q) },
                    onPlayTrackClick = { url, title, artist, art, cardId ->
                        viewModel.playTrackPreview(url, title, artist, art, cardId)
                    },
                    onSelectTrack = { track ->
                        viewModel.selectTrackForNote(track)
                        viewModel.dismissSearchMusicSheet()
                        onSelectTab(4)
                    },
                    onDismiss = { viewModel.dismissSearchMusicSheet() }
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    playerState: AudioPlayerState,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2B243B),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, PastelLavender.copy(alpha = 0.4f))
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (!playerState.artworkUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = playerState.artworkUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = PastelLavender,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playerState.trackTitle ?: "Preview Song",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = playerState.artistName ?: "iTunes Music",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PastelLavender)
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF261833),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Pause",
                                tint = Color(0xFF261833),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onCloseClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Audio Progress Bar
            LinearProgressIndicator(
                progress = { playerState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = PastelLavender,
                trackColor = Color(0xFF1E1B28)
            )
        }
    }
}

@Composable
fun ScreenTransitionLoadingOverlay() {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "screen_transition_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "file:///android_asset/loading.svg",
            contentDescription = "Loading...",
            imageLoader = imageLoader,
            modifier = Modifier
                .size(180.dp)
                .rotate(angle)
        )
    }
}
