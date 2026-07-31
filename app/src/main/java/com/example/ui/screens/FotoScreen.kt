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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.remote.DrivePhoto
import com.example.data.repository.GoogleDriveRepository
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelRose
import com.example.ui.theme.PlayfairBoldFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FotoScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("gdrive_prefs", Context.MODE_PRIVATE) }
    val driveRepo = remember { GoogleDriveRepository() }

    val defaultUrl = "https://script.google.com/macros/s/AKfycbzbn8KpFcTRHMuh3Q-gA5QPEPekyQ-G3BBCMUraH5Fz-8ozKpn2qrmOkdkFlUc1WZRcZA/exec"
    var webAppUrl by remember {
        val saved = prefs.getString("webapp_url", "")
        mutableStateOf(if (saved.isNullOrBlank()) defaultUrl else saved)
    }
    var inputUrl by remember { mutableStateOf(webAppUrl) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var showInstructionsDialog by remember { mutableStateOf(false) }

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
                // Fallback to sample photos if network error
                if (photos.isEmpty()) {
                    photos = driveRepo.defaultSamplePhotos
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(webAppUrl) {
        loadPhotos()
    }

    // Photo Picker Activity Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (webAppUrl.isBlank()) {
                Toast.makeText(context, "Atur URL Google Apps Script Web App terlebih dahulu!", Toast.LENGTH_LONG).show()
                showSetupDialog = true
                return@rememberLauncherForActivityResult
            }

            coroutineScope.launch {
                isUploading = true
                try {
                    val (filename, mimeType, base64) = withContext(Dispatchers.IO) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val originalBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()

                        if (originalBitmap == null) {
                            throw Exception("Gagal membaca file gambar")
                        }

                        // Scale down bitmap for fast upload
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
                        val fname = "curhat_${System.currentTimeMillis()}.jpg"
                        Triple(fname, "image/jpeg", b64Str)
                    }

                    val result = driveRepo.uploadPhotoToGDrive(webAppUrl, filename, mimeType, base64)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Galeri Foto GDrive 📸",
                                fontFamily = PlayfairBoldFamily,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PastelRose.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    text = "Eksperimental",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelRose,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (webAppUrl.isNotBlank()) "Terhubung ke Google Drive Folder Admin" else "Sampel Foto (Klik ⚙️ untuk set Apps Script URL)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PastelLavender)
                    }
                    IconButton(onClick = { showSetupDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan Apps Script", tint = MaterialTheme.colorScheme.onSurface)
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
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Foto")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Foto", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Warning / Setup info banner if WebAppUrl is empty
                if (webAppUrl.isBlank()) {
                    Surface(
                        color = PastelLavender.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = PastelLavender,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Drive Web App Belum Terhubung",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Menampilkan foto demo. Hubungkan Google Apps Script Web App untuk upload & sinkronisasi otomatis ke GDrive.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { showSetupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PastelLavender),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Atur", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1B28))
                            }
                        }
                    }
                }

                if (errorMessage != null && photos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { loadPhotos() }, colors = ButtonDefaults.buttonColors(containerColor = PastelLavender)) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                } else if (isLoading && photos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PastelLavender)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("⏳ Memuat foto dari Google Drive...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
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

            // Upload Overlay Loader
            if (isUploading) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PastelLavender)
                                Spacer(modifier = Modifier.height(16.dp))
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
                    .background(Color.Black.copy(alpha = 0.92f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Top Close Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = photo.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(photo.createdTime))
                        Text(
                            text = dateStr,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }

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

    // Setup Dialog (Enter Google Apps Script Web App URL)
    if (showSetupDialog) {
        AlertDialog(
            onDismissRequest = { showSetupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PastelLavender)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pengaturan GDrive Web App", fontFamily = PlayfairBoldFamily, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Masukkan URL Web App Google Apps Script yang sudah kamu deploy dari akun Google-mu:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        placeholder = { Text("https://script.google.com/macros/s/AKfycbx.../exec", fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PastelLavender),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { showInstructionsDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PastelLavender.copy(alpha = 0.2f),
                            contentColor = PastelLavender
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📄 Panduan & Code Script Google Apps Script", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        webAppUrl = inputUrl.trim()
                        prefs.edit().putString("webapp_url", webAppUrl).apply()
                        showSetupDialog = false
                        Toast.makeText(context, "URL Apps Script berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelLavender, contentColor = Color(0xFF1E1B28)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan URL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSetupDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tutup")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Comprehensive Manual Instructions Dialog
    if (showInstructionsDialog) {
        val scriptCode = """
const FOLDER_ID = "1Xhbe21cVwtUBSqNrm_LTfJHzPjvT7Zx7";

function doGet(e) {
  try {
    const folder = DriveApp.getFolderById(FOLDER_ID);
    const files = folder.getFiles();
    const result = [];
    
    while (files.hasNext()) {
      const file = files.next();
      const mime = file.getMimeType();
      if (mime.indexOf("image/") === 0 || mime === "application/octet-stream") {
        const fileId = file.getId();
        result.push({
          id: fileId,
          name: file.getName(),
          mimeType: mime,
          url: "https://lh3.googleusercontent.com/d/" + fileId,
          downloadUrl: "https://drive.google.com/uc?export=view&id=" + fileId,
          createdTime: file.getDateCreated().getTime()
        });
      }
    }
    result.sort((a, b) => b.createdTime - a.createdTime);
    return ContentService.createTextOutput(JSON.stringify({ status: "success", data: result })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: err.toString() })).setMimeType(ContentService.MimeType.JSON);
  }
}

function doPost(e) {
  try {
    const contents = JSON.parse(e.postData.contents);
    const filename = contents.filename || ("curhat_" + Date.now() + ".jpg");
    const mimeType = contents.mimeType || "image/jpeg";
    const bytes = Utilities.base64Decode(contents.base64Data);
    const blob = Utilities.newBlob(bytes, mimeType, filename);
    const folder = DriveApp.getFolderById(FOLDER_ID);
    const file = folder.createFile(blob);
    file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    const fileId = file.getId();
    return ContentService.createTextOutput(JSON.stringify({ status: "success", fileId: fileId, url: "https://lh3.googleusercontent.com/d/" + fileId, name: filename })).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: err.toString() })).setMimeType(ContentService.MimeType.JSON);
  }
}
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = {
                Text("Langkah-Langkah Setup Google Apps Script", fontFamily = PlayfairBoldFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. Buka script.google.com & buat 'New Project'", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("2. Hapus isi Code.gs, lalu paste kode JavaScript berikut:", fontSize = 12.sp)
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scriptCode,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Text("3. Klik 'Deploy' -> 'New Deployment'", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("4. Pilih jenis 'Web App':", fontSize = 12.sp)
                    Text("   • Execute as: Me (akun Google Anda)", fontSize = 11.sp, color = PastelLavender)
                    Text("   • Who has access: Anyone (Siapa Saja)", fontSize = 11.sp, color = PastelRose)
                    Text("5. Klik Deploy, berikan izin (Authorize Access).", fontSize = 12.sp)
                    Text("6. Copy Web App URL (berakhiran /exec) & paste di aplikasi!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PastelMint)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInstructionsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelLavender, contentColor = Color(0xFF1E1B28)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Saya Mengerti", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
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
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photo.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = photo.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = photo.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(photo.createdTime))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
