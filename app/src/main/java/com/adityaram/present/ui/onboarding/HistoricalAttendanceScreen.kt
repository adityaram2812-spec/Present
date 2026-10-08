package com.adityaram.present.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun HistoricalAttendanceScreen(
    onEnterManually: () -> Unit,
    onStartFresh: () -> Unit
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
                text = "Attendance records",
                style = Typography.displaySmall,
                color = colors.primaryText,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            Text(
                text = "Do you have previous attendance records?",
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
            // Option 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.spacing24))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
                    .padding(Dimens.spacing24)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Yes, I have them", style = Typography.headlineMedium, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Text(
                        text = "Add your current attendance.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))
                    Button(
                        onClick = onEnterManually,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryText, contentColor = colors.background),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add records", style = Typography.labelLarge)
                    }
                }
            }

            // Option 2
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.spacing24))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
                    .padding(Dimens.spacing24)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "No, I don't have them", style = Typography.headlineMedium, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Text(
                        text = "We'll start tracking from today.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing16))
                    OutlinedButton(
                        onClick = onStartFresh,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryText),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Start fresh", style = Typography.labelLarge)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(0.1f))
    }
}
