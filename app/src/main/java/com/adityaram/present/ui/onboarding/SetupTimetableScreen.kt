package com.adityaram.present.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

@Composable
fun SetupTimetableScreen(
    onNavigateToImport: () -> Unit,
    onNavigateToManual: () -> Unit
) {
    val colors = LocalPresentColors.current

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
                text = "Set up your timetable",
                style = Typography.displaySmall,
                color = colors.primaryText,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            Text(
                text = "Add your classes so Present can track your schedule.",
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
            // AI Import Option
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
                    Text(text = "Import timetable", style = Typography.headlineMedium, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Text(
                        text = "Upload a photo to automatically extract your classes.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))
                    Button(
                        onClick = onNavigateToImport,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryText, contentColor = colors.background),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Import",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text("Import timetable", style = Typography.labelLarge)
                    }
                }
            }

            // Skip Option
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.spacing24))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
                    .padding(Dimens.spacing24)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Manual setup", style = Typography.headlineMedium, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Text(
                        text = "Build your timetable from scratch.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))
                    OutlinedButton(
                        onClick = onNavigateToManual,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryText),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add subjects manually", style = Typography.labelLarge)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(0.1f))
    }
}
