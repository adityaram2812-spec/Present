package com.adityaram.present.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.PresentTheme
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar

@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppearanceViewModel = viewModel(factory = AppearanceViewModel.Factory)
) {
    val colors = LocalPresentColors.current
    val themeMode by viewModel.themeMode.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        PresentTopBar(
            title = "Appearance",
            subtitle = "Choose how Present looks",
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Theme Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            ) {
                AppearanceRow(
                    title = "Dark",
                    subtitle = "Always use dark appearance",
                    icon = Icons.Rounded.DarkMode,
                    isSelected = themeMode == "dark",
                    onClick = { viewModel.setThemeMode("dark") }
                )
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                AppearanceRow(
                    title = "Light",
                    subtitle = "Always use light appearance",
                    icon = Icons.Rounded.LightMode,
                    isSelected = themeMode == "light",
                    onClick = { viewModel.setThemeMode("light") }
                )
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                AppearanceRow(
                    title = "System",
                    subtitle = "Follow your Android system setting",
                    icon = Icons.Rounded.Smartphone,
                    isSelected = themeMode == "system",
                    onClick = { viewModel.setThemeMode("system") }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Preview Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Theme Preview",
                    style = Typography.labelMedium,
                    color = colors.secondaryText
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.softAccent)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val previewLabel = when (themeMode) {
                        "dark" -> "Dark"
                        "light" -> "Light"
                        else -> "System Mode"
                    }
                    Text(
                        text = "Active · $previewLabel",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent
                    )
                }
            }
            
            // Localized preview Theme overrides the external theme
            val previewIsSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val previewDarkTheme = when (themeMode) {
                "system" -> previewIsSystemDark
                "light" -> false
                else -> true
            }
            PresentTheme(darkTheme = previewDarkTheme) {
                val pColors = LocalPresentColors.current
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(pColors.background) // Usually '#101012' per design system
                        .border(1.dp, pColors.border, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "82.4%",
                                style = Typography.displayLarge,
                                color = pColors.primaryText
                            )
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(pColors.safe.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(pColors.safe)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+7.4% SAFE",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = pColors.safe
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(pColors.surface) // E.g., '#18181B' surface tile inside Dashboard
                            .border(1.dp, pColors.border, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Physics",
                                style = Typography.labelMedium,
                                color = pColors.primaryText
                            )
                            Text(
                                text = "11:00 – 12:30",
                                style = Typography.bodySmall,
                                color = pColors.mutedText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pColors.accent)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Mark ✓",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = pColors.background // Dark contrasting text
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 48.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = colors.mutedText,
                    modifier = Modifier.size(16.dp).offset(y = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Changes apply immediately across all screens and widgets without restarting the app.",
                    style = Typography.bodySmall,
                    color = colors.mutedText,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun AppearanceRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBg = if (isSelected) colors.softAccent else colors.elevatedSurface
        val iconColor = if (isSelected) colors.accent else colors.secondaryText
        
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.primaryText
            )
            Text(
                text = subtitle,
                style = Typography.bodySmall,
                color = colors.mutedText
            )
        }
        
        if (isSelected) {
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.background, // Highly legible check mark
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
