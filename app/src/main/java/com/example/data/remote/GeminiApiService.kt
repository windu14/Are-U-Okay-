package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiMessage(
    val role: String, // "user" or "model"
    val text: String
)

class GeminiApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val systemInstructionText = """
        Kamu adalah 'Teman Curhat AI' (bernama 'Teman AI'), seorang sahabat empatik, ramah, berpikiran terbuka, dan penuh perhatian khusus untuk remaja dan anak muda (Gen Z).
        Niche utama kamu adalah tentang percintaan (romance, galau, crush, patah hati), masalah hidup, kesehatan emosional ringan, pendidikan/sekolah, dan pencarian jati diri.
        Gunakan bahasa Indonesia yang santai, empatik, hangat, tidak menggurui, serta selipkan emoji yang relevan.
        Jika pengguna meminta saran lagu atau curhat emosional, berikan juga 1 rekomendasi judul lagu & artis yang cocok di akhir jawabanmu.
        Jawablah langsung pertanyaan atau curhatan pengguna sebagai obrolan obrolan biasa. Jangan menambahkan catatan atau curhatan secara otomatis ke database aplikasi, karena aplikasi sudah memiliki tombol khusus 'Simpan ke Catatan' untuk pengguna.
    """.trimIndent()

    private fun getApiKeyFromBuildConfig(): String {
        return try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun sendMessage(
        chatHistory: List<GeminiMessage>,
        userPrompt: String,
        apiKeyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = apiKeyOverride?.ifBlank { null }
                ?: getApiKeyFromBuildConfig()

            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalArgumentException("API Key Gemini belum terkonfigurasi. Masukkan API Key aktif di Pengaturan/Secrets.")
                )
            }

            // Build json request payload
            val rootJson = JSONObject()

            // System instruction
            val sysInstructionObj = JSONObject()
            val sysPartsArr = JSONArray()
            sysPartsArr.put(JSONObject().put("text", systemInstructionText))
            sysInstructionObj.put("parts", sysPartsArr)
            rootJson.put("system_instruction", sysInstructionObj)

            // Contents array
            val contentsArr = JSONArray()

            // Add previous history
            for (msg in chatHistory) {
                val msgObj = JSONObject()
                msgObj.put("role", msg.role)
                val partsArr = JSONArray()
                partsArr.put(JSONObject().put("text", msg.text))
                msgObj.put("parts", partsArr)
                contentsArr.put(msgObj)
            }

            // Add new user prompt
            val newMsgObj = JSONObject()
            newMsgObj.put("role", "user")
            val newPartsArr = JSONArray()
            newPartsArr.put(JSONObject().put("text", userPrompt))
            newMsgObj.put("parts", newPartsArr)
            contentsArr.put(newMsgObj)

            rootJson.put("contents", contentsArr)

            // Safety settings or generation config optional
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            rootJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = rootJson.toString().toRequestBody(mediaType)

            // We can try models: gemini-2.5-flash -> gemini-1.5-flash fallback
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Try fallback model if 2.5-flash fails
                val fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val fallbackReq = Request.Builder().url(fallbackUrl).post(body).build()
                val fallbackResp = client.newCall(fallbackReq).execute()
                val fallbackStr = fallbackResp.body?.string() ?: ""

                if (!fallbackResp.isSuccessful) {
                    val errJson = try { JSONObject(fallbackStr) } catch (e: Exception) { null }
                    val msg = errJson?.optJSONObject("error")?.optString("message") ?: "Gagal menghubungi Gemini AI (${response.code})"
                    return@withContext Result.failure(Exception(msg))
                }
                val text = extractTextFromGeminiResponse(fallbackStr)
                return@withContext Result.success(text)
            }

            val responseText = extractTextFromGeminiResponse(responseStr)
            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTextFromGeminiResponse(jsonStr: String): String {
        val root = JSONObject(jsonStr)
        val candidates = root.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content")
            if (content != null) {
                val parts = content.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        sb.append(parts.getJSONObject(i).optString("text", ""))
                    }
                    return sb.toString()
                }
            }
        }
        return "Maaf, aku belum bisa memberikan tanggapan saat ini. Coba tanyakan lagi ya!"
    }
}
