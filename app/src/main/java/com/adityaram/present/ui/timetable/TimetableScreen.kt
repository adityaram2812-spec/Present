package com.adityaram.present.ui.timetable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.PillShape
import com.adityaram.present.ui.theme.PresentColors
import com.adityaram.present.ui.theme.Typography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel = viewModel(factory = TimetableViewModel.Factory),
    listState: LazyListState = rememberLazyListState(),
    onAddClassClick: () -> Unit = {},
    onImportClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current
    var showAddClassSheet by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<TimetableClassUiModel?>(null) }

    if (showAddClassSheet || classToEdit != null) {
        val editingEntry = classToEdit
        AddClassBottomSheet(
            onDismissRequest = { 
                showAddClassSheet = false
                classToEdit = null
            },
            subjects = uiState.subjects,
            initialDate = uiState.selectedDate,
            classToEdit = editingEntry,
            onSave = { subjectName, day, start, end, room, teacher, isRecurring, date ->
                if (editingEntry != null) {
                    viewModel.updateClass(editingEntry.entry.id, subjectName, day, start, end, room, teacher, isRecurring, date)
                } else {
                    viewModel.addClass(subjectName, day, start, end, room, teacher, isRecurring, date)
                }
                showAddClassSheet = false
                classToEdit = null
            }
        )
    }

    var duplicateCollisionTarget by remember { mutableStateOf<TimetableClassUiModel?>(null) }
    var classToDelete by remember { mutableStateOf<TimetableClassUiModel?>(null) }

    if (classToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { classToDelete = null },
            title = { Text("Delete Class") },
            text = { Text("Are you sure you want to completely delete this class? Historical attendance records will not be deleted.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    viewModel.deleteClass(classToDelete!!)
                    classToDelete = null
                }) { Text("Delete", color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { classToDelete = null }) { Text("Cancel") }
            },
            containerColor = colors.surface,
            titleContentColor = colors.primaryText,
            textContentColor = colors.mutedText
        )
    }

    if (duplicateCollisionTarget != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { duplicateCollisionTarget = null },
            title = { Text("Exact Collision Detected") },
            text = { Text("Another class already exists at this exact time. Are you sure you want to duplicate it and create overlapping classes?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    viewModel.duplicateClass(duplicateCollisionTarget!!)
                    duplicateCollisionTarget = null
                }) { Text("Duplicate Anyway") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { duplicateCollisionTarget = null }) { Text("Cancel") }
            },
            containerColor = colors.surface,
            titleContentColor = colors.primaryText,
            textContentColor = colors.mutedText
        )
    }

    var cancelOccurrenceTarget by remember { mutableStateOf<TimetableClassUiModel?>(null) }

    if (cancelOccurrenceTarget != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { cancelOccurrenceTarget = null },
            title = { Text("Are you sure you want to cancel this occurrence?") },
            text = { Text("This will cancel this class only for the selected date. Your recurring timetable will remain unchanged.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    viewModel.cancelClassOccurrence(cancelOccurrenceTarget!!, uiState.selectedDate)
                    cancelOccurrenceTarget = null
                }) { Text("Cancel occurrence") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { cancelOccurrenceTarget = null }) { Text("Keep class") }
            },
            containerColor = colors.surface,
            titleContentColor = colors.primaryText,
            textContentColor = colors.mutedText
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.spacing20,
                        end = Dimens.spacing20,
                        top = Dimens.spacing24,
                        bottom = Dimens.spacing12
                    )
            ) {
                TimetableHeader(colors = colors)
                Spacer(modifier = Modifier.height(Dimens.spacing24))
                HorizontalDaySelector(
                    weekDates = uiState.weekDates,
                    selectedDate = uiState.selectedDate,
                    onDateSelect = { viewModel.selectDate(it) },
                    colors = colors
                )
                Spacer(modifier = Modifier.height(Dimens.spacing24))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                    // Add Class Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Dimens.spacing12))
                            .background(colors.accent)
                            .clickable(onClick = { showAddClassSheet = true })
                            .padding(vertical = Dimens.spacing12),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Class",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text(
                            text = "Add class",
                            style = Typography.labelLarge,
                            color = Color.White
                        )
                    }
                    
                    // Import Button
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Dimens.spacing12))
                            .background(colors.surface)
                            .clickable(onClick = onImportClick)
                            .padding(vertical = Dimens.spacing12),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Import",
                            tint = colors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text(
                            text = "Import",
                            style = Typography.labelLarge,
                            color = colors.primaryText
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.border, thickness = 1.dp)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                start = Dimens.spacing20,
                end = Dimens.spacing20,
                top = Dimens.spacing4,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacing12)
        ) {

        item {
            val totalMinutes = uiState.classes.filter { !it.isCancelled }.sumOf { 
                it.entry.endTime - it.entry.startTime 
            }
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            val totalTimeStr = if (hours > 0) "${hours}h ${mins}m total" else "${mins}m total"
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spacing8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dayName = uiState.selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    Text(
                        text = "$dayName Schedule",
                        style = Typography.headlineSmall, // 18px 600
                        color = colors.primaryText
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    Text(
                        text = "•",
                        style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacing8))
                    Text(
                        text = "${uiState.classes.size} sessions",
                        style = Typography.labelMedium,
                        color = colors.mutedText
                    )
                }
                Text(
                    text = totalTimeStr,
                    style = Typography.labelMedium,
                    color = colors.mutedText
                )
            }
        }

        if (uiState.classes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacing32),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scheduled classes for this day.",
                        style = Typography.bodyMedium,
                        color = colors.mutedText
                    )
                }
            }
        } else {
            items(uiState.classes) { cls ->
                TimetableClassCard(
                    model = cls,
                    selectedDate = uiState.selectedDate,
                    colors = colors,
                    onEditClick = { classToEdit = cls },
                    onDuplicateClick = {
                        val collision = uiState.classes.count { it.entry.startTime == cls.entry.startTime } > 1
                        if (collision) {
                            duplicateCollisionTarget = cls
                        } else {
                            viewModel.duplicateClass(cls)
                        }
                    },
                    onCancelOccurrenceClick = { cancelOccurrenceTarget = cls },
                    onRestoreOccurrenceClick = { viewModel.restoreClassOccurrence(cls, uiState.selectedDate) },
                    onDeleteClick = { classToDelete = cls }
                )
            }
        }
    }
}
}

@Composable
private fun TimetableHeader(colors: PresentColors) {
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
private fun HorizontalDaySelector(
    weekDates: List<LocalDate>,
    selectedDate: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    colors: PresentColors
) {
    val formatter = DateTimeFormatter.ofPattern("d")
    
    // Day Selector Card
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.spacing16))
            .background(colors.surface)
            .padding(vertical = Dimens.spacing12, horizontal = Dimens.spacing8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDates.forEach { date ->
            val isSelected = date == selectedDate
            val dayInitial = date.dayOfWeek.name.take(3)
            val isToday = date == LocalDate.now()
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(Dimens.spacing12))
                    .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onDateSelect(date) }
                    .padding(vertical = Dimens.spacing8)
            ) {
                Text(
                    text = dayInitial,
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) colors.accent else colors.mutedText,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = date.format(formatter),
                    style = Typography.headlineMedium,
                    color = if (isSelected) colors.accent else colors.primaryText
                )
                // Dot indicator for classes (We'll assume they have classes for UI, can be optimized later)
                Box(
                    modifier = Modifier
                        .padding(top = Dimens.spacing4)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isToday) colors.accent else colors.border) 
                )
            }
        }
    }
}

@Composable
private fun TimetableClassCard(
    model: TimetableClassUiModel,
    selectedDate: LocalDate,
    colors: PresentColors,
    onEditClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onCancelOccurrenceClick: () -> Unit,
    onRestoreOccurrenceClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    val formatTime = { minutes: Int ->
        val h = minutes / 60
        val m = minutes % 60
        String.format(Locale.getDefault(), "%02d:%02d", h, m)
    }
    
    val timeSpanStr = "${formatTime(model.entry.startTime)} - ${formatTime(model.entry.endTime)}"
    val durationMinutes = model.entry.endTime - model.entry.startTime
    
    val baseCardBg = colors.surface
    // If NEXT class, apply purple left-border natively via row or canvas
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.spacing16))
            .background(baseCardBg)
    ) {
        if (model.isNext) {
            // Draw the left accent border line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(colors.accent)
                    .align(Alignment.CenterStart)
            )
        }
        
        Column(
            modifier = Modifier.padding(Dimens.spacing16)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (model.isExtraClass) {
                        Text(
                            text = selectedDate.dayOfWeek.name.uppercase(), // Using week reference
                            style = Typography.labelSmall.copy(letterSpacing = 0.05.em),
                            color = colors.mutedText
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text(
                            text = timeSpanStr,
                            style = Typography.labelSmall.copy(letterSpacing = 0.05.em, textDecoration = if (model.isCancelled) TextDecoration.LineThrough else TextDecoration.None),
                            color = if (model.isCancelled) colors.mutedText.copy(alpha = 0.5f) else colors.mutedText
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text(text = "•", style = Typography.labelSmall, color = colors.mutedText)
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text(text = "${durationMinutes} min", style = Typography.labelSmall, color = colors.mutedText)
                    } else {
                        Text(
                            text = timeSpanStr,
                            style = Typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.05.em,
                                textDecoration = if (model.isCancelled) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (model.isCancelled) colors.mutedText.copy(alpha = 0.5f) else colors.mutedText
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        Text(text = "•", style = Typography.labelSmall, color = colors.mutedText)
                        Spacer(modifier = Modifier.width(Dimens.spacing8))
                        
                        if (model.isNext) {
                            Text(
                                text = "In 24m", // Placeholder for actual math if needed
                                style = Typography.labelSmall,
                                color = colors.mutedText
                            )
                        } else if (model.isCancelled) {
                             Text(
                                text = "Cancelled today", 
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFFEF4444)
                            )
                        } else {
                            Text(text = "${durationMinutes} min", style = Typography.labelSmall, color = colors.mutedText)
                        }
                    }
                }
                
                // Top Right Badges
                if (model.isNext) {
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.accent.copy(alpha = 0.15f))
                            .border(1.dp, colors.accent.copy(alpha = 0.3f), PillShape)
                            .padding(horizontal = Dimens.spacing12, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NEXT",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.05.em),
                            color = colors.accent
                        )
                    }
                } else if (model.isCancelled) {
                     Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.spacing8))
                            .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                            .padding(horizontal = Dimens.spacing8, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "One-time",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFEF4444)
                        )
                    }
                } else if (model.attendanceStatus == AttendanceStatus.PRESENT) {
                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.3f), PillShape)
                            .padding(horizontal = Dimens.spacing12, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Present",
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Present",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF22C55E)
                        )
                    }
                } else if (model.isExtraClass) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacing8)) {
                        Text(
                            text = "Extra class",
                            style = Typography.labelSmall,
                            color = colors.primaryText,
                            modifier = Modifier.clip(PillShape).background(colors.accent.copy(alpha=0.2f)).padding(horizontal=10.dp, vertical=4.dp)
                        )
                         Text(
                            text = "One-time",
                            style = Typography.labelSmall,
                            color = colors.mutedText,
                             modifier = Modifier.padding(top=4.dp)
                        )
                    }
                } else {
                     Text(
                        text = "Scheduled",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.mutedText
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.spacing4))
            
            Text(
                text = model.subjectName,
                style = Typography.headlineMedium.copy(
                    textDecoration = if (model.isCancelled) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (model.isCancelled) colors.mutedText.copy(alpha = 0.5f) else colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimens.spacing12))
            
            // Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (model.entry.room != null) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Room",
                            tint = colors.mutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = model.entry.room,
                            style = Typography.labelMedium,
                            color = colors.mutedText
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacing12))
                    }
                    if (model.entry.teacher != null) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Teacher",
                            tint = colors.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = model.entry.teacher,
                            style = Typography.labelMedium,
                            color = colors.mutedText
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showOverflowMenu = true }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = colors.mutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    androidx.compose.material3.DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Edit class") },
                            onClick = {
                                showOverflowMenu = false
                                onEditClick()
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { 
                                showOverflowMenu = false
                                onDuplicateClick()
                            }
                        )
                        if (model.isCancelled) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Restore occurrence") },
                                onClick = { 
                                    showOverflowMenu = false
                                    onRestoreOccurrenceClick()
                                }
                            )
                        } else {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Cancel this occurrence") },
                                onClick = { 
                                    showOverflowMenu = false
                                    onCancelOccurrenceClick()
                                }
                            )
                        }
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Delete class", color = Color(0xFFEF4444)) },
                            onClick = { 
                                showOverflowMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
            
            if (model.isCancelled) {
                Spacer(modifier = Modifier.height(Dimens.spacing12))
                Text(
                    text = "One-time cancellation · Recurring schedule unaffected",
                    style = Typography.bodySmall,
                    color = colors.mutedText.copy(alpha = 0.6f)
                )
            }
        }
    }
}
