package com.adityaram.present.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adityaram.present.R
import com.adityaram.present.data.auth.AuthState
import com.adityaram.present.ui.auth.AuthViewModel
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

@Composable
fun ChooseAccountModeScreen(
    authViewModel: AuthViewModel,
    onNavigateToSignIn: () -> Unit,
    onNavigateNext: () -> Unit
) {
    val colors = LocalPresentColors.current
    val authState by authViewModel.authState.collectAsState()

    // Auto-advance if signed in
    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) {
            onNavigateNext()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(Dimens.spacing24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(0.15f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Choose your setup",
                style = Typography.displaySmall,
                color = colors.primaryText,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            Text(
                text = "Present works offline perfectly, but signing in unlocks timetable imports.",
                style = Typography.bodyLarge,
                color = colors.secondaryText,
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.weight(0.2f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing16)
        ) {
            // Google Sign-In Option
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.spacing24))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
                    .padding(Dimens.spacing24)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Recommended", style = Typography.labelSmall, color = colors.accent)
                    Spacer(modifier = Modifier.height(Dimens.spacing4))
                    Text(text = "Sign in with Google", style = Typography.headlineMedium, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Text(
                        text = "Unlocks timetable imports and cross-device sync.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))
                    Button(
                        onClick = onNavigateToSignIn,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.elevatedSurface, contentColor = colors.primaryText),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Sign in",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text("Sign in", style = Typography.labelLarge)
                    }
                }
            }

            // Offline Mode Option
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.spacing24))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
                    .padding(Dimens.spacing24)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Offline mode", style = Typography.headlineMedium, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Text(
                        text = "Keep everything exclusively on this device.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))
                    OutlinedButton(
                        onClick = onNavigateNext,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryText),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Continue offline", style = Typography.labelLarge)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(0.1f))
    }
}
