package com.example.data.remote

import com.example.BuildConfig
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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val defaultGroqApiKey = "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"

    private fun buildSystemInstruction(
        userName: String,
        notesCount: Int,
        recentNotesSummary: String,
        topSongsSummary: String
    ): String {
        return """
            Kamu adalah 'Mochibot' (juga dikenal sebagai Teman AI), maskot resmi & pendamping kesehatan emosional cerdas di aplikasi "Are You Okay?".
            
            PERAN & KARAKTER UTAMA:
            1. PENDENGAR EMOSIONAL & PSIKOLOGI KLINIS RINGAN: Gunakan teknik Active Listening, Reframe Pikiran Negatif (Cognitive Behavioral Therapy / CBT), Validasi Emosi, serta panduan koping cemas & depresi ringan.
            2. PERPERSPEKTIF FILOSOFIS DEPAS & BIJAK: Jika pengguna bertanya atau curhat masalah hidup, selipkan kearifan filosofis (misalnya Stoikisme Marcus Aurelius / Epictetus tentang dikotomi kendali, Logoterapi Viktor Frankl tentang arti penderitaan, Eksistensialisme, atau Mindfulness).
            3. HANGAT, GEN Z FRIENDLY & EMPATIK: Gunakan bahasa Indonesia yang santai, empatik, tidak kaku, jujur, hangat, dan tidak menggurui. Sapalah pengguna dengan nama dekatnya: "$userName".
            
            PRINSIP EFISIENSI RESPON (HEMAT KUOTA AI):
            - Jawablah secara ringkas, padat, hangat, dan langsung pada inti emosi (maksimal 2-3 paragraf pendek atau 80-120 kata).
            - Hindari pengulangan kata atau kalimat basa-basi yang berlebihan agar hemat kuota/token API tanpa mengurangi empati, wawasan psikologi klinis, dan kedalaman filosofis.

            KONTEKS PENGGUNA TERKINI DI DATABASE APLIKASI:
            - Nama Pengguna: $userName
            - Jumlah Catatan Curhat di Database: $notesCount catatan
            - Catatan/Mood Terbaru Pengguna: ${if (recentNotesSummary.isNotBlank()) recentNotesSummary else "Belum ada catatan baru"}
            - Lagu Favorit Pengguna di App: ${if (topSongsSummary.isNotBlank()) topSongsSummary else "Belum ada lagu tersimpan"}
            
            PENGETAHUAN LENGKAP TENTANG APLIKASI "ARE YOU OKAY?":
            - Fitur Catatan Curhat: Tempat menulis perasaan pribadi, memilih mood emoji, dan melampirkan lagu iTunes.
            - Fitur Curhat Global: Komunitas tempat berbagi curhatan anonim dan saling memberi tanggapan/komentar hangat.
            - Fitur Relaksasi Audio: Pemutar lagu preview & frekuensi suara penenang jiwa.
            - Fitur Latihan Pernapasan: Panduan napas 4-7-8 untuk menenangkan cemas.
            
            KEMAMPUAN AKSI LANGSUNG KE DATABASE (TINDAKAN OTOMATIS):
            Jika pengguna meminta kamu untuk:
            - "Buatkan kata-kata filosofis / curhatan / rangkuman dan simpan ke catatan / database", ATAU
            - "Buat catatan curhat tentang X beserta lagu Y", ATAU
            - Menyimpan hasil diskusi/quote langsung ke catatan curhat mereka,
            Maka selain memberikan respon obrolan hangat secara lengkap, kamu WAJIB menyertakan perintah aksi di bagian PALING AKHIR pesanmu dengan format berikut:
            
            [ACTION_CREATE_NOTE: {"content": "Tulis kalimat curhat / rangkuman / quote filosofis yang indah di sini", "category": "Perjalanan Jati Diri", "moodEmoji": "✨", "songTitle": "Judul Lagu (opsional)", "artistName": "Penyanyi (opsional)"}]
            
            Pilihan Category: "Asmara & Cinta", "Masalah Hidup", "Perjalanan Jati Diri", "Pendidikan & Sekolah".
            Pilihan MoodEmoji: "💔", "🥹", "🌧️", "✨", "❤️‍🔥", "🎓".
            
            Jika pengguna tidak meminta menyimpan ke catatan, kamu tetap bisa memberikan 1 rekomendasi lagu & penyanyi yang cocok di akhir pesanmu. Jawablah dengan penuh empati, kedalaman filosofi, dan kehangatan sahabat.
        """.trimIndent()
    }

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
        apiKeyOverride: String? = null,
        userName: String = "Sahabat",
        notesCount: Int = 0,
        recentNotesSummary: String = "",
        topSongsSummary: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val systemPrompt = buildSystemInstruction(userName, notesCount, recentNotesSummary, topSongsSummary)
        try {
            val activeKey = apiKeyOverride?.ifBlank { null }
                ?: getApiKeyFromBuildConfig().ifBlank { defaultGroqApiKey }

            if (activeKey.startsWith("gsk_") || activeKey == defaultGroqApiKey) {
                return@withContext sendGroqMessage(activeKey, chatHistory, userPrompt, systemPrompt)
            } else {
                val geminiResult = sendGeminiMessage(activeKey, chatHistory, userPrompt, systemPrompt)
                if (geminiResult.isSuccess) {
                    return@withContext geminiResult
                }
                return@withContext sendGroqMessage(defaultGroqApiKey, chatHistory, userPrompt, systemPrompt)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sendGroqMessage(
        apiKey: String,
        chatHistory: List<GeminiMessage>,
        userPrompt: String,
        systemInstruction: String
    ): Result<String> {
        val modelsToTry = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
        val mediaType = "application/json; charset=utf-8".toMediaType()

        for (model in modelsToTry) {
            try {
                val rootJson = JSONObject()
                rootJson.put("model", model)
                rootJson.put("temperature", 0.7)

                val messagesArr = JSONArray()
                
                val sysObj = JSONObject()
                sysObj.put("role", "system")
                sysObj.put("content", systemInstruction)
                messagesArr.put(sysObj)

                for (msg in chatHistory) {
                    val mObj = JSONObject()
                    mObj.put("role", if (msg.role == "model") "assistant" else "user")
                    mObj.put("content", msg.text)
                    messagesArr.put(mObj)
                }

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
                continue
            }
        }

        return Result.failure(Exception("Gagal menghubungi server AI. Silakan coba beberapa saat lagi."))
    }

    private fun sendGeminiMessage(
        apiKey: String,
        chatHistory: List<GeminiMessage>,
        userPrompt: String,
        systemInstruction: String
    ): Result<String> {
        val rootJson = JSONObject()

        val sysInstructionObj = JSONObject()
        val sysPartsArr = JSONArray()
        sysPartsArr.put(JSONObject().put("text", systemInstruction))
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

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        val responseStr = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"
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
        return "Maaf, aku belum bisa memberikan tanggapan saat ini. Boleh coba tanyakan lagi ya!"
    }
}

