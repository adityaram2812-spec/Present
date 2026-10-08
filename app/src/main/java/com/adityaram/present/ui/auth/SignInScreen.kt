package com.adityaram.present.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.adityaram.present.data.auth.AuthResult
import com.adityaram.present.ui.components.PresentTextField
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToCreateAccount: () -> Unit,
    onNavigateToResetPassword: () -> Unit,
    onNavigateToAccount: () -> Unit
) {
    val colors = LocalPresentColors.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                    top = Dimens.spacing24,
                    bottom = Dimens.spacing12
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.primaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.spacing16))
            Text(
                text = "Sign in",
                style = Typography.titleLarge,
                color = colors.primaryText
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.screenHorizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacing32))
            
            Text(
                text = "Welcome back",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your details to sign in",
                style = Typography.bodyMedium,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(32.dp))

            PresentTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = "Email address",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            PresentTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = "Password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Forgot password?",
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent,
                    modifier = Modifier.clickable { if (!isLoading) onNavigateToResetPassword() }.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Button(
                onClick = { 
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter both email and password."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    viewModel.signInWithEmail(email.trim(), password) { result ->
                        isLoading = false
                        when (result) {
                            is AuthResult.Success -> {
                                onNavigateToAccount()
                            }
                            is AuthResult.Error -> {
                                errorMessage = result.message
                            }
                            is AuthResult.RequiresRecentLogin -> {
                                // Ignore here
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, disabledContainerColor = colors.softAccent),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.background, strokeWidth = 2.dp)
                } else {
                    Text(text = "Sign in", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colors.background)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
                Text("or", color = colors.secondaryText, modifier = Modifier.padding(horizontal = 16.dp))
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.border))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (!isLoading) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            viewModel.signInWithGoogle(context) { result ->
                                isLoading = false
                                when (result) {
                                    is AuthResult.Success -> {
                                        onNavigateToAccount()
                                    }
                                    is AuthResult.Error -> {
                                        errorMessage = result.message
                                    }
                                    is AuthResult.RequiresRecentLogin -> {}
                                }
                            }
                        }
                    } 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryText, contentColor = colors.background),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.background, strokeWidth = 2.dp)
                } else {
                    Text(text = "Continue with Google", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = { if (!isLoading) onNavigateToCreateAccount() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = colors.primaryText),
                enabled = !isLoading
            ) {
                Text(text = "Create an account", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
