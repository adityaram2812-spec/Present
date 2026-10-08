package com.adityaram.present.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.PillShape
import com.adityaram.present.ui.theme.Typography
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    listState: LazyListState = rememberLazyListState(),
    onNavigateToTimetable: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current
    val haptic = LocalHapticFeedback.current
    
    var showHolidayConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Dimens.spacing20,
                end = Dimens.spacing20,
                top = 84.dp // Spacer for fixed header (24 padding + 24 icon + 12 padding + 24 spacedBy)
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing24)
        ) {
            if (!uiState.hasTimetable) {
            item {
                HomeEmptyState(onNavigateToTimetable = onNavigateToTimetable)
            }
        } else {
            item {
                AttendanceHero(uiState)
                Spacer(modifier = Modifier.height(Dimens.spacing24))
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.spacing8),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Today's Schedule",
                            style = Typography.headlineSmall, // 18px 600
                            color = colors.primaryText
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text(
                            text = "${uiState.todayClasses.size} classes",
                            style = Typography.bodyMedium,
                            color = colors.mutedText
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Timetable",
                            style = Typography.labelMedium,
                            color = colors.accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.spacing8))
                                .clickable { onNavigateToTimetable() }
                                .padding(4.dp)
                        )
                    }
                }
            }

            if (uiState.todayClasses.isNotEmpty() || uiState.isHoliday) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.spacing16),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val holidayBaseColor = if (uiState.isHoliday) colors.accent else colors.surface
                        val holidayContentColor = if (uiState.isHoliday) colors.background else colors.primaryText
                        val holidayBorder = if (uiState.isHoliday) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.dp, colors.border)
                        
                        Button(
                            onClick = { 
                                if (uiState.isHoliday) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.unmarkHoliday()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Holiday removed")
                                    }
                                } else {
                                    showHolidayConfirm = true 
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = holidayBorder,
                            colors = ButtonDefaults.buttonColors(containerColor = holidayBaseColor, contentColor = holidayContentColor),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                            elevation = null
                        ) {
                            Text("Holiday", style = Typography.labelSmall.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        
                        Button(
                            onClick = { 
                                if (!uiState.isHoliday) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val eligibleCount = uiState.todayClasses.count { it.attendanceStatus != AttendanceStatus.CANCELLED }
                                    viewModel.markBulkAttendance(AttendanceStatus.ABSENT)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar("$eligibleCount lectures marked absent", "Undo")
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.performUndo()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!uiState.isHoliday) colors.surface else colors.surface.copy(alpha=0.5f), 
                                contentColor = if (!uiState.isHoliday) colors.critical else colors.critical.copy(alpha=0.5f)
                            ),
                            border = BorderStroke(1.dp, if (!uiState.isHoliday) colors.border else colors.border.copy(alpha=0.5f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                            elevation = null,
                            enabled = !uiState.isHoliday
                        ) {
                            Text("Mark all absent", style = Typography.labelSmall.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        
                        Button(
                            onClick = { 
                                if (!uiState.isHoliday) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val eligibleCount = uiState.todayClasses.count { it.attendanceStatus != AttendanceStatus.CANCELLED }
                                    viewModel.markBulkAttendance(AttendanceStatus.PRESENT)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar("$eligibleCount lectures marked present", "Undo")
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.performUndo()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!uiState.isHoliday) colors.surface else colors.surface.copy(alpha=0.5f), 
                                contentColor = if (!uiState.isHoliday) colors.safe else colors.safe.copy(alpha=0.5f)
                            ),
                            border = BorderStroke(1.dp, if (!uiState.isHoliday) colors.border else colors.border.copy(alpha=0.5f)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                            elevation = null,
                            enabled = !uiState.isHoliday
                        ) {
                            Text("Mark all present", style = Typography.labelSmall.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            if (showHolidayConfirm) {
                item {
                    AlertDialog(
                        onDismissRequest = { showHolidayConfirm = false },
                        title = { Text("Mark today as a holiday?", color = colors.primaryText) },
                        text = { Text("This will cancel all scheduled lectures for today. No attendance will be recorded.", color = colors.mutedText) },
                        confirmButton = {
                            TextButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.markHoliday()
                                showHolidayConfirm = false
                            }) {
                                Text("Mark Holiday", color = colors.accent)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showHolidayConfirm = false }) {
                                Text("Cancel", color = colors.primaryText)
                            }
                        },
                        containerColor = colors.surface,
                        textContentColor = colors.mutedText,
                        titleContentColor = colors.primaryText
                    )
                }
            }
            
            if (uiState.isHoliday) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacing16), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Holiday", style = Typography.headlineMedium, color = colors.accent)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "No lectures today.", style = Typography.bodyMedium, color = colors.mutedText)
                    }
                }
            } else if (uiState.todayClasses.isEmpty()) {
                item {
                    Text(
                        text = "No scheduled classes for today.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                }
            } else {
                items(
                    items = uiState.todayClasses,
                    key = { it.entryId }
                ) { cls ->
                    ClassCard(
                        model = cls,
                        onMarkAttendance = { status ->
                            viewModel.markAttendance(cls.attendanceRecordId, cls.entryId, cls.subjectId, cls.startTime, status)
                        }
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacing12))
                }
            }

        } // ends !hasTimetable else
            
            item {
                Spacer(modifier = Modifier.height(56.dp))
            }
    } // ends LazyColumn
        
    // Fixed Header Overlay
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(colors.background)
            .padding(
                start = Dimens.spacing20,
                end = Dimens.spacing20,
                top = Dimens.spacing24,
                bottom = Dimens.spacing12
            )
    ) {
        HomeHeader()
    } // ends Column
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    } // ends Box
} // ends HomeScreen

@Composable
fun HomeHeader() {
    val colors = LocalPresentColors.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(24.dp).offset(y = 1.5.dp)) {
            drawCircle(
                color = colors.accent,
                radius = size.minDimension / 2,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = colors.accent,
                radius = size.minDimension / 6
            )
        }
        Spacer(modifier = Modifier.width(Dimens.spacing12))
        Text(
            text = "present",
            style = Typography.headlineLarge.copy(letterSpacing = (-0.03).em),
            color = colors.primaryText
        )
    }
}

@Composable
fun AttendanceHero(uiState: HomeUiState) {
    val colors = LocalPresentColors.current
    
    val ringColor = when {
        uiState.overallPercentage >= 0.85f -> Color(0xFF22C55E)
        uiState.overallPercentage >= 0.75f -> Color(0xFF4ADE80)
        uiState.overallPercentage >= 0.65f -> Color(0xFFFBBF24)
        uiState.overallPercentage >= 0.50f -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.spacing24))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
            .padding(vertical = Dimens.spacing24, horizontal = Dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OVERALL",
                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.17.em),
                color = colors.mutedText
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ATTENDANCE",
                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.17.em),
                color = colors.mutedText
            )
        }
        
        Spacer(modifier = Modifier.height(Dimens.spacing32))
        
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            AttendanceRing(
                percentage = uiState.overallPercentage,
                color = ringColor,
                trackColor = colors.elevatedSurface,
                strokeWidth = 14.dp
            )
            Text(
                text = String.format(java.util.Locale.US, "%.1f%%", uiState.overallPercentage * 100),
                style = Typography.displayLarge.copy(fontSize = 36.sp),
                color = ringColor
            )
        }
        
        Spacer(modifier = Modifier.height(Dimens.spacing32))
        
        val total = uiState.overallPresent + uiState.overallAbsent
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.spacing8),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "${uiState.overallPresent}", style = Typography.headlineMedium, color = colors.primaryText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "PRESENT", style = Typography.labelSmall.copy(letterSpacing = 0.04.em), color = colors.mutedText)
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.border))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "${uiState.overallAbsent}", style = Typography.headlineMedium, color = colors.primaryText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "ABSENT", style = Typography.labelSmall.copy(letterSpacing = 0.04.em), color = colors.mutedText)
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(colors.border))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "$total", style = Typography.headlineMedium, color = colors.primaryText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "TOTAL", style = Typography.labelSmall.copy(letterSpacing = 0.04.em), color = colors.mutedText)
            }
        }
        
        Spacer(modifier = Modifier.height(Dimens.spacing24))
        
        if (uiState.overallRecoveryClassesNeeded != null || uiState.overallClassesCanMiss != null) {
            val msg = if (uiState.overallRecoveryClassesNeeded != null) {
                val count = uiState.overallRecoveryClassesNeeded
                val word = if (count == 1) "class" else "classes"
                "Attend $count more $word to reach ${uiState.attendanceRequirement}%."
            } else {
                val count = uiState.overallClassesCanMiss ?: 0
                val word = if (count == 1) "class" else "classes"
                "You can miss $count $word and stay above ${uiState.attendanceRequirement}%."
            }
            
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.elevatedSurface)
                    .border(1.dp, colors.border, PillShape)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = colors.secondaryText, radius = size.minDimension / 2, style = Stroke(1.5.dp.toPx()))
                        drawCircle(color = colors.secondaryText, radius = size.minDimension / 5)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = msg,
                        style = Typography.labelSmall,
                        color = colors.secondaryText,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceRing(
    percentage: Float, 
    color: Color, 
    trackColor: Color,
    strokeWidth: androidx.compose.ui.unit.Dp = 8.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * percentage,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SmartInsight(insight: String) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.spacing16))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing16))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = colors.primaryText,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.spacing12))
        Text(
            text = insight,
            style = Typography.bodyMedium,
            color = colors.secondaryText
        )
    }
}

@Composable
fun ClassCard(
    model: ClassUiModel,
    onMarkAttendance: (AttendanceStatus) -> Unit
) {
    val colors = LocalPresentColors.current
    
    val formatTime = { minutes: Int ->
        val h = minutes / 60
        val m = minutes % 60
        "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.elevatedSurface) // In Stitch design it looks elevated over the Bg
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(Dimens.spacing16)
    ) {
        Column {
            if (model.isTemporary && model.temporaryLabel != null) {
                Row(
                    modifier = Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(6.dp)).background(colors.accent.copy(alpha=0.15f)).padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = colors.accent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(model.temporaryLabel, style = Typography.labelSmall, color = colors.accent)
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.subjectName,
                    style = Typography.headlineMedium,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f).padding(end = Dimens.spacing8)
                )
                if (model.isNext) {
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    val currentMins = java.time.LocalTime.now().hour * 60 + java.time.LocalTime.now().minute
                    val diff = model.startTime - currentMins
                    val nextText = if (diff in 1..120) "Next class · In ${diff}m" else "Next class"
                    
                    Row(
                        Modifier
                            .clip(PillShape)
                            .background(colors.softAccent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = nextText,
                            style = Typography.labelSmall,
                            color = colors.secondaryText
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.spacing4))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${formatTime(model.startTime)}  ·  ${model.room ?: "TBA"}",
                    style = Typography.bodyMedium,
                    color = colors.mutedText,
                    modifier = Modifier.weight(1f).padding(end = Dimens.spacing8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = String.format(java.util.Locale.US, "%.1f%%", model.subjectPercentage * 100),
                    style = Typography.labelMedium,
                    color = colors.mutedText
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.spacing16))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)
            ) {
                val isPresent = model.attendanceStatus == AttendanceStatus.PRESENT
                val isAbsent = model.attendanceStatus == AttendanceStatus.ABSENT
                val haptic = LocalHapticFeedback.current
    
    var showBulkMenu by remember { mutableStateOf(false) }
    var showBulkAbsentConfirm by remember { mutableStateOf(false) }

                val pSource = remember { MutableInteractionSource() }
                val pPressed by pSource.collectIsPressedAsState()
                
                val pScale by animateFloatAsState(
                    targetValue = if (pPressed) 0.96f else 1f,
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                )
                val pBgColor by animateColorAsState(
                    targetValue = if (isPresent) colors.safe.copy(alpha = 0.15f) else colors.surface,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )
                val pIconColor by animateColorAsState(
                    targetValue = if (isPresent) colors.safe else colors.primaryText,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )
                val pBorderColor by animateColorAsState(
                    targetValue = if (isPresent) Color.Transparent else colors.border,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pBgColor)
                        .border(1.dp, pBorderColor, RoundedCornerShape(12.dp))
                        .scale(pScale)
                        .clickable(interactionSource = pSource, indication = null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMarkAttendance(AttendanceStatus.PRESENT)
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        null,
                        tint = pIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Present",
                        style = Typography.labelLarge,
                        color = pIconColor
                    )
                }
                
                val aSource = remember { MutableInteractionSource() }
                val aPressed by aSource.collectIsPressedAsState()
                
                val aScale by animateFloatAsState(
                    targetValue = if (aPressed) 0.96f else 1f,
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                )
                val aBgColor by animateColorAsState(
                    targetValue = if (isAbsent) colors.critical.copy(alpha = 0.15f) else colors.surface,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )
                val aIconColor by animateColorAsState(
                    targetValue = if (isAbsent) colors.critical else colors.primaryText,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )
                val aBorderColor by animateColorAsState(
                    targetValue = if (isAbsent) Color.Transparent else colors.border,
                    animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(aBgColor)
                        .border(1.dp, aBorderColor, RoundedCornerShape(12.dp))
                        .scale(aScale)
                        .clickable(interactionSource = aSource, indication = null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMarkAttendance(AttendanceStatus.ABSENT)
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        null,
                        tint = aIconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Absent",
                        style = Typography.labelLarge,
                        color = aIconColor
                    )
                }
            }
            
            if (model.isAttendanceLow) {
                Spacer(modifier = Modifier.height(Dimens.spacing16))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Info, 
                        contentDescription = null, 
                        tint = colors.critical, 
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Attendance low for this subject.",
                        style = Typography.labelMedium,
                        color = colors.critical
                    )
                }
            }
        }
    }
}

@Composable
fun HomeEmptyState(onNavigateToTimetable: () -> Unit = {}) {
    val colors = LocalPresentColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.spacing24))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(Dimens.spacing24))
            .padding(Dimens.spacing24)
    ) {
        Column {
            Text(text = "No timetable is set up", style = Typography.headlineLarge, color = colors.primaryText)
            Spacer(modifier = Modifier.height(Dimens.spacing8))
            Text(text = "Get started by adding your classes.", style = Typography.bodyMedium, color = colors.secondaryText)
            Spacer(modifier = Modifier.height(Dimens.spacing24))
            
            Button(
                onClick = onNavigateToTimetable,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF101012)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Import timetable", style = Typography.labelLarge)
            }
            
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            
            OutlinedButton(
                onClick = onNavigateToTimetable,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primaryText),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add manually", style = Typography.labelLarge)
            }
        }
    }
}
