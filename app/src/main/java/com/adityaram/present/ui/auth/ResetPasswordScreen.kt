package com.adityaram.present.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.adityaram.present.data.auth.AuthResult
import com.adityaram.present.ui.components.PresentTextField
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val colors = LocalPresentColors.current
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

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
                text = "Forgot password",
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
                text = "Reset your password",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                style = Typography.bodyMedium,
                color = colors.secondaryText,
                lineHeight = Typography.bodyMedium.lineHeight
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isSuccess) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF22C55E).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Check your email",
                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "We've sent a password reset link to $email. Follow the link to create a new password, then return here to sign in.",
                        style = Typography.bodyMedium,
                        color = Color(0xFF16A34A).copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text(text = "Return to sign in", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colors.background)
                }
            } else {
                PresentTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = "Email address",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    enabled = !isLoading
                )
    
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
                        if (email.isBlank()) {
                            errorMessage = "Please enter an email address."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        
                        viewModel.sendPasswordResetEmail(email.trim()) { result ->
                            isLoading = false
                            when (result) {
                                is AuthResult.Success -> {
                                    isSuccess = true
                                }
                                is AuthResult.Error -> {
                                    errorMessage = result.message
                                }
                                is AuthResult.RequiresRecentLogin -> {}
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
                        Text(text = "Send link", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colors.background)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
