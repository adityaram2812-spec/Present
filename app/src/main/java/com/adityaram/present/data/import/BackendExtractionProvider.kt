package com.adityaram.present.data.import

import com.adityaram.present.domain.import.ExtractionResult
import com.adityaram.present.domain.import.TimetableExtractionProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class BackendExtractionProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : TimetableExtractionProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    override suspend fun extract(file: File): Result<ExtractionResult> = withContext(Dispatchers.IO) {
        android.util.Log.d("PresentImport", "IMPORT_PROVIDER_CALLED")
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(Exception("AUTH_REQUIRED: Authentication required."))
            
            // 1. Get Firebase ID token gracefully shielding exceptions natively
            val idTokenResult = user.getIdToken(false).await()
            val idToken = idTokenResult.token ?: return@withContext Result.failure(Exception("AUTH_REQUIRED: Failed to obtain authentication token."))

            // 1.5 Get App Check Token
            val appCheckTokenResult = com.google.firebase.appcheck.FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            val appCheckToken = appCheckTokenResult.token

            val mediaType = if (file.extension.equals("pdf", ignoreCase = true)) {
                "application/pdf"
            } else if (file.extension.equals("png", ignoreCase = true)) {
                "image/png"
            } else {
                "image/jpeg"
            }

            // 2. Wrap image bytes in strict Multipart Form natively mapped to Node endpoints
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody(mediaType.toMediaType())
                )
                .build()

            // 3. Delegate directly towards the active remote backend securely injecting auth bearers
            val requestBuilder = Request.Builder()
                .url("https://present-backend-56n2.onrender.com/api/extract-timetable")
                .header("Authorization", "Bearer ${idToken}")
                
            if (appCheckToken.isNotEmpty()) {
                requestBuilder.header("X-Firebase-AppCheck", appCheckToken)
            }
                
            val request = requestBuilder.post(requestBody).build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                var errorCode = "UNKNOWN_ERROR"
                try {
                    val errorJson = JSONObject(errorBody)
                    if (errorJson.has("error")) {
                        errorCode = errorJson.getString("error")
                    }
                } catch (e: Exception) {
                    // Ignore JSON parsing errors for error body masking cleanly 
                }
                
                android.util.Log.d("PresentImport", "IMPORT_EXTRACTION_FAILED: Code ${response.code}")
                // 4. Map logical server states strictly into distinct Exception variants handled by ImportViewModel organically
                return@withContext when (response.code) {
                    401 -> Result.failure(Exception("AUTH_REQUIRED: Please sign in again."))
                    403 -> {
                         if (errorCode == "AI_IMPORT_LIMIT_REACHED") {
                             Result.failure(Exception("AI_IMPORT_LIMIT_REACHED: You have reached your free AI import limit."))
                         } else {
                             Result.failure(Exception("Access denied by backend."))
                         }
                    }
                    500, 502, 503, 504 -> Result.failure(Exception("AI_EXTRACTION_FAILED: Backend failed to extract timetable. ($errorCode)"))
                    413 -> Result.failure(Exception("FILE_TOO_LARGE: The image must be under 5MB."))
                    415 -> Result.failure(Exception("UNSUPPORTED_MEDIA_TYPE: Only JPEG, PNG, and PDF are supported."))
                    else -> Result.failure(Exception("Server returned HTTP ${response.code}: $errorCode"))
                }
            }

            val jsonOutput = response.body?.string() ?: return@withContext Result.failure(Exception("Empty API response from backend."))
            
            val jsonObject = JSONObject(jsonOutput)
            val groupsFound = jsonObject.optJSONArray("groups_found")
            val detectedGroups = mutableListOf<String>()
            
            if (groupsFound != null) {
                for (i in 0 until groupsFound.length()) {
                    detectedGroups.add(groupsFound.getString(i))
                }
            }
            
            // 5. Structure payload identically mimicking Gemini extraction models returning valid ParsedTimetable shapes 
            android.util.Log.d("PresentImport", "IMPORT_EXTRACTION_SUCCESS")
            Result.success(
                ExtractionResult(
                    responseJson = jsonOutput,
                    detectedGroups = detectedGroups,
                    tokenUsageReport = "Powered by Present Backend (Render)"
                )
            )

        } catch (e: IOException) {
            android.util.Log.d("PresentImport", "IMPORT_EXTRACTION_FAILED: IOException ${e.message}")
            Result.failure(Exception("Network error connecting to backend. Please check your connection."))
        } catch (e: Exception) {
            android.util.Log.d("PresentImport", "IMPORT_EXTRACTION_FAILED: Exception ${e.message}")
            Result.failure(Exception(e.localizedMessage ?: "Unknown network error occurred."))
        }
    }
}
