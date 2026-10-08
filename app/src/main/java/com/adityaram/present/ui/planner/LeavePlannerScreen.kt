package com.adityaram.present.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Composable
fun LeavePlannerScreen(
    viewModel: LeavePlannerViewModel = viewModel(factory = LeavePlannerViewModel.Factory),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current

    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        
        Column(modifier = Modifier.fillMaxSize()) {
            PresentTopBar(
                title = "Leave Planner",
                subtitle = "See how planned leave affects attendance",
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
            item {
                ProjectedImpactCard(uiState)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (uiState.leavePlans.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No planned leave", style = Typography.bodyLarge, color = colors.secondaryText, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Plan your first leave to see projection", style = Typography.bodySmall, color = colors.mutedText)
                    }
                }
            } else {
                item {
                    Text(
                        text = "UPCOMING LEAVES",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.sp),
                        color = colors.mutedText,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                items(uiState.leavePlans) { plan ->
                    LeavePlanCard(
                        planModel = plan,
                        onDelete = { viewModel.deleteLeavePlan(plan.entity) }
                    )
                }
            }
        }
    }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = colors.accent,
            contentColor = colors.surface
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Plan Leave")
        }

    if (showCreateDialog) {
        CreateLeaveDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { startDate, endDate, reason ->
                viewModel.addLeavePlan(startDate, endDate, reason)
                showCreateDialog = false
            }
        )
    }
}
}

@Composable
fun ProjectedImpactCard(uiState: LeavePlannerUiState) {
    val colors = LocalPresentColors.current
    
    val currentFormatted = "%.1f".format(uiState.currentAttendance * 100)
    val projectedFormatted = "%.1f".format(uiState.projectedAttendance * 100)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(20.dp)
    ) {
        Text("Projected Attendance", style = Typography.labelMedium, color = colors.secondaryText)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$currentFormatted%", 
                    style = Typography.headlineMedium, 
                    color = colors.primaryText,
                    fontWeight = FontWeight.Bold
                )
                Text("Current", style = Typography.labelSmall, color = colors.mutedText)
            }
            
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack, 
                contentDescription = null, 
                modifier = Modifier.size(24.dp).padding(end = 4.dp), 
                tint = colors.mutedText
            )
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$projectedFormatted%", 
                    style = Typography.headlineMedium, 
                    color = if (uiState.projectedAttendance < uiState.currentAttendance) colors.critical else colors.safe,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.totalAffectedLectures} lectures affected", 
                    style = Typography.labelSmall, 
                    color = colors.mutedText
                )
            }
        }
    }
}

@Composable
fun LeavePlanCard(planModel: LeavePlanModel, onDelete: () -> Unit) {
    val colors = LocalPresentColors.current
    val formatter = DateTimeFormatter.ofPattern("d MMM")
    
    val startDate = LocalDate.ofEpochDay(planModel.entity.startDateEpochDays)
    val endDate = LocalDate.ofEpochDay(planModel.entity.endDateEpochDays)
    
    val dateText = if (startDate == endDate) {
        startDate.format(formatter)
    } else {
        "${startDate.format(formatter)} – ${endDate.format(formatter)}"
    }
    
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
                Text(text = dateText, style = Typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.primaryText)
                if (!planModel.entity.reason.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = planModel.entity.reason, style = Typography.bodySmall, color = colors.secondaryText)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.elevatedSurface).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("${planModel.durationDays} day${if(planModel.durationDays > 1) "s" else ""}", style = Typography.labelSmall, color = colors.primaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.critical.copy(alpha=0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("${planModel.affectedLectures} lectures affected", style = Typography.labelSmall, color = colors.critical)
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = colors.mutedText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CreateLeaveDialog(onDismiss: () -> Unit, onSave: (Long, Long, String) -> Unit) {
    val colors = LocalPresentColors.current
    
    var reason by remember { mutableStateOf("") }
    
    var startSelection by remember { mutableStateOf<LocalDate?>(null) }
    var endSelection by remember { mutableStateOf<LocalDate?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).padding(vertical = 24.dp),
        title = { Text("Plan Leave", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.primaryText) },
        text = {
            Column {
                // We leave spacing and context text
                Box(modifier = Modifier.fillMaxWidth()) {
                    PresentDateRangePicker(
                        startDate = startSelection,
                        endDate = endSelection,
                        onDateSelected = { date ->
                            if (startSelection == null || (startSelection != null && endSelection != null)) {
                                startSelection = date
                                endSelection = null
                            } else if (startSelection != null && endSelection == null) {
                                if (date.isBefore(startSelection)) {
                                    startSelection = date
                                    endSelection = null
                                } else {
                                    endSelection = date
                                }
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Reason (Optional)", style = Typography.labelMedium, color = colors.secondaryText, modifier = Modifier.padding(bottom = 8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.background,
                        unfocusedContainerColor = colors.background,
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.primaryText,
                        unfocusedTextColor = colors.primaryText
                    ),
                    placeholder = { Text("e.g. Diwali Vacation", style = Typography.bodyMedium, color = colors.mutedText) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val msStart = startSelection?.toEpochDay()
                    val msEnd = endSelection?.toEpochDay() ?: msStart
                    
                    if (msStart != null && msEnd != null) {
                        onSave(msStart, msEnd, reason.trim())
                    }
                },
                enabled = startSelection != null,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.surface)
            ) {
                Text("Plan Leave")
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

@Composable
fun PresentDateRangePicker(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    val colors = LocalPresentColors.current
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Month Selector Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
            Text(currentMonth.format(formatter), style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.primaryText)
            Row {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous Month", tint = colors.primaryText)
                }
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next Month", tint = colors.primaryText)
                }
            }
        }
        
        // Days of Week Header
        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("S", "M", "T", "W", "T", "F", "S")
            days.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, style = Typography.labelSmall, color = colors.secondaryText, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
        
        // Grid
        val firstDayOfWeek = currentMonth.dayOfWeek.value % 7
        val daysInMonth = currentMonth.lengthOfMonth()
        val totalCells = ceil((firstDayOfWeek + daysInMonth) / 7.0).toInt() * 7
        
        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOfWeek + 1
                        
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = currentMonth.withDayOfMonth(dayNumber)
                                val effectiveEnd = endDate ?: startDate
                                
                                val isStart = cellDate == startDate
                                val isEnd = cellDate == effectiveEnd
                                val isBetween = startDate != null && effectiveEnd != null && cellDate.isAfter(startDate) && cellDate.isBefore(effectiveEnd)
                                
                                val isSelectionRange = startDate != null && effectiveEnd != null && startDate != effectiveEnd
                                
                                // Range background connecting cells naturally
                                if (isBetween) {
                                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).background(colors.accent.copy(alpha=0.15f)))
                                } else if (isStart && isSelectionRange) {
                                    Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.6f).align(Alignment.CenterEnd).background(colors.accent.copy(alpha=0.15f)))
                                } else if (isEnd && isSelectionRange) {
                                    Box(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.6f).align(Alignment.CenterStart).background(colors.accent.copy(alpha=0.15f)))
                                }
                                
                                // Circle bounds for Start/End
                                if (isStart || isEnd) {
                                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(colors.accent))
                                }
                                
                                // Interaction and Text
                                Box(
                                    modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { onDateSelected(cellDate) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(), 
                                        style = Typography.bodyMedium, 
                                        color = if (isStart || isEnd) colors.surface else colors.primaryText,
                                        fontWeight = if (isStart || isEnd) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
