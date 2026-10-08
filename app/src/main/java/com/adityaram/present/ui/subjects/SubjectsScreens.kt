package com.adityaram.present.ui.subjects

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.ui.theme.LocalPresentColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onNavigateToSubjectDetail: (Long) -> Unit,
    viewModel: SubjectsViewModel = viewModel(factory = SubjectsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current
    val haptic = LocalHapticFeedback.current
    
    var subjectToEdit by remember { mutableStateOf<SubjectUiModel?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectUiModel?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (uiState.subjects.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 132.dp), // Clear fixed header including title
                contentAlignment = Alignment.Center
            ) {
                Text("No subjects found", color = colors.secondaryText, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = com.adityaram.present.ui.theme.Dimens.spacing20,
                    end = com.adityaram.present.ui.theme.Dimens.spacing20,
                    top = 90.dp, // Spacer for fixed header (logo only, no title)
                    bottom = com.adityaram.present.ui.theme.Dimens.spacing24
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.subjects) { uiModel ->
                    SubjectCard(
                        uiModel = uiModel,
                        onClick = { onNavigateToSubjectDetail(uiModel.subject.id) },
                        onEdit = { subjectToEdit = uiModel },
                        onDelete = { subjectToDelete = uiModel }
                    )
                }
            }
        }

        // Fixed Header Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .background(colors.background)
                .padding(
                    start = com.adityaram.present.ui.theme.Dimens.spacing20,
                    end = com.adityaram.present.ui.theme.Dimens.spacing20,
                    top = com.adityaram.present.ui.theme.Dimens.spacing24,
                    bottom = com.adityaram.present.ui.theme.Dimens.spacing12
                )
        ) {
            com.adityaram.present.ui.home.HomeHeader()
        }
    }

    if (subjectToEdit != null) {
        EditSubjectBottomSheet(
            uiModel = subjectToEdit!!,
            onDismiss = { subjectToEdit = null },
            onSave = { newName, newTeacher -> 
                val success = viewModel.editSubject(subjectToEdit!!.subject.id, newName, newTeacher)
                if (success) {
                    subjectToEdit = null
                }
                // Return success boolean so bottom sheet can show error if duplicate? It's fine for now, we don't have a toast.
                // Wait, if it fails, we should just let the user know, but a simple dismiss is fine for duplicate rule. Actually let's just close if successful.
            }
        )
    }

    if (subjectToDelete != null) {
        DeleteSubjectDialog(
            uiModel = subjectToDelete!!,
            onDismiss = { subjectToDelete = null },
            onConfirm = {
                viewModel.deleteSubject(subjectToDelete!!.subject.id)
                subjectToDelete = null
            }
        )
    }


}

@Composable
private fun SubjectCard(
    uiModel: SubjectUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalPresentColors.current
    val isCritical = uiModel.percentage < uiModel.targetRequirement
    val statusColor = if (isCritical) colors.critical else colors.accent

    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiModel.subject.name,
                    color = colors.primaryText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (!uiModel.subject.teacher.isNullOrBlank()) {
                    Text(
                        text = uiModel.subject.teacher,
                        color = colors.secondaryText,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "${uiModel.presentCount} / ${uiModel.presentCount + uiModel.absentCount} attended",
                    color = colors.mutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(uiModel.percentage * 100).toInt()}%",
                        color = statusColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More Options", tint = colors.secondaryText)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Subject", color = colors.primaryText) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = colors.critical) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subjectId: Long,
    onBack: () -> Unit,
    viewModel: SubjectsViewModel = viewModel(factory = SubjectsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current
    val haptic = LocalHapticFeedback.current

    var showBulkMenu by remember { mutableStateOf(false) }
    var showBulkAbsentConfirm by remember { mutableStateOf(false) }
    var showBulkPresentConfirm by remember { mutableStateOf(false) }

    val uiModel = uiState.subjects.find { it.subject.id == subjectId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiModel?.subject?.name ?: "Subject Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = colors.primaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.primaryText,
                    navigationIconContentColor = colors.primaryText
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp)
            )
        },
        containerColor = colors.background
    ) { padding ->
        if (uiModel != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SubjectDetailHeader(uiModel = uiModel)
                }
                
                item {
                    val hasTodayClasses = uiModel.todayClasses.filter { it.status != AttendanceStatus.CANCELLED }.isNotEmpty()

                    if (hasTodayClasses) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Today's Classes",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showBulkPresentConfirm = true },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accent.copy(alpha = 0.1f), 
                                        contentColor = colors.accent
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { 
                                    Text("Mark present", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) 
                                }
                                Button(
                                    onClick = { showBulkAbsentConfirm = true },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.critical.copy(alpha = 0.1f), 
                                        contentColor = colors.critical
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { 
                                    Text("Mark absent", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) 
                                }
                            }
                        }
                    }

                    Text(
                        text = "HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = colors.secondaryText,
                        modifier = Modifier.padding(
                            top = if(hasTodayClasses) 24.dp else 16.dp, 
                            bottom = 8.dp
                        )
                    )

                    if (showBulkPresentConfirm) {
                        AlertDialog(
                            onDismissRequest = { showBulkPresentConfirm = false },
                            title = { Text("Mark present?", color = colors.primaryText) },
                            text = { Text("Mark this class as present?", color = colors.mutedText) },
                            confirmButton = {
                                TextButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.markBulkAttendance(subjectId, AttendanceStatus.PRESENT)
                                    showBulkPresentConfirm = false
                                }) {
                                    Text("Mark present", color = colors.accent)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBulkPresentConfirm = false }) {
                                    Text("Cancel", color = colors.primaryText)
                                }
                            },
                            containerColor = colors.surface,
                            textContentColor = colors.mutedText,
                            titleContentColor = colors.primaryText
                        )
                    }

                    if (showBulkAbsentConfirm) {
                        AlertDialog(
                            onDismissRequest = { showBulkAbsentConfirm = false },
                            title = { Text("Mark absent?", color = colors.primaryText) },
                            text = { Text("Mark this class as absent?", color = colors.mutedText) },
                            confirmButton = {
                                TextButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.markBulkAttendance(subjectId, AttendanceStatus.ABSENT)
                                    showBulkAbsentConfirm = false
                                }) {
                                    Text("Mark absent", color = colors.critical)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showBulkAbsentConfirm = false }) {
                                    Text("Cancel", color = colors.primaryText)
                                }
                            },
                            containerColor = colors.surface,
                            textContentColor = colors.mutedText,
                            titleContentColor = colors.primaryText
                        )
                    }
                }

                val historyRecords = uiModel.records.filter { it.status != AttendanceStatus.CANCELLED }
                if (historyRecords.isEmpty()) {
                    item {
                        Text(
                            text = "No recorded classes yet.",
                            color = colors.secondaryText,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    items(
                        items = historyRecords,
                        key = { record -> record.id }
                    ) { record ->
                        HistoryRecordCard(record = record, onToggle = { viewModel.toggleAttendance(record) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectDetailHeader(uiModel: SubjectUiModel) {
    val colors = LocalPresentColors.current
    val total = uiModel.presentCount + uiModel.absentCount
    val isCritical = uiModel.percentage < uiModel.targetRequirement
    val statusColor = if (isCritical) colors.critical else colors.accent

    val requirementMsg = if (isCritical) {
        val reqFraction = uiModel.targetRequirement / 100f
        if (reqFraction < 1f) {
            val needed = ceil((reqFraction * total - uiModel.presentCount) / (1f - reqFraction)).toInt()
            "Attend $needed more next classes to hit ${uiModel.targetRequirement.toInt()}%"
        } else {
            "Attendance is critically low."
        }
    } else {
        "You are on track above ${uiModel.targetRequirement.toInt()}%."
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = String.format(java.util.Locale.US, "%.1f%%", uiModel.percentage * 100),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
        Text(
            text = "${uiModel.presentCount} Attended / $total Total",
            color = colors.secondaryText,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
        ) {
            Text(
                text = requirementMsg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                textAlign = TextAlign.Center,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun HistoryRecordCard(record: AttendanceRecord, onToggle: () -> Unit) {
    val colors = LocalPresentColors.current
    val isPresent = record.status == AttendanceStatus.PRESENT
    val statusColor = if (isPresent) colors.accent else colors.critical
    
    val sdf = remember { SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) }
    val dateStr = sdf.format(Date(record.date))
    val h = record.startTime / 60
    val m = record.startTime % 60
    val amPm = if (h >= 12) "PM" else "AM"
    val disp = if (h == 0) 12 else if (h > 12) h - 12 else h
    val timeStr = String.format("%02d:%02d %s", disp, m, amPm)

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
    )
    
    val bgColor by animateColorAsState(
        targetValue = if (isPresent) colors.safe.copy(alpha = 0.15f) else colors.critical.copy(alpha = 0.15f),
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surface) 
        // Just empty border or remove border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.primaryText
                )
                Text(
                    text = timeStr,
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isPresent) "Present" else "Absent",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = if (isPresent) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                    contentDescription = null,
                    tint = statusColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EditSubjectBottomSheet(
    uiModel: SubjectUiModel,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    val colors = LocalPresentColors.current
    var subjectName by remember { mutableStateOf(uiModel.subject.name) }
    var teacher by remember { mutableStateOf(uiModel.subject.teacher ?: "") }

    val coroutineScope = rememberCoroutineScope()
    val subjectRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val teacherRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.9f)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Subject",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.secondaryText,
                            focusedLabelColor = colors.accent,
                            unfocusedLabelColor = colors.secondaryText,
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(subjectRequester)
                            .onFocusChanged { focusState -> if (focusState.isFocused) coroutineScope.launch { subjectRequester.bringIntoView() } }
                    )

                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Teacher (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.secondaryText,
                            focusedLabelColor = colors.accent,
                            unfocusedLabelColor = colors.secondaryText,
                            focusedTextColor = colors.primaryText,
                            unfocusedTextColor = colors.primaryText
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(teacherRequester)
                            .onFocusChanged { focusState -> if (focusState.isFocused) coroutineScope.launch { teacherRequester.bringIntoView() } }
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = { onSave(subjectName, teacher) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(16.dp),
                        enabled = subjectName.isNotBlank()
                    ) {
                        Text(
                            text = "Save changes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
        }
    }
}

@Composable
private fun DeleteSubjectDialog(
    uiModel: SubjectUiModel,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalPresentColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "Delete Subject",
                fontWeight = FontWeight.Bold,
                color = colors.primaryText
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete '${uiModel.subject.name}'?\n\nThis will permanently delete all associated timetable entries and attendance records. This action cannot be undone.",
                color = colors.secondaryText
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = colors.critical, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.primaryText)
            }
        }
    )
}
