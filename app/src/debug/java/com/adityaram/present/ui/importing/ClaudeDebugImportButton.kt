package com.adityaram.present.ui.importing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adityaram.present.debug.ClaudeEvaluationHelper
import com.adityaram.present.ui.theme.LocalPresentColors

/**
 * DEBUG-ONLY composable that renders the "Test Opus Extraction" button.
 * This file exists ONLY in src/debug/ and is entirely excluded from release APKs.
 *
 * When tapped, it reads the validated Claude Opus 35-class JSON from raw resources
 * and injects it through ClaudeEvaluationHelper → ClaudeExtractionProvider →
 * AiTimetableParser → ImportViewModel.evaluateParserForDevelopment().
 */
@Composable
fun ClaudeDebugImportButton(viewModel: ImportViewModel) {
    val context = LocalContext.current
    val colors = LocalPresentColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                val json = context.resources
                    .openRawResource(com.adityaram.present.R.raw.claude_opus_payload)
                    .bufferedReader()
                    .use { it.readText() }
                ClaudeEvaluationHelper.injectClaudeJson(context, viewModel, json)
            },
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚗️", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Test Opus Extraction (DEBUG)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
                Text(
                    text = "Inject validated 35-class benchmark payload",
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
            }
        }
    }
}
