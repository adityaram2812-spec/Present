package com.adityaram.present.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.components.PresentTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAndFeedbackScreen(onBack: () -> Unit) {
    val colors = LocalPresentColors.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "Help & feedback",
                onBack = onBack
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = Dimens.screenHorizontalPadding)
                    .padding(bottom = Dimens.spacing40)
                    .padding(top = Dimens.spacing16)
            ) {
            Text(
                text = "Need help?",
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.primaryText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Consult the common issues and frequently asked questions below. If you're experiencing a major issue, submit a feedback request directly.",
                style = Typography.bodyMedium,
                color = colors.secondaryText,
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            FaqItem(
                question = "How does attendance tracking work?",
                answer = "Present stores your timetable and attendance records locally on your device."
            )

            FaqItem(
                question = "Can I import my timetable?",
                answer = "Yes. You can import a timetable from supported formats available in Present, including automatic timetable extraction."
            )

            FaqItem(
                question = "Does timetable extraction use the internet?",
                answer = "Yes. Automatic timetable extraction requires a signed-in account and securely sends the selected timetable image to Present's backend for processing."
            )

            FaqItem(
                question = "How many timetable imports can I use?",
                answer = "Free accounts currently include 2 timetable image imports. Premium accounts may have different limits depending on the current entitlement."
            )

            FaqItem(
                question = "Is my attendance data uploaded?",
                answer = "Attendance data is designed to remain on-device."
            )

            FaqItem(
                question = "What happens if automatic extraction gets something wrong?",
                answer = "The user should review the extracted timetable before confirming/importing it."
            )
            
            FaqItem(
                question = "I found a bug. What should I do?",
                answer = "Use the feedback/contact action."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Feedback Link Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { /* TODO: Bind to real feedback email intent */ }
                    .background(colors.surface)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Send feedback",
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                )
            }
        }
    }
}
}

@Composable
private fun FaqItem(question: String, answer: String) {
    val colors = LocalPresentColors.current
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = question,
            style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.primaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = answer,
            style = Typography.bodyMedium,
            color = colors.secondaryText,
            lineHeight = 20.sp
        )
    }
}
