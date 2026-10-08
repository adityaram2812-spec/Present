package com.adityaram.present.ui.auth

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adityaram.present.data.auth.AuthResult
import com.adityaram.present.data.auth.AuthState
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.adityaram.present.ui.components.PresentTopBar

@Composable
fun AccountScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val entitlement by viewModel.entitlement.collectAsState()
    val colors = LocalPresentColors.current
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        PresentTopBar(
            title = "Account",
            subtitle = "Manage your Present account",
            onBack = onBack
        )

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.screenHorizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacing24))

            when (val state = authState) {
                is AuthState.SignedIn -> {
                    AccountSignedInView(
                        user = state.user,
                        entitlement = entitlement,
                        onSignOut = { viewModel.signOut() },
                        onDeleteAccount = {
                            viewModel.deleteAccount { result ->
                                if (result is AuthResult.RequiresRecentLogin) {
                                    viewModel.signOut()
                                }
                            }
                        },
                        onRefreshEntitlement = { viewModel.fetchEntitlement() },
                        context = context,
                        scope = scope
                    )
                }
                else -> {
                    AccountSignedOutView(
                        onGoogleSignIn = {
                            scope.launch {
                                viewModel.signInWithGoogle(context) { /* handled by state flow */ }
                            }
                        },
                        onEmailSignIn = onNavigateToSignIn,
                        isLoading = authState is AuthState.Loading
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing40))
        }
    }
}

@Composable
private fun AccountSignedInView(
    user: com.google.firebase.auth.FirebaseUser,
    entitlement: com.adityaram.present.data.account.AiEntitlement?,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onRefreshEntitlement: () -> Unit,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val colors = LocalPresentColors.current
    var showSignOutDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showDeleteDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        onRefreshEntitlement()
    }

    if (showSignOutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(text = "Sign out of Present?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("You'll need to sign in again to access account-based features and timetable imports.")
            },
            confirmButton = {
                TextButton(onClick = { 
                    showSignOutDialog = false
                    onSignOut() 
                }) {
                    Text("Sign out", color = colors.critical)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = colors.primaryText)
                }
            },
            containerColor = colors.surface,
            titleContentColor = colors.primaryText,
            textContentColor = colors.secondaryText
        )
    }

    if (showDeleteDialog) {
        val red = Color(0xFFEF4444)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(text = "Delete your account?", fontWeight = FontWeight.Bold, color = red)
            },
            text = {
                Text("This permanently deletes your Present account. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    onDeleteAccount() 
                }) {
                    Text("Delete account", color = red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.primaryText)
                }
            },
            containerColor = colors.surface,
            titleContentColor = red,
            textContentColor = colors.secondaryText
        )
    }
    
    // ACCOUNT PROFILE
    Text(
        text = "ACCOUNT PROFILE",
        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em),
        color = colors.mutedText,
        modifier = Modifier.padding(bottom = Dimens.spacing12)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "User"
        val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.elevatedSurface),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.accent)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colors.primaryText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = user.email ?: "", style = Typography.bodySmall, color = colors.secondaryText)
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(colors.elevatedSurface)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Signed in", style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = Color(0xFF22C55E))
            }
        }
    }
    
    Spacer(modifier = Modifier.height(Dimens.spacing32))
    
    Spacer(modifier = Modifier.height(Dimens.spacing32))
    
    // DATA & PRIVACY
    Text(
        text = "DATA & PRIVACY",
        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em),
        color = colors.mutedText,
        modifier = Modifier.padding(bottom = Dimens.spacing12)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
    ) {
        // AI Imports
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Timetable imports", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.primaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (entitlement == null) {
                        Text(text = "Fetching quota...", style = Typography.bodySmall, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { 0f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = colors.accent,
                            trackColor = colors.border
                        )
                    } else if (entitlement.limit > 100 || entitlement.limit <= -1) {
                        Text(text = "Unlimited imports", style = Typography.bodySmall, color = colors.secondaryText)
                    } else {
                        Text(text = "${entitlement.used} of ${entitlement.limit} used", style = Typography.bodySmall, color = colors.secondaryText)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val progressValue = if (entitlement.limit > 0) (entitlement.used.toFloat() / entitlement.limit.toFloat()).coerceIn(0f, 1f) else 0f
                        val progressColor = if (progressValue >= 1f) Color(0xFFEF4444) else colors.accent
                        
                        LinearProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = progressColor,
                            trackColor = colors.border
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        
        // Privacy Info
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF22C55E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Local data privacy", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colors.primaryText)
                    Text(
                        text = "Logs and attendance stay strictly on this device.",
                        style = Typography.bodySmall,
                        color = colors.secondaryText,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing32))

    // MANAGE
    Text(
        text = "MANAGE",
        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em),
        color = colors.mutedText,
        modifier = Modifier.padding(bottom = Dimens.spacing12)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
    ) {
        // Sign Out Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSignOutDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Sign out", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = colors.primaryText, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.mutedText)
        }
        
        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        
        // Delete Account Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDeleteDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val red = Color(0xFFEF4444)
            Text(text = "Delete account", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = red, modifier = Modifier.weight(1f))
            Text(text = "Permanently remove", style = Typography.labelSmall, color = colors.mutedText)
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Signing out or deleting your account never deletes your local timetable or class logs.",
        style = Typography.bodySmall,
        color = colors.mutedText,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun AccountSignedOutView(
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    isLoading: Boolean
) {
    val colors = LocalPresentColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.elevatedSurface),
            contentAlignment = Alignment.Center
        ) {
            // Unauthenticated icon
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.mutedText, modifier = Modifier.size(32.dp))
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "No account signed in",
            style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.primaryText
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "An account is required to automatically extract your timetable. Manual setup is always available offline.",
            style = Typography.bodyMedium,
            color = colors.secondaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 22.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Google button
        Button(
            onClick = { if (!isLoading) onGoogleSignIn() },
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
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Email button
        OutlinedButton(
            onClick = { if (!isLoading) onEmailSignIn() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = colors.primaryText),
            enabled = !isLoading
        ) {
            Text(text = "Continue with email", style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Present prioritizes your privacy. Your class logs and attendance stay strictly on this device.",
            style = Typography.labelSmall,
            color = colors.mutedText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
