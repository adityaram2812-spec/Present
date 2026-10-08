package com.adityaram.present.data.import

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.adityaram.present.BuildConfig
import com.adityaram.present.domain.import.ExtractionResult
import com.adityaram.present.domain.import.TimetableExtractionProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class GeminiExtractionProvider : TimetableExtractionProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun extract(file: File): Result<ExtractionResult> = withContext(Dispatchers.IO) {
        try {
            val isPdf = file.extension.equals("pdf", ignoreCase = true)
            val mediaType = if (isPdf) "application/pdf" else "image/jpeg"
            val fileBytes = if (isPdf) file.readBytes() else compressImage(file)
            
            if (fileBytes.isEmpty()) {
                return@withContext Result.failure(Exception("File is empty or unreadable."))
            }

            val base64Data = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
            
            // Build the JSON payload for Gemini API
            val payload = buildGeminiPayload(base64Data, mediaType)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
            
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "No body"
                return@withContext Result.failure(Exception("AI Extraction failed. HTTP ${response.code}: $errBody"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty API response"))
            val responseJsonObj = JSONObject(responseBody)
            
            // Extract the usageMetadata for cost calculation
            var usageReport = "Cost unknown"
            if (responseJsonObj.has("usageMetadata")) {
                val usage = responseJsonObj.getJSONObject("usageMetadata")
                val promptTokens = usage.optInt("promptTokenCount", 0)
                val candidateTokens = usage.optInt("candidatesTokenCount", 0)
                val totalTokens = usage.optInt("totalTokenCount", 0)
                
                // Gemini 3.1 Flash-Lite rates:
                // $0.25 per 1M input tokens,  $1.50 per 1M output tokens (including thinking tokens)
                val promptCost = (promptTokens.toDouble() / 1_000_000) * 0.25
                val candidateCost = (candidateTokens.toDouble() / 1_000_000) * 1.50
                val estimatedCost = promptCost + candidateCost
                
                usageReport = "Prompt: $promptTokens, Response: $candidateTokens, Total: $totalTokens\nEstimated Cost: $${String.format("%.5f", estimatedCost)}"
                println("Gemini Usage: $usageReport")
            }

            // Extract the structured output text
            val candidates = responseJsonObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("No extraction candidates returned."))
            }
            
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text")
                ?: return@withContext Result.failure(Exception("Response missing text output."))

            val cleanJson = textOutput.trim().removePrefix("```json").removeSuffix("```").trim()

            // Validate against AiBackendResponse
            val aiData = try {
                gson.fromJson(cleanJson, AiBackendResponse::class.java)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Structured response parse error: ${e.message}"))
            }

            if (aiData.groupsFound == null || aiData.classes == null) {
                return@withContext Result.failure(Exception("Invalid timetable extraction. Vital fields missing."))
            }
            
            Result.success(ExtractionResult(
                responseJson = cleanJson,
                detectedGroups = aiData.groupsFound,
                tokenUsageReport = usageReport
            ))

        } catch (e: IOException) {
            Result.failure(Exception("Network error connecting to Gemini API."))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unknown AI extraction error occurred."))
        }
    }

    private fun buildGeminiPayload(base64Data: String, mimeType: String): JSONObject {
        // Prompt Text
        val prompt = """
            Extract the timetable schedule from this document.
            Follow these specific rules:
            1. Think minimally and briefly step-by-step before extraction to ensure correctness.
            2. When a timetable cell spans multiple adjacent time columns, treat the entire visual span as one session and use the start of the first covered column and end of the last covered column as the session's start/end.
            3. Explicit rule for this timetable: The 11:20-12:20 and 12:20-13:20 columns are one merged 2-hour practical block, therefore its interval is 11:20-13:20. Never return 11:20-12:20 or split these practicals.
            4. Assign 'shared' to classes without batches, or 'A', 'B', 'C' strictly to batched classes. Parallel A/B/C batch alternatives occupying the same merged block share the same 11:20-13:20 time.
            5. Preserve normal one-hour theory classes as one-hour sessions. Preserve Friday MinP as 14:00-16:00.
            6. Exclude BREAK, LUNCH, and RECESS from the classes array entirely.
            7. Format times in 24-hour HH:MM. Do not infer duration solely from the nearest one-hour column label; use the visible boundaries.
        """.trimIndent()

        val jsonClassesProps = JSONObject().apply {
            put("day", JSONObject().put("type", "string"))
            put("startTime", JSONObject().put("type", "string"))
            put("endTime", JSONObject().put("type", "string"))
            put("subject", JSONObject().put("type", "string"))
            put("teacher", JSONObject().put("type", "string"))
            put("room", JSONObject().put("type", "string"))
            put("group", JSONObject().put("type", "string"))
        }

        val jsonClassesItems = JSONObject().apply {
            put("type", "object")
            put("properties", jsonClassesProps)
            put("required", JSONArray().apply {
                put("day"); put("startTime"); put("endTime"); put("subject")
            })
        }

        val jsonSchemaProps = JSONObject().apply {
            put("division", JSONObject().put("type", "string"))
            put("groups_found", JSONObject().apply {
                put("type", "array")
                put("items", JSONObject().put("type", "string"))
            })
            put("classes", JSONObject().apply {
                put("type", "array")
                put("items", jsonClassesItems)
            })
        }

        val responseSchema = JSONObject().apply {
            put("type", "object")
            put("properties", jsonSchemaProps)
            put("required", JSONArray().apply { put("groups_found"); put("classes") })
        }

        val generationConfig = JSONObject().apply {
            put("responseMimeType", "application/json")
            put("responseSchema", responseSchema)
        }

        val textPart = JSONObject().put("text", prompt)
        val inlineDataPart = JSONObject().put("inlineData", JSONObject().apply {
            put("mimeType", mimeType)
            put("data", base64Data)
        })

        val partsArray = JSONArray().apply {
            put(textPart)
            put(inlineDataPart)
        }

        val contentsArray = JSONArray().apply {
            put(JSONObject().put("parts", partsArray))
        }

        return JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", generationConfig)
        }
    }

    private fun compressImage(file: File): ByteArray {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)
        
        val maxSize = 2048
        var inSampleSize = 1
        if (options.outWidth > maxSize || options.outHeight > maxSize) {
            val halfHeight: Int = options.outHeight / 2
            val halfWidth: Int = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxSize || halfWidth / inSampleSize >= maxSize) {
                inSampleSize *= 2
            }
        }
        
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream) 
        bitmap.recycle()
        return stream.toByteArray()
    }
}
