package com.adityaram.present.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import com.adityaram.present.ui.timetable.TimetableClassUiModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.adityaram.present.data.model.Subject
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.ExperimentalFoundationApi
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddClassBottomSheet(
    onDismissRequest: () -> Unit,
    subjects: List<Subject>,
    initialDate: LocalDate,
    classToEdit: TimetableClassUiModel? = null,
    onSave: (subjectName: String, dayOfWeek: Int, startTime: Int, endTime: Int, room: String?, teacher: String?, isRecurring: Boolean, date: LocalDate?) -> Unit
) {
    val colors = LocalPresentColors.current
    
    // Form state
    val coroutineScope = rememberCoroutineScope()
    val subjectRequester = remember { BringIntoViewRequester() }
    val roomRequester = remember { BringIntoViewRequester() }
    val teacherRequester = remember { BringIntoViewRequester() }

    var subjectName by remember { mutableStateOf(classToEdit?.subjectName ?: "") }
    var isRecurring by remember { mutableStateOf(classToEdit?.entry?.isRecurring ?: true) }
    
    var startTime by remember { mutableStateOf<LocalTime?>(classToEdit?.entry?.startTime?.let { LocalTime.of(it / 60, it % 60) }) }
    var endTime by remember { mutableStateOf<LocalTime?>(classToEdit?.entry?.endTime?.let { LocalTime.of(it / 60, it % 60) }) }
    
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var room by remember { mutableStateOf(classToEdit?.entry?.room ?: "") }
    var teacher by remember { mutableStateOf(classToEdit?.entry?.teacher ?: "") }
    var date by remember { mutableStateOf(classToEdit?.entry?.specificDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: initialDate) } 
    var dayOfWeek by remember { mutableStateOf(classToEdit?.entry?.dayOfWeek ?: initialDate.dayOfWeek.value) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val formatTime = { time: LocalTime? ->
        if (time != null) time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        else "Select Time"
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.mutedText) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacing24)
                .padding(bottom = Dimens.spacing24)
                .imePadding()
        ) {
            Text(
                text = if (classToEdit != null) "Edit Class" else "Add Class",
                style = Typography.headlineMedium,
                color = colors.primaryText,
                modifier = Modifier.padding(bottom = Dimens.spacing16)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = Typography.labelMedium,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = Dimens.spacing16)
                )
            }

            // Subject 
            val normalizedInput = subjectName.trim().replace("\\s+".toRegex(), " ")
            val existingMatch = subjects.find { it.name.equals(normalizedInput, ignoreCase = true) }

            Text(text = "Subject", style = Typography.labelMedium, color = colors.mutedText)
            Spacer(modifier = Modifier.height(Dimens.spacing8))
            OutlinedTextField(
                value = subjectName,
                onValueChange = { subjectName = it },
                placeholder = { Text("Enter subject name", color = colors.mutedText, style = Typography.bodyMedium) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.primaryText,
                    unfocusedTextColor = colors.primaryText
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(subjectRequester)
                    .onFocusEvent { if (it.isFocused) coroutineScope.launch { subjectRequester.bringIntoView() } },
                shape = RoundedCornerShape(Dimens.spacing12)
            )
            if (existingMatch != null && subjectName.isNotEmpty() && existingMatch.name != subjectName) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacing8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "'${existingMatch.name}' already exists.",
                        style = Typography.bodySmall,
                        color = colors.mutedText,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Use existing",
                        style = Typography.labelMedium,
                        color = colors.accent,
                        modifier = Modifier.clickable {
                            subjectName = existingMatch.name
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.spacing16))

            // Repeats Weekly Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Repeats weekly", style = Typography.bodyMedium, color = colors.primaryText)
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
                )
            }
            Spacer(modifier = Modifier.height(Dimens.spacing16))

            // Date / Day
            if (isRecurring) {
                Text(text = "Day", style = Typography.labelMedium, color = colors.mutedText)
                Spacer(modifier = Modifier.height(Dimens.spacing8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { index, label ->
                        val dayVal = index + 1
                        val isSelected = dayVal == dayOfWeek
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(Dimens.spacing8))
                                .background(if (isSelected) colors.accent else colors.surface)
                                .clickable { dayOfWeek = dayVal }
                                .padding(vertical = Dimens.spacing8),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) colors.background else colors.primaryText
                            )
                        }
                    }
                }
            } else {
                Text(text = "Date", style = Typography.labelMedium, color = colors.mutedText)
                Spacer(modifier = Modifier.height(Dimens.spacing8))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.spacing12))
                        .background(colors.surface)
                        .clickable { showDatePicker = true }
                        .padding(Dimens.spacing16)
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.getDefault())),
                        style = Typography.bodyLarge,
                        color = colors.primaryText
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.spacing16))

            // Times 
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Start Time", style = Typography.labelMedium, color = colors.mutedText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.spacing12))
                            .background(colors.surface)
                            .clickable { showStartTimePicker = true }
                            .padding(Dimens.spacing16)
                    ) {
                        Text(
                            text = formatTime(startTime),
                            style = Typography.bodyLarge,
                            color = if (startTime != null) colors.primaryText else colors.mutedText
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "End Time", style = Typography.labelMedium, color = colors.mutedText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.spacing12))
                            .background(colors.surface)
                            .clickable { showEndTimePicker = true }
                            .padding(Dimens.spacing16)
                    ) {
                        Text(
                            text = formatTime(endTime),
                            style = Typography.bodyLarge,
                            color = if (endTime != null) colors.primaryText else colors.mutedText
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimens.spacing16))

            // Room / Teacher
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spacing12)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Room (Optional)", style = Typography.labelMedium, color = colors.mutedText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(roomRequester)
                            .onFocusEvent { if (it.isFocused) coroutineScope.launch { roomRequester.bringIntoView() } },
                        shape = RoundedCornerShape(Dimens.spacing12)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Teacher (Optional)", style = Typography.labelMedium, color = colors.mutedText)
                    Spacer(modifier = Modifier.height(Dimens.spacing8))
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(teacherRequester)
                            .onFocusEvent { if (it.isFocused) coroutineScope.launch { teacherRequester.bringIntoView() } },
                        shape = RoundedCornerShape(Dimens.spacing12)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.spacing24))

            Button(
                onClick = {
                    if (subjectName.trim().isEmpty()) {
                        errorMessage = "Subject is required."
                        return@Button
                    }
                    if (startTime == null) {
                        errorMessage = "Start time is required."
                        return@Button
                    }
                    if (endTime == null) {
                        errorMessage = "End time is required."
                        return@Button
                    }
                    
                    val startMins = startTime!!.hour * 60 + startTime!!.minute
                    val endMins = endTime!!.hour * 60 + endTime!!.minute

                    if (startMins >= endMins) {
                        errorMessage = "End time must be after start time."
                        return@Button
                    }
                    
                    if (!isRecurring && date == null) {
                        errorMessage = "Date is required."
                        return@Button
                    }

                    errorMessage = null

                    onSave(
                        subjectName,
                        dayOfWeek,
                        startMins,
                        endMins,
                        room,
                        teacher,
                        isRecurring,
                        if (!isRecurring) date else null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(Dimens.spacing12)
            ) {
                Text(if (classToEdit != null) "Save Changes" else "Save Class", style = Typography.labelLarge, color = Color.White)
            }
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onConfirm = {
                startTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showStartTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showEndTimePicker) {
        val initialEndHour = startTime?.hour ?: 10
        val initialEndMinute = startTime?.minute ?: 0
        val timePickerState = rememberTimePickerState(initialHour = initialEndHour, initialMinute = initialEndMinute)
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onConfirm = {
                endTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showEndTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = colors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = colors.primaryText) }
            },
            colors = DatePickerDefaults.colors(containerColor = colors.background)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = colors.primaryText,
                    headlineContentColor = colors.primaryText,
                    currentYearContentColor = colors.primaryText,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = colors.accent,
                    dayContentColor = colors.primaryText,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = colors.accent,
                    todayContentColor = colors.accent,
                    todayDateBorderColor = colors.accent
                )
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = LocalPresentColors.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel", color = colors.primaryText) }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK", color = colors.accent) }
        },
        text = { content() },
        containerColor = colors.background
    )
}
