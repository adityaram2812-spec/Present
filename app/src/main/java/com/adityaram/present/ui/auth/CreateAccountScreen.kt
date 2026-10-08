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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.adityaram.present.data.auth.AuthResult
import com.adityaram.present.ui.components.PresentTextField
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

@Composable
fun CreateAccountScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val colors = LocalPresentColors.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
                text = "Create account",
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
                text = "Welcome to Present",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Set up your account for timetable imports",
                style = Typography.bodyMedium,
                color = colors.secondaryText
            )

            Spacer(modifier = Modifier.height(32.dp))

            PresentTextField(
                value = name,
                onValueChange = { name = it; errorMessage = null },
                label = "Full Name",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                label = "Password (min 6 characters)",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
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
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter all fields."
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = "Password must be at least 6 characters."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    
                    viewModel.createAccountWithEmail(name.trim(), email.trim(), password) { result ->
                        isLoading = false
                        when (result) {
                            is AuthResult.Success -> {
                                // Automatically triggers state flow correctly, backing out to Account
                                onNavigateToSignIn()
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
                    Text(text = "Create account", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colors.background)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
