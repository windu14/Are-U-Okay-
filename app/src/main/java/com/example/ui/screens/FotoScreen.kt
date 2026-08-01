package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.remote.DrivePhoto
import com.example.data.repository.GoogleDriveRepository
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PlayfairBoldFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Circular Wavy Progress Indicator.
 * EXCLUSIVELY used during uploading photos to Google Drive.
 */
@Composable
fun CircularWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = PastelLavender,
    trackColor: Color = color.copy(alpha = 0.2f),
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 3.dp,
    wavelengths: Int = 10,
    size: Dp = 48.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavy_progress")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val amplitudePx = amplitude.toPx()
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseRadius = (minOf(this.size.width, this.size.height) - strokePx - amplitudePx * 2) / 2f

        if (baseRadius <= 0) return@Canvas

        // 1. Draw track
        val trackPath = Path()
        val numPointsTrack = 100
        for (i in 0..numPointsTrack) {
            val angle = (i.toFloat() / numPointsTrack) * 2 * Math.PI
            val wave = sin(angle * wavelengths + waveOffset.toDouble()) * amplitudePx
            val r = baseRadius + wave
            val x = center.x + (r * cos(angle + Math.toRadians(rotation.toDouble()))).toFloat()
            val y = center.y + (r * sin(angle + Math.toRadians(rotation.toDouble()))).toFloat()
            if (i == 0) trackPath.moveTo(x, y) else trackPath.lineTo(x, y)
        }
        drawPath(
            path = trackPath,
            color = trackColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // 2. Draw active arc segment (approx 270 deg)
        val activePath = Path()
        val numPointsActive = 75
        val arcRad = 1.5 * Math.PI
        val startRad = Math.toRadians(rotation.toDouble())
        for (i in 0..numPointsActive) {
            val fraction = i.toFloat() / numPointsActive
            val angle = fraction * arcRad
            val wave = sin(angle * wavelengths + waveOffset.toDouble()) * amplitudePx
            val r = baseRadius + wave
            val x = center.x + (r * cos(angle + startRad)).toFloat()
            val y = center.y + (r * sin(angle + startRad)).toFloat()
            if (i == 0) activePath.moveTo(x, y) else activePath.lineTo(x, y)
        }
        drawPath(
            path = activePath,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

/**
 * Custom Skeleton Loading Layout used when initially opening/fetching FotoScreen.
 * Replaces the Wavy Progress Indicator with an elegant custom pulsing grid.
 */
@Composable
fun CustomFotoSkeletonLoading(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Custom Loading Status Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = PastelLavender.copy(alpha = alpha),
                modifier = Modifier.size(10.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Memuat galeri foto Google Drive...",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Skeleton Grid 2 Columns x 3 Rows
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) {
            items(6) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Skeleton Image Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = alpha * 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Skeleton Text Placeholders
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.25f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.2f))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom soft pulse loader used inside individual card image placeholders.
 */
@Composable
fun CustomCardImageLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_loader")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PastelLavender.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = PastelLavender.copy(alpha = alpha + 0.3f),
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Custom loader used inside full-screen image viewer preview dialog.
 */
@Composable
fun CustomFullViewerImageLoader(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "viewer_loader")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "viewer_alpha"
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = PastelLavender.copy(alpha = alpha * 0.25f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = PastelLavender.copy(alpha = alpha),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Memuat foto...",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FotoScreen(
    currentUsername: String = "Remaja Ceria",
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val driveRepo = remember { GoogleDriveRepository() }
    val webAppUrl = GoogleDriveRepository.DEFAULT_WEB_APP_URL

    var photos by remember { mutableStateOf<List<DrivePhoto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var selectedPhotoForViewer by remember { mutableStateOf<DrivePhoto?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadPhotos() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = driveRepo.fetchPhotosFromGDrive(webAppUrl)
            result.onSuccess { list ->
                photos = list
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Gagal memuat foto dari Google Drive"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPhotos()
    }

    // Photo Picker Activity Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                try {
                    val safeUsername = currentUsername.ifBlank { "User" }
                    val (filename, mimeType, base64) = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        if (originalBitmap == null) {
                            throw Exception("Gagal membaca file gambar")
                        }

                        // Compress and scale down bitmap for fast upload
                        val maxDim = 1280
                        val width = originalBitmap.width
                        val height = originalBitmap.height
                        val scaledBitmap = if (width > maxDim || height > maxDim) {
                            val ratio = width.toFloat() / height.toFloat()
                            val newW = if (width > height) maxDim else (maxDim * ratio).toInt()
                            val newH = if (height >= width) maxDim else (maxDim / ratio).toInt()
                            Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
                        } else {
                            originalBitmap
                        }

                        val baos = ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                        val bytes = baos.toByteArray()
                        val b64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val cleanUploader = safeUsername.replace(" ", "_")
                        val fname = "curhat_${cleanUploader}_${System.currentTimeMillis()}.jpg"
                        Triple(fname, "image/jpeg", b64Str)
                    }

                    val result = driveRepo.uploadPhotoToGDrive(
                        webAppUrl = webAppUrl,
                        filename = filename,
                        mimeType = mimeType,
                        base64Data = base64,
                        uploader = safeUsername
                    )
                    result.onSuccess { uploadedPhoto ->
                        Toast.makeText(context, "✨ Foto berhasil di-upload ke GDrive!", Toast.LENGTH_SHORT).show()
                        photos = listOf(uploadedPhoto) + photos
                    }.onFailure { err ->
                        Toast.makeText(context, "❌ Gagal Upload: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = "Galeri Foto GDrive",
                                fontFamily = PlayfairBoldFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Tersimpan di Google Drive Cloud Admin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { loadPhotos() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Galeri",
                            tint = PastelLavender
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                containerColor = PastelLavender,
                contentColor = Color(0xFF1E1B28),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Foto")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Foto", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Initial Loading State: Uses Custom Skeleton Pulse Grid instead of Wavy Progress
                isLoading && photos.isEmpty() -> {
                    CustomFotoSkeletonLoading()
                }

                errorMessage != null && photos.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = errorMessage ?: "Gagal terhubung ke Google Drive",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { loadPhotos() },
                                colors = ButtonDefaults.buttonColors(containerColor = PastelLavender),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFF1E1B28),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Coba Lagi",
                                    color = Color(0xFF1E1B28),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                photos.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PastelLavender.copy(alpha = 0.2f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = PastelLavender,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "Belum Ada Foto di Google Drive",
                                    fontFamily = PlayfairBoldFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Unggah foto pertama kamu! Semua foto tersimpan otomatis di folder Google Drive.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PastelLavender,
                                        contentColor = Color(0xFF1E1B28)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unggah Foto", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            PhotoCardItem(
                                photo = photo,
                                onClick = { selectedPhotoForViewer = photo }
                            )
                        }
                    }
                }
            }

            // Material 3 Expressive Wavy Progress Indicator is strictly reserved for Uploading Overlay
            AnimatedVisibility(
                visible = isUploading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularWavyProgressIndicator(
                                    color = PastelLavender,
                                    size = 56.dp,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "Meng-upload foto ke GDrive...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mohon tunggu sejenak",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-Screen Image Preview Viewer Dialog
    if (selectedPhotoForViewer != null) {
        val photo = selectedPhotoForViewer!!
        Dialog(
            onDismissRequest = { selectedPhotoForViewer = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto oleh ${photo.uploader}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    loading = {
                        CustomFullViewerImageLoader()
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ImageNotSupported,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                )

                // Top Close Bar (Clean User Account & Date Time metadata without raw filename)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = PastelLavender,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Oleh: ${photo.uploader}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val dateStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date(photo.createdTime))
                        Text(
                            text = dateStr,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = { selectedPhotoForViewer = null },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCardItem(
    photo: DrivePhoto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto oleh ${photo.uploader}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        CustomCardImageLoader()
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ImageNotSupported,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                )

                // 'diposting' Badge for photos posted from curhat
                if (photo.name.contains("curhat", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 10.dp),
                        color = PastelLavender,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "diposting",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1B28),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Clean Card Metadata (User Account + Date Time, NO raw filename)
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = PastelLavender,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Oleh: ${photo.uploader}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(photo.createdTime))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}
