package com.adityaram.present.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.notifications.AttendanceCheckTiming
import com.adityaram.present.notifications.UpcomingClassTiming
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar
import kotlinx.coroutines.delay

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory),
    onBack: () -> Unit
) {
    val colors = LocalPresentColors.current
    val context = LocalContext.current
    
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val upcomingClassEnabled by viewModel.upcomingClassEnabled.collectAsState()
    val upcomingClassTiming by viewModel.upcomingClassTiming.collectAsState()
    val attendanceCheckEnabled by viewModel.attendanceCheckEnabled.collectAsState()
    val attendanceCheckTiming by viewModel.attendanceCheckTiming.collectAsState()
    val marginAlertsEnabled by viewModel.marginAlertsEnabled.collectAsState()
    val quietModeEnabled by viewModel.quietModeEnabled.collectAsState()
    val attendanceRequirement by viewModel.attendanceRequirement.collectAsState()

    var hasOsPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasOsPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasOsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "Reminders",
                subtitle = "Choose which reminders Present can send you",
                onBack = onBack
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = Dimens.spacing16,
                        bottom = Dimens.spacing40
                    )
                    .padding(horizontal = Dimens.screenHorizontalPadding)
            ) {
            // Master Switch
            CardRow(
                icon = { BellIcon() },
                title = "Allow notifications",
                subtitle = "Master switch for all on-device reminders",
                isToggled = hasOsPermission && notificationsEnabled,
                onToggle = { 
                    if (!hasOsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && it) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setNotificationsEnabled(it) 
                    }
                }
            )
            
            if (!hasOsPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android notification permission is denied. Enable it in system settings.",
                    style = Typography.bodySmall,
                    color = Color(0xFFF87171),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // REMINDERS
            SectionLabel("REMINDERS")
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            SectionCard {
                SettingsRow(
                    icon = { ClockIcon() },
                    title = "Upcoming class",
                    subtitle = "Remind me before a scheduled class",
                    isToggled = upcomingClassEnabled,
                    onToggle = { viewModel.setUpcomingClassEnabled(it) }
                )
                TimingSelectorRow(
                    options = UpcomingClassTiming.values().map { it.label },
                    selected = upcomingClassTiming,
                    onSelect = { viewModel.setUpcomingClassTiming(it) }
                )
                
                Divider()
                
                SettingsRow(
                    icon = { UserCheckIcon() },
                    title = "Attendance check",
                    subtitle = "Prompt to log status right after class",
                    isToggled = attendanceCheckEnabled,
                    onToggle = { viewModel.setAttendanceCheckEnabled(it) }
                )
                TimingSelectorRow(
                    options = AttendanceCheckTiming.values().map { it.label },
                    selected = attendanceCheckTiming,
                    onSelect = { viewModel.setAttendanceCheckTiming(it) }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // MARGIN ALERTS
            SectionLabel("MARGIN ALERTS")
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            CardRow(
                icon = { WarningIcon() },
                title = "Low margin danger",
                subtitle = "Alert when subject drops near or below target\nCalculated based on $attendanceRequirement% default quota",
                isToggled = marginAlertsEnabled,
                onToggle = { viewModel.setMarginAlertsEnabled(it) }
            )

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // QUIET MODE
            SectionLabel("QUIET MODE")
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            SectionCard {
                SettingsRow(
                    icon = null,
                    title = "Do not disturb",
                    subtitle = "Mute class triggers and log reminders",
                    isToggled = quietModeEnabled,
                    onToggle = { viewModel.setQuietModeEnabled(it) }
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Window",
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (quietModeEnabled) colors.primaryText else colors.mutedText
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "22:00",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (quietModeEnabled) colors.primaryText else colors.mutedText
                        )
                        Text(
                            text = "  to  ",
                            style = Typography.bodyMedium,
                            color = colors.mutedText
                        )
                        Text(
                            text = "07:00",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (quietModeEnabled) colors.primaryText else colors.mutedText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // Removed privacy text regarding Android Alarm Manager
            
            // TEST NOTIFICATION
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable { viewModel.sendTestNotification(context) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VibrationIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send test notification",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primaryText
                    )
                }
            }
        }
    }
}
}

@Composable
private fun SectionLabel(label: String) {
    val colors = LocalPresentColors.current
    Text(
        text = label,
        style = Typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.12.em
        ),
        color = colors.mutedText
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalPresentColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
    ) {
        content()
    }
}

@Composable
private fun Divider() {
    val colors = LocalPresentColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(colors.border)
    )
}

@Composable
private fun CardRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    isToggled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(16.dp),
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
                lineHeight = 16.sp,
                color = colors.mutedText
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        PresentSwitch(checked = isToggled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SettingsRow(
    icon: (@Composable () -> Unit)?,
    title: String,
    subtitle: String,
    isToggled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
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
        }
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
        Spacer(modifier = Modifier.width(14.dp))
        PresentSwitch(checked = isToggled, onCheckedChange = onToggle)
    }
}

@Composable
private fun TimingSelectorRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) colors.elevatedSurface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = Typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (isSelected) colors.accent else colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

@Composable
private fun PresentSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalPresentColors.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.primaryText,
            checkedTrackColor = colors.accent,
            uncheckedThumbColor = colors.primaryText,
            uncheckedTrackColor = colors.elevatedSurface,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

// Icons

@Composable
private fun BellIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.25f, h * 0.35f)
            cubicTo(w * 0.25f, h * 0.1f, w * 0.75f, h * 0.1f, w * 0.75f, h * 0.35f)
            cubicTo(w * 0.75f, h * 0.6f, w * 0.85f, h * 0.75f, w * 0.85f, h * 0.75f)
            lineTo(w * 0.15f, h * 0.75f)
            cubicTo(w * 0.15f, h * 0.75f, w * 0.25f, h * 0.6f, w * 0.25f, h * 0.35f)
            close()
        }
        drawPath(p, color = colors.secondaryText, style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round))
        
        val clapper = Path().apply {
            moveTo(w * 0.42f, h * 0.75f)
            cubicTo(w * 0.42f, h * 0.85f, w * 0.58f, h * 0.85f, w * 0.58f, h * 0.75f)
        }
        drawPath(clapper, color = colors.secondaryText, style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
private fun ClockIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        drawCircle(colors.secondaryText, radius = size.width * 0.4f, style = Stroke(stroke))
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.3f), androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f), androidx.compose.ui.geometry.Offset(size.width * 0.65f, size.height * 0.65f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun UserCheckIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        drawCircle(colors.secondaryText, radius = w * 0.18f, center = androidx.compose.ui.geometry.Offset(w*0.35f, h * 0.32f), style = Stroke(width = stroke))
        val path = Path().apply {
            moveTo(w * 0.05f, h * 0.82f)
            cubicTo(w * 0.05f, h * 0.58f, w * 0.65f, h * 0.58f, w * 0.65f, h * 0.82f)
        }
        drawPath(path, color = colors.secondaryText, style = Stroke(width = stroke))
        
        // Checkmark
        val check = Path().apply {
            moveTo(w * 0.65f, h * 0.6f)
            lineTo(w * 0.75f, h * 0.7f)
            lineTo(w * 0.95f, h * 0.45f)
        }
        drawPath(check, color = colors.secondaryText, style = Stroke(width = stroke, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
private fun WarningIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.9f, h * 0.8f)
            lineTo(w * 0.1f, h * 0.8f)
            close()
        }
        drawPath(p, color = Color(0xFFFBBF24), style = Stroke(width = stroke, join = StrokeJoin.Round))
        drawLine(Color(0xFFFBBF24), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.4f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.6f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(Color(0xFFFBBF24), radius = 1.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.7f))
    }
}

@Composable
private fun ShieldIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.9f, h * 0.25f)
            lineTo(w * 0.9f, h * 0.55f)
            cubicTo(w * 0.9f, h * 0.8f, w * 0.5f, h * 0.95f, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.5f, h * 0.95f, w * 0.1f, h * 0.8f, w * 0.1f, h * 0.55f)
            lineTo(w * 0.1f, h * 0.25f)
            close()
        }
        drawPath(p, color = colors.secondaryText, style = Stroke(width = stroke, join = StrokeJoin.Round))
        val check = Path().apply {
            moveTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.7f, h * 0.35f)
        }
        drawPath(check, color = colors.secondaryText, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun VibrationIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = colors.primaryText,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.15f),
            size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.7f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawLine(colors.primaryText, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.3f), androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.7f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(colors.primaryText, androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.4f), androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.6f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(colors.primaryText, androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.3f), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.7f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(colors.primaryText, androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.4f), androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.6f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
