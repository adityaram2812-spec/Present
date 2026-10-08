package com.adityaram.present.ui.temporary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TemporaryLectureType
import com.adityaram.present.data.model.TimetableEntry
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.PillShape
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporaryLecturesScreen(
    viewModel: TemporaryLecturesViewModel = viewModel(factory = TemporaryLecturesViewModel.Factory),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "Temporary Lectures",
                subtitle = "Manage date-specific overrides",
                onBack = onBack
            )
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                top = 20.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.temporaryLectures.isEmpty() && !uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No temporary lectures", style = Typography.titleMedium, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add an override for a specific date", style = Typography.bodyMedium, color = colors.secondaryText)
                    }
                }
            } else {
                items(uiState.temporaryLectures) { item ->
                    TemporaryLectureCard(
                        model = item,
                        onDelete = { viewModel.deleteTemporaryLecture(item.entity) }
                    )
                }
            }
        }
    }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = colors.accent,
            contentColor = colors.surface
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Temporary Lecture")
        }

    if (showCreateDialog) {
        CreateTemporaryLectureDialog(
            uiState = uiState,
            onDismiss = { showCreateDialog = false },
            onSave = { date, start, end, sub, teacher, room, type, replaceId ->
                viewModel.addTemporaryLecture(date, start, end, sub, teacher, room, type, replaceId)
                showCreateDialog = false
            }
        )
    }
}
}

@Composable
fun TemporaryLectureCard(model: TemporaryLectureUiModel, onDelete: () -> Unit) {
    val colors = LocalPresentColors.current
    
    val isReplacement = model.entity.type == TemporaryLectureType.REPLACEMENT
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = model.dateText, style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.accent, modifier = Modifier.padding(bottom = 4.dp))
                
                Text(text = model.timeText, style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 2.dp))
                
                Text(text = model.subjectName, style = Typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val pipText = if (isReplacement) {
                    "Replaces ${model.resolvesToTargetName}"
                } else {
                    "Extra lecture"
                }
                
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.elevatedSurface).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(pipText, style = Typography.labelSmall, color = colors.primaryText)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = colors.mutedText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemporaryLectureDialog(
    uiState: TemporaryLecturesUiState,
    onDismiss: () -> Unit,
    onSave: (Long, Int, Int, Long, String?, String?, TemporaryLectureType, Long?) -> Unit
) {
    val colors = LocalPresentColors.current
    
    var type by remember { mutableStateOf(TemporaryLectureType.ADDITION) }
    
    val datePickerState = rememberDatePickerState()
    
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedReplacesId by remember { mutableStateOf<Long?>(null) }
    
    var startHour by remember { mutableStateOf(10) }
    var startMin by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(11) }
    var endMin by remember { mutableStateOf(0) }
    
    var roomText by remember { mutableStateOf("") }
    var teacherText by remember { mutableStateOf("") }

    val formatTimeComponent = { c: Int -> c.toString().padStart(2, '0') }

    val selectedDateEpochDays = datePickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()
    }
    
    val dayOfWeek = selectedDateEpochDays?.let { LocalDate.ofEpochDay(it).dayOfWeek.value }
    val validBaseEntries = if (dayOfWeek != null) {
        uiState.allTimetableEntries.filter { it.dayOfWeek == dayOfWeek }
            .distinctBy { Pair(it.subjectId, it.startTime) }
    } else emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = Dimens.spacing24),
        title = { Text("Add Temporary Lecture", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.primaryText) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                
                // 1. Type
                Text("Type", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    val addColor = if (type == TemporaryLectureType.ADDITION) colors.accent else colors.elevatedSurface
                    val addText = if (type == TemporaryLectureType.ADDITION) colors.surface else colors.primaryText
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(addColor).clickable { type = TemporaryLectureType.ADDITION }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("Add extra", style = Typography.labelMedium, color = addText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val repColor = if (type == TemporaryLectureType.REPLACEMENT) colors.accent else colors.elevatedSurface
                    val repText = if (type == TemporaryLectureType.REPLACEMENT) colors.surface else colors.primaryText
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(repColor).clickable { type = TemporaryLectureType.REPLACEMENT }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("Replace", style = Typography.labelMedium, color = repText)
                    }
                }
                
                // 2. Date Picker (Simplified to dropdown style conceptually or just reuse standard DatePicker)
                Text("Date", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    DatePicker(
                        state = datePickerState,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                        colors = DatePickerDefaults.colors(
                            containerColor = colors.surface,
                            dayContentColor = colors.primaryText,
                            selectedDayContainerColor = colors.accent,
                            selectedDayContentColor = colors.surface,
                            todayContentColor = colors.accent,
                            todayDateBorderColor = colors.accent
                        )
                    )
                }
                
                if (type == TemporaryLectureType.REPLACEMENT) {
                    Text("Scheduled Lecture", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                    
                    if (validBaseEntries.isEmpty()) {
                        Text("No classes scheduled on this date.", style = Typography.bodyMedium, color = colors.critical, modifier = Modifier.padding(bottom = 16.dp))
                    } else {
                        validBaseEntries.forEach { entry ->
                            val sub = uiState.availableSubjects.find { it.id == entry.subjectId }
                            val isSel = selectedReplacesId == entry.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) colors.accent.copy(alpha=0.15f) else colors.elevatedSurface)
                                    .border(1.dp, if (isSel) colors.accent else colors.border, RoundedCornerShape(8.dp))
                                    .clickable { selectedReplacesId = entry.id }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${formatTimeComponent(entry.startTime / 60)}:${formatTimeComponent(entry.startTime % 60)}", style = Typography.labelSmall, color = colors.secondaryText)
                                    Text(sub?.name ?: "?", style = Typography.bodyMedium, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
                                }
                                if (isSel) Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accent)
                            }
                        }
                    }
                }
                
                if (type == TemporaryLectureType.ADDITION) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Time", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                            // Simple text inputs for now due to complexity of TimePicker in dialogues
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = formatTimeComponent(startHour), 
                                    onValueChange = { startHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                                OutlinedTextField(
                                    value = formatTimeComponent(startMin),
                                    onValueChange = { startMin = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("End Time", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = formatTimeComponent(endHour), 
                                    onValueChange = { endHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(":", modifier = Modifier.padding(horizontal = 4.dp))
                                OutlinedTextField(
                                    value = formatTimeComponent(endMin),
                                    onValueChange = { endMin = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                val labelSubject = if (type == TemporaryLectureType.REPLACEMENT) "Replacement Subject" else "Subject"
                Text(labelSubject, style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    uiState.availableSubjects.forEach { sub ->
                        val isSel = selectedSubjectId == sub.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.accent.copy(alpha=0.15f) else colors.elevatedSurface)
                                .clickable { selectedSubjectId = sub.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sub.name, style = Typography.bodyMedium, color = colors.primaryText, modifier = Modifier.weight(1f))
                            if (isSel) Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accent)
                        }
                    }
                }
                
                Text("Room (Optional)", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = roomText,
                    onValueChange = { roomText = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Teacher (Optional)", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = teacherText,
                    onValueChange = { teacherText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            val isDateValid = selectedDateEpochDays != null
            val isSubjectValid = selectedSubjectId != null
            val isReplacesValid = (type == TemporaryLectureType.ADDITION) || (selectedReplacesId != null)
            val isTimeValid = (type == TemporaryLectureType.REPLACEMENT) || ((startHour * 60 + startMin) < (endHour * 60 + endMin))
            
            val canSave = isDateValid && isSubjectValid && isReplacesValid && isTimeValid
            
            Button(
                onClick = {
                    if (canSave) {
                        var effStart = startHour * 60 + startMin
                        var effEnd = endHour * 60 + endMin
                        
                        if (type == TemporaryLectureType.REPLACEMENT && selectedReplacesId != null) {
                            val target = validBaseEntries.find { it.id == selectedReplacesId }
                            if (target != null) {
                                effStart = target.startTime
                                effEnd = target.endTime
                            }
                        }
                        
                        onSave(selectedDateEpochDays!!, effStart, effEnd, selectedSubjectId!!, teacherText.ifBlank { null }, roomText.ifBlank { null }, type, selectedReplacesId)
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.surface)
            ) {
                Text(if (type == TemporaryLectureType.ADDITION) "Add lecture" else "Add replacement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.primaryText)
            }
        },
        containerColor = colors.surface
    )
}
