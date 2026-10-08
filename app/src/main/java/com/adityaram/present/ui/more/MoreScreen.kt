package com.adityaram.present.ui.more

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.ui.home.HomeHeader
import com.adityaram.present.ui.theme.*

@Composable
fun MoreScreen(
    viewModel: MoreViewModel = viewModel(factory = MoreViewModel.Factory),
    scrollState: ScrollState = rememberScrollState(),
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToSubjects: () -> Unit = {},
    onNavigateToTimetable: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToThreshold: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToLeavePlanner: () -> Unit = {},
    onNavigateToTemporaryLectures: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    top = 84.dp, // Space for fixed header
                    bottom = Dimens.spacing40
                )
                .padding(horizontal = Dimens.screenHorizontalPadding)
        ) {

            // ── TOOLS ──────────────────────────────────────────────────
            SectionLabel("TOOLS")
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            SectionCard {
                MoreRow(
                    icon = { CalendarIcon() },
                    title = "Calendar",
                    subtitle = "View attendance by date",
                    onClick = { onNavigateToCalendar() }
                )
                MoreRow(
                    icon = { AnalyticsIcon() },
                    title = "Analytics",
                    subtitle = "See attendance trends & streaks",
                    onClick = { onNavigateToAnalytics() }
                )
                MoreRow(
                    icon = { PlannerIcon() },
                    title = "Attendance Planner",
                    subtitle = "Simulate your classes",
                    onClick = { onNavigateToPlanner() },
                    showDivider = true
                )
                MoreRow(
                    icon = { CalendarIcon() }, // Reuse calendar icon for Leave Planner visually fits
                    title = "Leave Planner",
                    subtitle = "Plan your upcoming leave",
                    onClick = { onNavigateToLeavePlanner() },
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // ── MANAGE ─────────────────────────────────────────────────
            SectionLabel("MANAGE")
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            SectionCard {
                MoreRow(
                    icon = { SubjectsIcon() },
                    title = "Subjects",
                    subtitle = "Manage course thresholds",
                    trailingBadge = "${uiState.activeSubjectCount} active",
                    onClick = { onNavigateToSubjects() }
                )
                MoreRow(
                    icon = { TimetableIcon() },
                    title = "Timetable",
                    subtitle = "Weekly slots & class periods",
                    onClick = { onNavigateToTimetable() },
                    showDivider = true
                )
                MoreRow(
                    icon = { CalendarIcon() },
                    title = "Temporary Lectures",
                    subtitle = "Add class substitutions & extras",
                    onClick = { onNavigateToTemporaryLectures() },
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // ── PREFERENCES ────────────────────────────────────────────
            SectionLabel("PREFERENCES")
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            SectionCard {
                MoreRow(
                    icon = { RemindersIcon() },
                    title = "Reminders",
                    subtitle = "Morning briefing & class alerts",
                    onClick = { onNavigateToNotifications() }
                )
                MoreRow(
                    icon = { AppearanceIcon() },
                    title = "Appearance",
                    subtitle = "Dark, light, or follow system",
                    trailingValue = uiState.appearanceLabel,
                    onClick = { onNavigateToAppearance() }
                )
                MoreRow(
                    icon = { ThresholdIcon() },
                    title = "Default Threshold",
                    subtitle = "Academic attendance requirement",
                    trailingValue = uiState.thresholdLabel,
                    onClick = { onNavigateToThreshold() },
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing32))

            // ── ACCOUNT ────────────────────────────────────────────────
            SectionLabel("ACCOUNT")
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            SectionCard {
                MoreRow(
                    icon = { AccountIcon() },
                    title = "Account",
                    subtitle = "Show account and timetable import access",
                    onClick = { onNavigateToAccount() },
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing40))

            // ── DEBUG (ONLY IN DEV) ────────────────────────────────────────────────
            if (com.adityaram.present.BuildConfig.DEBUG) {
                SectionLabel("DEBUG & DEVELOPMENT")
                Spacer(modifier = Modifier.height(Dimens.spacing12))
                SectionCard {
                    MoreRow(
                        icon = { RemindersIcon() },
                        title = "Reset Onboarding",
                        subtitle = "Mark onboarding incomplete (Requires app restart)",
                        onClick = { viewModel.resetOnboarding() },
                        showDivider = false
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spacing40))
            }

            // ── FOOTER ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToAbout() }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "About Present",
                    style = Typography.labelLarge,
                    color = colors.accent
                )
                Spacer(modifier = Modifier.height(Dimens.spacing16))
                Text(
                    text = "Version 1.0 (Build 104) · Made for students",
                    style = Typography.labelSmall,
                    color = colors.mutedText
                )
                Spacer(modifier = Modifier.height(Dimens.spacing4))
                Text(
                    text = "No account required · Stored strictly on-device",
                    style = Typography.labelSmall,
                    color = colors.mutedText
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacing16))
        }

        // ── Fixed Header ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .background(colors.background)
                .padding(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                    top = Dimens.spacing24,
                    bottom = Dimens.spacing12
                )
        ) {
            HomeHeader()
        }
    }
}

// ── Reusable components ─────────────────────────────────────────────────

@Composable
private fun IdentityCard() {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.elevatedSurface),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                drawCircle(
                    color = colors.accent,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = colors.accent,
                    radius = size.minDimension / 6
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "present",
                    style = Typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = (-0.02).em
                    ),
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "v1.0",
                    style = Typography.labelSmall,
                    color = colors.mutedText
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Offline-first attendance utility",
                style = Typography.bodySmall,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        // Local badge
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(colors.softAccent)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Local",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accent
                )
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
private fun MoreRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailingBadge: String? = null,
    trailingValue: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val colors = LocalPresentColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
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

            // Text
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

            // Trailing badge or value
            if (trailingBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(colors.elevatedSurface)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = trailingBadge,
                        style = Typography.labelSmall,
                        color = colors.secondaryText
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (trailingValue != null) {
                Text(
                    text = trailingValue,
                    style = Typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.secondaryText
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Chevron
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

// ── Lucide-style outline icons ──────────────────────────────────────────

@Composable
private fun CalendarIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val s = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val r = 3.dp.toPx()
        drawRoundRect(
            color = colors.secondaryText,
            topLeft = androidx.compose.ui.geometry.Offset(s, s + 2.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(w - 2 * s, h - 2 * s - 2.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Top pegs
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.33f, s), androidx.compose.ui.geometry.Offset(w * 0.33f, s + 3.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.67f, s), androidx.compose.ui.geometry.Offset(w * 0.67f, s + 3.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        // Horizontal divider
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(s, h * 0.45f), androidx.compose.ui.geometry.Offset(w - s, h * 0.45f), strokeWidth = 1.5.dp.toPx())
    }
}

@Composable
private fun AnalyticsIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Three vertical bars
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.7f), androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.3f), strokeWidth = stroke)
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.7f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.15f), strokeWidth = stroke)
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.7f), androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.45f), strokeWidth = stroke)
        // Base line
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.75f), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.75f), strokeWidth = stroke)
    }
}

@Composable
private fun PlannerIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val radius = w * 0.38f
        drawCircle(colors.secondaryText, radius = radius, style = Stroke(width = stroke))
        // Crosshair
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(cx, cy - radius * 0.6f), androidx.compose.ui.geometry.Offset(cx, cy + radius * 0.6f), strokeWidth = stroke)
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(cx - radius * 0.6f, cy), androidx.compose.ui.geometry.Offset(cx + radius * 0.6f, cy), strokeWidth = stroke)
        drawCircle(colors.secondaryText, radius = 1.5.dp.toPx())
    }
}

@Composable
private fun SubjectsIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Book shape
        val r = 2.dp.toPx()
        drawRoundRect(
            color = colors.secondaryText,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.76f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style = Stroke(width = stroke)
        )
        // Spine
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.12f), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.88f), strokeWidth = stroke)
    }
}

@Composable
private fun TimetableIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Grid
        val r = 2.5.dp.toPx()
        drawRoundRect(
            color = colors.secondaryText,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            style = Stroke(width = stroke)
        )
        // Horizontal lines
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.4f), androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.4f), strokeWidth = stroke)
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.65f), androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.65f), strokeWidth = stroke)
        // Vertical lines
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.37f, h * 0.1f), androidx.compose.ui.geometry.Offset(w * 0.37f, h * 0.9f), strokeWidth = stroke)
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.63f, h * 0.1f), androidx.compose.ui.geometry.Offset(w * 0.63f, h * 0.9f), strokeWidth = stroke)
    }
}

@Composable
private fun RemindersIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.35f)
            cubicTo(w * 0.25f, h * 0.1f, w * 0.75f, h * 0.1f, w * 0.75f, h * 0.35f)
            cubicTo(w * 0.75f, h * 0.6f, w * 0.85f, h * 0.75f, w * 0.85f, h * 0.75f)
            lineTo(w * 0.15f, h * 0.75f)
            cubicTo(w * 0.15f, h * 0.75f, w * 0.25f, h * 0.6f, w * 0.25f, h * 0.35f)
            close()
        }
        drawPath(p, color = colors.secondaryText, style = Stroke(width = stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        
        val clapper = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.42f, h * 0.75f)
            cubicTo(w * 0.42f, h * 0.85f, w * 0.58f, h * 0.85f, w * 0.58f, h * 0.75f)
        }
        drawPath(clapper, color = colors.secondaryText, style = Stroke(width = stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
private fun AppearanceIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val r = w * 0.35f
        
        drawCircle(
            color = colors.secondaryText,
            radius = r,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = Stroke(width = stroke)
        )
        
        drawArc(
            color = colors.secondaryText,
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(cx - r, cy - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }
}

@Composable
private fun ThresholdIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Gauge / speedometer
        drawArc(
            color = colors.secondaryText,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = stroke),
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.7f)
        )
        // Needle
        drawLine(colors.secondaryText, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.55f), androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.35f), strokeWidth = stroke)
        drawCircle(colors.secondaryText, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.55f))
    }
}

@Composable
private fun AccountIcon() {
    val colors = LocalPresentColors.current
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        val cx = w / 2
        // Head
        drawCircle(colors.secondaryText, radius = w * 0.18f, center = androidx.compose.ui.geometry.Offset(cx, h * 0.32f), style = Stroke(width = stroke))
        // Body arc
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.82f)
            cubicTo(w * 0.2f, h * 0.58f, w * 0.8f, h * 0.58f, w * 0.8f, h * 0.82f)
        }
        drawPath(path, color = colors.secondaryText, style = Stroke(width = stroke))
    }
}
