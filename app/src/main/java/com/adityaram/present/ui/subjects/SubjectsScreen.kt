package com.adityaram.present.ui.subjects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adityaram.present.ui.theme.LocalPresentColors

@Composable
fun SubjectsScreen() {
    val colors = LocalPresentColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Subjects Placeholder", color = colors.primaryText)
    }
}
