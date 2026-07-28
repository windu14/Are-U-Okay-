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

sealed class UpdateCheckResult {
    data class Success(val updateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val latestVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class GitHubUpdateManager(
    private val repoOwner: String = "windu14",
    private val repoName: String = "Are-U-Okay-"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdatesDetailed(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            // First try GET /releases to find the latest release with an APK asset
            val listUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases"
            val request = Request.Builder()
                .url(listUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AreYouOkayApp")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 403) {
                    return@withContext UpdateCheckResult.Error("Batas request GitHub API terlampaui. Coba lagi beberapa saat lagi.")
                }

                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullByBlank()) {
                        val releasesArray = org.json.JSONArray(bodyStr)
                        var latestReleaseWithApk: JSONObject? = null
                        var apkUrl = ""

                        for (i in 0 until releasesArray.length()) {
                            val releaseObj = releasesArray.getJSONObject(i)
                            if (releaseObj.optBoolean("draft", false)) continue

                            val assets = releaseObj.optJSONArray("assets") ?: continue
                            for (j in 0 until assets.length()) {
                                val asset = assets.getJSONObject(j)
                                val assetName = asset.optString("name", "")
                                if (assetName.endsWith(".apk", ignoreCase = true)) {
                                    apkUrl = asset.optString("browser_download_url", "")
                                    latestReleaseWithApk = releaseObj
                                    break
                                }
                            }

                            if (latestReleaseWithApk != null) break
                        }

                        if (latestReleaseWithApk != null && apkUrl.isNotBlank()) {
                            val tagName = latestReleaseWithApk.optString("tag_name", "").trim()
                            val releaseTitle = latestReleaseWithApk.optString("name", tagName)
                            val releaseNotes = latestReleaseWithApk.optString("body", "Pembaruan versi baru tersedia.")

                            val cleanRemote = cleanVersion(tagName)
                            val cleanCurrent = cleanVersion(BuildConfig.VERSION_NAME)
                            val isNewer = isVersionNewer(cleanRemote, cleanCurrent)

                            Log.d("UpdateManager", "Current: $cleanCurrent, Remote: $cleanRemote, IsNewer: $isNewer")

                            val updateInfo = AppUpdateInfo(
                                latestVersionName = tagName,
                                releaseTitle = releaseTitle,
                                releaseNotes = releaseNotes,
                                downloadUrl = apkUrl,
                                isUpdateAvailable = isNewer
                            )

                            return@withContext if (isNewer) {
                                UpdateCheckResult.Success(updateInfo)
                            } else {
                                UpdateCheckResult.UpToDate(cleanCurrent, cleanRemote)
                            }
                        } else if (releasesArray.length() > 0) {
                            val firstRelease = releasesArray.getJSONObject(0)
                            val tagName = firstRelease.optString("tag_name", "terbaru")
                            return@withContext UpdateCheckResult.Error("Release $tagName ditemukan, tetapi file APK belum diunggah.")
                        }
                    }
                }
            }

            // Fallback to /releases/latest if /releases failed or returned empty
            val latestUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val latestRequest = Request.Builder()
                .url(latestUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "AreYouOkayApp")
                .build()

            client.newCall(latestRequest).execute().use { response ->
                if (response.code == 404) {
                    return@withContext UpdateCheckResult.Error("Repository $repoOwner/$repoName atau release belum ada di GitHub.")
                }
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("Gagal menghubungi GitHub API (HTTP ${response.code}).")
                }

                val bodyStr = response.body?.string()
                if (bodyStr.isNullOrBlank()) {
                    return@withContext UpdateCheckResult.Error("Respon dari GitHub kosong.")
                }

                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "").trim()
                val releaseTitle = json.optString("name", tagName)
                val releaseNotes = json.optString("body", "Pembaruan versi baru tersedia.")

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

                if (downloadUrl.isBlank()) {
                    return@withContext UpdateCheckResult.Error("Release $tagName ditemukan, tetapi file APK belum diunggah.")
                }

                val cleanRemote = cleanVersion(tagName)
                val cleanCurrent = cleanVersion(BuildConfig.VERSION_NAME)
                val isNewer = isVersionNewer(cleanRemote, cleanCurrent)

                val updateInfo = AppUpdateInfo(
                    latestVersionName = tagName,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    isUpdateAvailable = isNewer
                )

                return@withContext if (isNewer) {
                    UpdateCheckResult.Success(updateInfo)
                } else {
                    UpdateCheckResult.UpToDate(cleanCurrent, cleanRemote)
                }
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Error checking update", e)
            return@withContext UpdateCheckResult.Error(e.localizedMessage ?: "Gagal terhubung ke internet.")
        }
    }

    private fun String?.isNullByBlank(): Boolean = this == null || this.isBlank()

    suspend fun checkForUpdates(): AppUpdateInfo? {
        return when (val result = checkForUpdatesDetailed()) {
            is UpdateCheckResult.Success -> result.updateInfo
            else -> null
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
