package com.example.data.repository

import com.example.data.remote.DrivePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoogleDriveRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        const val DEFAULT_WEB_APP_URL = "https://script.google.com/macros/s/AKfycbzbn8KpFcTRHMuh3Q-gA5QPEPekyQ-G3BBCMUraH5Fz-8ozKpn2qrmOkdkFlUc1WZRcZA/exec"
    }

    suspend fun fetchPhotosFromGDrive(webAppUrl: String): Result<List<DrivePhoto>> = withContext(Dispatchers.IO) {
        val targetUrl = if (webAppUrl.isNotBlank()) webAppUrl else DEFAULT_WEB_APP_URL

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error: ${response.code} ${response.message}"))
                }

                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                val status = rootJson.optString("status", "error")

                if (status == "success") {
                    val dataArr = rootJson.optJSONArray("data")
                    val list = mutableListOf<DrivePhoto>()

                    if (dataArr != null) {
                        for (i in 0 until dataArr.length()) {
                            val obj = dataArr.getJSONObject(i)
                            val id = obj.optString("id")
                            val name = obj.optString("name", "Gambar $i")
                            val mimeType = obj.optString("mimeType", "image/jpeg")
                            val url = obj.optString("url")
                            val downloadUrl = obj.optString("downloadUrl")
                            val createdTime = obj.optLong("createdTime", System.currentTimeMillis())

                            if (url.isNotBlank() || id.isNotBlank()) {
                                val uploaderName = parseUploader(name, obj.optString("uploader", ""))
                                list.add(
                                    DrivePhoto(
                                        id = id,
                                        name = name,
                                        uploader = uploaderName,
                                        mimeType = mimeType,
                                        url = if (url.isNotBlank()) url else "https://lh3.googleusercontent.com/d/$id",
                                        downloadUrl = downloadUrl,
                                        createdTime = createdTime
                                    )
                                )
                            }
                        }
                    }

                    Result.success(list)
                } else {
                    val msg = rootJson.optString("message", "Gagal mengambil foto dari GDrive")
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun parseUploader(name: String, jsonUploader: String): String {
        if (jsonUploader.isNotBlank()) return jsonUploader
        if (name.startsWith("curhat_")) {
            val parts = name.removePrefix("curhat_").split("_")
            if (parts.size >= 2) {
                val candidate = parts.dropLast(1).joinToString(" ").replace(".jpg", "", true).replace(".png", "", true)
                if (candidate.isNotBlank() && !candidate.all { it.isDigit() }) {
                    return candidate
                }
            }
        }
        return "Anggota Komunitas"
    }

    suspend fun uploadPhotoToGDrive(
        webAppUrl: String,
        filename: String,
        mimeType: String,
        base64Data: String,
        uploader: String = "Pengguna Are You Okay"
    ): Result<DrivePhoto> = withContext(Dispatchers.IO) {
        val targetUrl = if (webAppUrl.isNotBlank()) webAppUrl else DEFAULT_WEB_APP_URL

        try {
            val payload = JSONObject().apply {
                put("filename", filename)
                put("mimeType", mimeType)
                put("base64Data", base64Data)
                put("uploader", uploader)
            }

            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(targetUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error ${response.code}: Gagal upload ke GDrive"))
                }

                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                val status = rootJson.optString("status", "error")

                if (status == "success") {
                    val fileId = rootJson.optString("fileId")
                    val name = rootJson.optString("name", filename)
                    val url = rootJson.optString("url", "https://lh3.googleusercontent.com/d/$fileId")

                    val photo = DrivePhoto(
                        id = fileId,
                        name = name,
                        uploader = uploader,
                        mimeType = mimeType,
                        url = url,
                        createdTime = System.currentTimeMillis()
                    )
                    Result.success(photo)

                } else {
                    val msg = rootJson.optString("message", "Gagal upload gambar ke Google Drive")
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
