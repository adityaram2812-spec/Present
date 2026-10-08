package com.adityaram.present.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.adityaram.present.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class AuthRepository(private val auth: FirebaseAuth) {

    val authState: Flow<AuthState> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                trySend(AuthState.SignedIn(user))
            } else {
                trySend(AuthState.SignedOut)
            }
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInWithGoogle(context: Context): AuthResult {
        return try {
            val credentialManager = CredentialManager.create(context)
            
            // Get web client id from generated string resource
            val webClientId = context.getString(R.string.default_web_client_id)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && 
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                AuthResult.Success(authResult.user)
            } else {
                AuthResult.Error("Unsupported Google credential type received.")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Error("Sign in canceled.")
        } catch (e: GetCredentialException) {
            AuthResult.Error("Google Sign-In failed. Please try again.")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AuthResult.Error(mapFirebaseAuthError(e))
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            AuthResult.Success(result.user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseAuthError(e))
        }
    }
    
    suspend fun createAccountWithEmail(email: String, pass: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            AuthResult.Success(result.user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseAuthError(e))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun updateDisplayName(name: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("No user signed in.")
        return try {
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseAuthError(e))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success(null)
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseAuthError(e))
        }
    }

    suspend fun deleteAccount(): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("No user signed in.")
        return try {
            user.delete().await()
            AuthResult.Success(null)
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            AuthResult.RequiresRecentLogin
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseAuthError(e))
        }
    }
    
    private fun mapFirebaseAuthError(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "Account not found or has been disabled."
            is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
            is FirebaseAuthUserCollisionException -> "An account already exists for this email."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use a stronger password."
            is FirebaseAuthRecentLoginRequiredException -> "For your security, please sign in again to complete this action."
            is com.google.firebase.FirebaseNetworkException -> "Network error. Please check your connection."
            else -> exception.localizedMessage ?: "An unexpected authentication error occurred."
        }
    }
}
