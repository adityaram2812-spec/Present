package com.adityaram.present.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

@Composable
fun PresentBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalPresentColors.current
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.elevatedSurface)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Navigate back",
            tint = colors.primaryText,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun PresentTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp), // Consistent, tight inset mapping closely to status bar bounds
        verticalAlignment = Alignment.CenterVertically
    ) {
        PresentBackButton(onClick = onBack)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Typography.headlineMedium,
                color = colors.primaryText
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp)) // Refined spacing attaching subtitle explicitly to title
                Text(
                    text = subtitle,
                    style = Typography.bodySmall, // Body small matches Present's compact subtitle requirement
                    color = colors.mutedText
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailingContent()
        }
    }
}
