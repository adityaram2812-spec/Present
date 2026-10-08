package com.adityaram.present.ui.threshold

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.PillShape
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdScreen(
    viewModel: ThresholdViewModel = viewModel(factory = ThresholdViewModel.Factory),
    onNavigateBack: () -> Unit
) {
    val currentThreshold by viewModel.thresholdUiState.collectAsState()
    val colors = LocalPresentColors.current
    val scrollState = rememberScrollState()

    var sliderValue by remember(currentThreshold) { mutableFloatStateOf(currentThreshold.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        PresentTopBar(
            title = "Default Threshold",
            subtitle = "Set your attendance requirement",
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(
                    top = Dimens.spacing16,
                    bottom = Dimens.spacing40,
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding
                )
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ATTENDANCE REQUIREMENT",
                    style = Typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.12.em
                    ),
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(colors.softAccent)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Active Default",
                        style = Typography.labelSmall,
                        color = colors.accent
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing24))

            // Prominent Value Display
            Text(
                text = "${sliderValue.toInt()}%",
                style = Typography.displayLarge.copy(fontSize = 56.sp, fontWeight = FontWeight.Bold),
                color = colors.primaryText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacing16))

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    viewModel.setThreshold(sliderValue.toInt())
                },
                valueRange = 50f..100f,
                steps = 49,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.border,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "50%",
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.mutedText
                )
                Text(
                    text = "100%",
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.mutedText
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // Quick Values Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(60, 65, 75, 80, 85)
                presets.forEach { preset ->
                    val isSelected = sliderValue.toInt() == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .background(if (isSelected) colors.accent else colors.surface)
                            .clickable {
                                sliderValue = preset.toFloat()
                                viewModel.setThreshold(preset)
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$preset%",
                            style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else colors.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing16))

            Text(
                text = "Present uses this target across your attendance insights.",
                style = Typography.bodySmall,
                color = colors.mutedText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacing40))

            // "Used Across Present" Scope Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .padding(20.dp)
            ) {
                Text(
                    text = "Used across Present",
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.height(Dimens.spacing16))
                ScopeItem("Home recovery guidance")
                ScopeItem("Analytics")
                ScopeItem("Attendance Planner")
                ScopeItem("Margin alerts", isLast = true)
            }
        }
    }
}

@Composable
private fun ScopeItem(text: String, isLast: Boolean = false) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = if (isLast) 0.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.accent)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = Typography.bodySmall,
            color = colors.secondaryText
        )
    }
}
