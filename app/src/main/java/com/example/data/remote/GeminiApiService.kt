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

    private val defaultGroqApiKey = "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"

    private val systemInstructionText = """
        Kamu adalah 'Teman Curhat AI' (bernama 'Teman AI'), seorang sahabat empatik, ramah, berpikiran terbuka, dan penuh perhatian khusus untuk remaja dan anak muda (Gen Z).
        Niche utama kamu adalah tentang percintaan (romance, galau, crush, patah hati), masalah hidup, kesehatan emosional ringan, pendidikan/sekolah, dan pencarian jati diri.
        Gunakan bahasa Indonesia yang santai, empatik, hangat, tidak menggurui, serta selipkan emoji yang relevan.
        Jika pengguna meminta saran lagu atau curhat emosional, berikan juga 1 rekomendasi judul lagu & artis yang cocok di akhir jawabanmu.
        Jawablah langsung pertanyaan atau curhatan pengguna sebagai obrolan biasa. Jangan menambahkan catatan atau curhatan secara otomatis ke database aplikasi, karena aplikasi sudah memiliki tombol khusus 'Simpan ke Catatan' untuk pengguna.
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
            val activeKey = apiKeyOverride?.ifBlank { null }
                ?: getApiKeyFromBuildConfig().ifBlank { defaultGroqApiKey }

            // Check if key is Groq API key (starts with "gsk_" or using defaultGroqApiKey)
            if (activeKey.startsWith("gsk_") || activeKey == defaultGroqApiKey) {
                return@withContext sendGroqMessage(activeKey, chatHistory, userPrompt)
            } else {
                // Otherwise try Gemini API first, with Groq as fallback
                val geminiResult = sendGeminiMessage(activeKey, chatHistory, userPrompt)
                if (geminiResult.isSuccess) {
                    return@withContext geminiResult
                }
                // Fallback to Groq default key if Gemini fails or quota exceeded
                return@withContext sendGroqMessage(defaultGroqApiKey, chatHistory, userPrompt)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendGroqMessage(
        apiKey: String,
        chatHistory: List<GeminiMessage>,
        userPrompt: String
    ): Result<String> {
        val modelsToTry = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
        val mediaType = "application/json; charset=utf-8".toMediaType()

        for (model in modelsToTry) {
            try {
                val rootJson = JSONObject()
                rootJson.put("model", model)
                rootJson.put("temperature", 0.7)

                val messagesArr = JSONArray()
                
                // System instruction
                val sysObj = JSONObject()
                sysObj.put("role", "system")
                sysObj.put("content", systemInstructionText)
                messagesArr.put(sysObj)

                // History
                for (msg in chatHistory) {
                    val mObj = JSONObject()
                    mObj.put("role", if (msg.role == "model") "assistant" else "user")
                    mObj.put("content", msg.text)
                    messagesArr.put(mObj)
                }

                // New User Prompt
                val userObj = JSONObject()
                userObj.put("role", "user")
                userObj.put("content", userPrompt)
                messagesArr.put(userObj)

                rootJson.put("messages", messagesArr)

                val body = rootJson.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseStr = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val root = JSONObject(responseStr)
                    val choices = root.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val messageObj = firstChoice.optJSONObject("message")
                        val content = messageObj?.optString("content") ?: ""
                        if (content.isNotBlank()) {
                            return Result.success(content)
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next model if current fails
                continue
            }
        }

        return Result.failure(Exception("Gagal menghubungi AI Server. Silakan coba lagi sebentar lagi."))
    }

    private fun sendGeminiMessage(
        apiKey: String,
        chatHistory: List<GeminiMessage>,
        userPrompt: String
    ): Result<String> {
        val rootJson = JSONObject()

        val sysInstructionObj = JSONObject()
        val sysPartsArr = JSONArray()
        sysPartsArr.put(JSONObject().put("text", systemInstructionText))
        sysInstructionObj.put("parts", sysPartsArr)
        rootJson.put("system_instruction", sysInstructionObj)

        val contentsArr = JSONArray()
        for (msg in chatHistory) {
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            val partsArr = JSONArray()
            partsArr.put(JSONObject().put("text", msg.text))
            msgObj.put("parts", partsArr)
            contentsArr.put(msgObj)
        }

        val newMsgObj = JSONObject()
        newMsgObj.put("role", "user")
        val newPartsArr = JSONArray()
        newPartsArr.put(JSONObject().put("text", userPrompt))
        newMsgObj.put("parts", newPartsArr)
        contentsArr.put(newMsgObj)

        rootJson.put("contents", contentsArr)

        val genConfig = JSONObject()
        genConfig.put("temperature", 0.7)
        rootJson.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        val responseStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
            val fallbackReq = Request.Builder().url(fallbackUrl).post(body).build()
            val fallbackResp = client.newCall(fallbackReq).execute()
            val fallbackStr = fallbackResp.body?.string() ?: ""

            if (!fallbackResp.isSuccessful) {
                val errJson = try { JSONObject(fallbackStr) } catch (e: Exception) { null }
                val msg = errJson?.optJSONObject("error")?.optString("message") ?: "Gagal menghubungi Gemini AI (${response.code})"
                return Result.failure(Exception(msg))
            }
            val text = extractTextFromGeminiResponse(fallbackStr)
            return Result.success(text)
        }

        val responseText = extractTextFromGeminiResponse(responseStr)
        return Result.success(responseText)
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
