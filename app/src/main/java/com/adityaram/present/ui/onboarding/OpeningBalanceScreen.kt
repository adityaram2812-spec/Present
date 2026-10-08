package com.adityaram.present.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adityaram.present.data.model.Subject
import com.adityaram.present.ui.components.PresentTextField
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

@Composable
fun OpeningBalanceScreen(
    viewModel: OpeningBalanceViewModel,
    onFinished: () -> Unit
) {
    val colors = LocalPresentColors.current
    val uiState by viewModel.uiState.collectAsState()

    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = Dimens.spacing24, start = Dimens.spacing24, end = Dimens.spacing24)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Opening balances",
                style = Typography.displaySmall,
                color = colors.primaryText,
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            Text(
                text = "Enter your current attendance. Leave at 0 if you're not sure.",
                style = Typography.bodyLarge,
                color = colors.secondaryText,
            )
        }

        Spacer(modifier = Modifier.height(Dimens.spacing24))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing16),
            contentPadding = PaddingValues(bottom = 100.dp) // Leave space for FAB
        ) {
            if (uiState.subjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.spacing24))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
                            .padding(Dimens.spacing24),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No subjects added yet. Complete setup to go home.",
                            style = Typography.bodyMedium,
                            color = colors.mutedText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(uiState.subjects, key = { it.id }) { subject ->
                    SubjectBalanceInput(
                        subject = subject,
                        present = uiState.balances[subject.id]?.present ?: 0,
                        absent = uiState.balances[subject.id]?.absent ?: 0,
                        onUpdate = { p, a -> viewModel.updateBalance(subject.id, p, a) }
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Button(
            onClick = {
                isSaving = true
                viewModel.completeOnboarding(onSuccess = onFinished)
            },
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF101012)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacing24)
                .padding(bottom = Dimens.spacing12)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = colors.background, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(Dimens.spacing8))
                Text("Finish setup", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
fun SubjectBalanceInput(
    subject: Subject,
    present: Int,
    absent: Int,
    onUpdate: (Int, Int) -> Unit
) {
    val colors = LocalPresentColors.current

    var pText by remember(subject.id) { mutableStateOf(if (present > 0) present.toString() else "") }
    var aText by remember(subject.id) { mutableStateOf(if (absent > 0) absent.toString() else "") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.spacing16))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing16))
            .padding(Dimens.spacing16)
    ) {
        Column {
            Text(
                text = subject.name,
                style = Typography.headlineMedium,
                color = colors.primaryText
            )
            Spacer(modifier = Modifier.height(Dimens.spacing16))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                Box(modifier = Modifier.weight(1f)) {
                    PresentTextField(
                        value = pText,
                        onValueChange = {
                            pText = it
                            val p = it.toIntOrNull() ?: 0
                            onUpdate(p, absent)
                        },
                        label = "Attended",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    PresentTextField(
                        value = aText,
                        onValueChange = {
                            aText = it
                            val a = it.toIntOrNull() ?: 0
                            onUpdate(present, a)
                        },
                        label = "Missed",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }
    }
}
