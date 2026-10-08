package com.adityaram.present.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.account.CloudUserRepository
import com.adityaram.present.data.auth.AuthRepository
import com.adityaram.present.data.auth.AuthResult
import com.adityaram.present.data.auth.AuthState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import com.adityaram.present.data.account.AiEntitlement
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val cloudUserRepository: CloudUserRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Idle
        )
        
    init {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.SignedIn) {
                    cloudUserRepository.syncProfile()
                }
            }
        }
    }
    val currentUser 
        get() = authRepository.currentUser
        
    private val _entitlement = MutableStateFlow<AiEntitlement?>(null)
    val entitlement = _entitlement.asStateFlow()
    
    fun fetchEntitlement() {
        viewModelScope.launch {
            _entitlement.value = cloudUserRepository.fetchEntitlement()
        }
    }

    fun signInWithGoogle(context: Context, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(context)
            onResult(result)
        }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, pass)
            onResult(result)
        }
    }

    fun createAccountWithEmail(name: String, email: String, pass: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.createAccountWithEmail(email, pass)
            if (result is AuthResult.Success) {
                authRepository.updateDisplayName(name)
            }
            onResult(result)
        }
    }
    
    fun sendPasswordResetEmail(email: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            onResult(result)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun deleteAccount(onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            onResult(result)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PresentApplication
                return AuthViewModel(application.container.authRepository, application.container.cloudUserRepository) as T
            }
        }
    }
}
