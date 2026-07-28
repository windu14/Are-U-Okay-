package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class GitHubUpdateManager(
    private val repoOwner: String = "windu14",
    private val repoName: String = "Are-U-Okay-"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AreYouOkayApp")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("UpdateManager", "GitHub API failed: ${response.code}")
                    return@withContext null
                }

                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)

                val tagName = json.optString("tag_name", "").trim()
                val releaseTitle = json.optString("name", tagName)
                val releaseNotes = json.optString("body", "Pembaruan versi baru tersedia.")

                // Find APK asset
                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                if (downloadUrl.isBlank() || tagName.isBlank()) {
                    return@withContext null
                }

                val cleanRemote = cleanVersion(tagName)
                val cleanCurrent = cleanVersion(BuildConfig.VERSION_NAME)
                val isNewer = isVersionNewer(cleanRemote, cleanCurrent)

                Log.d("UpdateManager", "Current: $cleanCurrent, Remote: $cleanRemote, IsNewer: $isNewer")

                return@withContext AppUpdateInfo(
                    latestVersionName = tagName,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    isUpdateAvailable = isNewer
                )
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error checking update", e)
            null
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "AreYouOkayApp")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onError("Gagal mengunduh file update (${response.code})")
                    }
                    return@withContext
                }

                val responseBody = response.body
                if (responseBody == null) {
                    withContext(Dispatchers.Main) {
                        onError("Gagal menerima file update")
                    }
                    return@withContext
                }

                val totalBytes = responseBody.contentLength()
                val destFile = File(context.getExternalFilesDir(null), "update_app.apk")
                if (destFile.exists()) {
                    destFile.delete()
                }

                val inputStream = responseBody.byteStream()
                val outputStream = FileOutputStream(destFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    onProgress(1.0f)
                    installApk(context, destFile)
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Download error", e)
            withContext(Dispatchers.Main) {
                onError("Gagal mengunduh: ${e.localizedMessage}")
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) return

            val authority = "${context.packageName}.provider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Izinkan aplikasi menginstall update terlebih dahulu",
                        Toast.LENGTH_LONG
                    ).show()

                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Gagal menginstall APK", e)
            Toast.makeText(context, "Gagal membuka installer: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cleanVersion(version: String): String {
        return version.trim().lowercase().removePrefix("v").replace("-release", "")
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote == current) return false
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
