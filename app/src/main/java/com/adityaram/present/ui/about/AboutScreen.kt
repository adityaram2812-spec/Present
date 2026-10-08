package com.adityaram.present.ui.about

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.adityaram.present.BuildConfig
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.components.PresentTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToLicenses: () -> Unit
) {
    val colors = LocalPresentColors.current
    val scrollState = rememberScrollState()
    var easterEggTrigger by remember { mutableLongStateOf(0L) }
    var easterEggColor by remember { mutableStateOf(colors.accent) }

    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "About Present",
                onBack = onBack,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.elevatedSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v$versionName ($versionCode)",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.secondaryText
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = Dimens.spacing40)
                    .padding(top = Dimens.spacing24)
            ) {

            // 1. BRAND HERO
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Present Logo Native Drawing
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(40.dp)) {
                        drawCircle(
                            color = colors.accent,
                            radius = size.minDimension / 2,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawCircle(
                            color = colors.accent,
                            radius = size.minDimension / 6
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "present",
                    style = Typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.02).em
                    ),
                    color = colors.primaryText
                )
                
                Text(
                    text = "Know where you stand.",
                    style = Typography.bodyLarge,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Version $versionName  ·  Build $versionCode  ·  Stable",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Present is a simple attendance tracker designed to help you know where you stand, without the clutter.",
                    style = Typography.bodyMedium,
                    color = colors.secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. ON-DEVICE STORAGE CARD
            Column(
                modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .padding(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.elevatedSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val w = size.width
                            val h = size.height
                            drawRoundRect(
                                color = Color(0xFF10B981), // Safe Green Accent
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.2f),
                                size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.6f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawLine(Color(0xFF10B981), androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.4f), androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.4f), strokeWidth = 1.5.dp.toPx())
                            drawCircle(Color(0xFF10B981), radius = 1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.6f))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "On-Device Storage",
                            style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your attendance data stays on your device. Present only uses secure cloud services when you choose account-based features such as automatic timetable extraction.",
                            style = Typography.bodySmall,
                            color = colors.secondaryText,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. MORE SECTION
                Text(
                    text = "MORE",
                    style = Typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.12.em
                    ),
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                ) {
                    MoreRow(
                        icon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.secondaryText) },
                        title = "Privacy",
                        subtitle = "Learn how Present handles your data",
                        onClick = onNavigateToPrivacy,
                        showDivider = true,
                        colors = colors
                    )
                    MoreRow(
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.secondaryText) },
                        title = "Help & feedback",
                        subtitle = "Questions, feedback or something not working?",
                        onClick = onNavigateToHelp,
                        showDivider = false,
                        colors = colors
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // 4. FOOTER LINKS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Privacy policy",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.mutedText,
                        modifier = Modifier.clickable { onNavigateToPrivacy() }
                    )
                    Text("  ·  ", style = Typography.labelSmall, color = colors.border)
                    Text(
                        text = "Terms of use",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.mutedText,
                        modifier = Modifier.clickable { onNavigateToTerms() }
                    )
                    Text("  ·  ", style = Typography.labelSmall, color = colors.border)
                    Text(
                        text = "Open-source licenses",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = colors.mutedText,
                        modifier = Modifier.clickable { onNavigateToLicenses() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. FOOTER CREDIT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Made with ", style = Typography.labelSmall, color = colors.mutedText)
                    Canvas(modifier = Modifier.size(10.dp)) {
                        val cw = size.width
                        val ch = size.height
                        val path = Path().apply {
                            moveTo(cw / 2, ch / 5)
                            cubicTo(cw * 5 / 14, 0f, 0f, cw / 15, cw / 28, ch * 2 / 5)
                            cubicTo(cw / 14, ch * 2 / 3, cw * 3 / 7, ch * 5 / 6, cw / 2, ch)
                            cubicTo(cw * 4 / 7, ch * 5 / 6, cw * 13 / 14, ch * 2 / 3, cw * 27 / 28, ch * 2 / 5)
                            cubicTo(cw, cw / 15, cw * 9 / 14, 0f, cw / 2, ch / 5)
                            close()
                        }
                        drawPath(path = path, color = colors.accent)
                    }
                    Text(" by ", style = Typography.labelSmall, color = colors.mutedText)
                    Text(
                        text = "Aditya Ram", 
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                        color = colors.secondaryText,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            easterEggColor = colors.accent
                            easterEggTrigger = System.currentTimeMillis()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Special thanks to ", style = Typography.labelSmall.copy(fontSize = 11.sp), color = colors.mutedText)
                    Text(
                        text = "Vidhi Boricha",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = colors.secondaryText,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            easterEggColor = Color(0xFFEF4444)
                            easterEggTrigger = System.currentTimeMillis()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
        
        // 6. EASTER EGG OVERLAY (Z-Index above everything)
        if (easterEggTrigger != 0L) {
            Box(modifier = Modifier.fillMaxSize().zIndex(100f)) {
                HeartEasterEggOverlay(
                    trigger = easterEggTrigger, 
                    accentColor = easterEggColor,
                    onComplete = { easterEggTrigger = 0L }
                )
            }
        }
    }
}

@Composable
private fun MoreRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    colors: com.adityaram.present.ui.theme.PresentColors
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.elevatedSurface),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.primaryText
                )
                Text(
                    text = subtitle,
                    style = Typography.bodySmall,
                    color = colors.mutedText
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(20.dp)
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 70.dp, end = 16.dp)
                    .height(0.5.dp)
                    .background(colors.border)
            )
        }
    }
}
