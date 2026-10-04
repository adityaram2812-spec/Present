package com.adityaram.present.data.account

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class CloudUserRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun syncProfile() {
        val user = auth.currentUser ?: return
        val docRef = firestore.collection("users").document(user.uid)
        
        try {
            val snapshot = docRef.get().await()
            val data = mutableMapOf<String, Any>(
                "uid" to user.uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            user.displayName?.let { data["displayName"] = it }
            user.email?.let { data["email"] = it }
            user.photoUrl?.let { data["photoUrl"] = it.toString() }

            if (!snapshot.exists()) {
                data["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(data).await()
                Log.d("CloudUserRepository", "Created new user profile in Firestore")
            } else {
                docRef.set(data, SetOptions.merge()).await()
                Log.d("CloudUserRepository", "Merged existing user profile in Firestore")
            }

            // -----------------------------------------------------------------
        } catch (e: Exception) {
            // Silently catch exceptions to respect offline-first behaviors and prevent blocking MainThread.
            Log.e("CloudUserRepository", "Failed to sync profile: ${e.message}", e)
        }
    }

    suspend fun getProfile(): CloudUserProfile? {
        val user = auth.currentUser ?: return null
        return try {
            val doc = firestore.collection("users").document(user.uid).get().await()
            doc.toObject(CloudUserProfile::class.java)
        } catch (e: Exception) {
            Log.e("CloudUserRepository", "Failed to fetch profile: ${e.message}", e)
            null
        }
    }

    suspend fun fetchEntitlement(): AiEntitlement? {
        val user = auth.currentUser
        if (user == null) {
            return null
        }
        return try {
            val tokenResult = user.getIdToken(false).await()
            val idToken = tokenResult.token

            if (!idToken.isNullOrEmpty()) {
                val appCheckTokenResult = kotlinx.coroutines.withTimeoutOrNull(5000L) {
                    com.google.firebase.appcheck.FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                }
                
                val appCheckToken = appCheckTokenResult?.token ?: ""

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                    
                val requestBuilder = okhttp3.Request.Builder()
                    .url("https://present-backend-56n2.onrender.com/api/auth/entitlement")
                    .header("Authorization", "Bearer $idToken")
                    .get()
                    
                if (appCheckToken.isNotEmpty()) {
                    requestBuilder.header("X-Firebase-AppCheck", appCheckToken)
                }
                
                val request = requestBuilder.build()

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyText = response.body?.string()
                            if (bodyText != null) {
                                val json = org.json.JSONObject(bodyText)
                                AiEntitlement(
                                    used = json.optInt("used", 0),
                                    limit = json.optInt("limit", 2),
                                    remaining = json.optInt("remaining", 0),
                                    authenticated = json.optBoolean("authenticated", false)
                                )
                            } else null
                        } else null
                    }
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

data class AiEntitlement(
    val used: Int,
    val limit: Int,
    val remaining: Int,
    val authenticated: Boolean
)
