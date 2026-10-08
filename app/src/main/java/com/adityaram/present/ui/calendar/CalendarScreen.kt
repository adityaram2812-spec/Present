package com.adityaram.present.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onNavigateToBottomNav: (String) -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            PresentTopBar(
                title = "Calendar",
                onBack = onBack,
                trailingContent = {
                    TextButton(onClick = { viewModel.onSelectToday() }) {
                        Text("Today", color = colors.accent, style = Typography.labelLarge)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        CalendarHeaderAndGrid(
                            uiState = uiState,
                            onPrevMonth = viewModel::onPreviousMonth,
                            onNextMonth = viewModel::onNextMonth,
                            onDayClick = viewModel::onDateSelected
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CalendarLegend()
                    }
                }

                item {
                    SelectedDateSummary(uiState = uiState)
                }
                
                item { 
                    DailyCalculatedSummary(uiState = uiState)
                }

                items(uiState.selectedSessions) { session ->
                    SessionRowItem(
                        session = session,
                        onToggle = { viewModel.toggleAttendanceStatus(session.record) }
                    )
                }


                item {
                    InformationCard()
                }
            }
        }
    }
}

@Composable
fun CalendarHeaderAndGrid(
    uiState: CalendarUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val colors = LocalPresentColors.current
    val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uiState.currentMonth.format(monthFmt),
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous Month", tint = colors.mutedText, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next Month", tint = colors.mutedText, modifier = Modifier.size(20.dp))
                }
            }
            if (uiState.monthAverage != null) {
                Text(
                    text = "${(uiState.monthAverage * 100).toInt()}% avg",
                    style = Typography.labelLarge,
                    color = colors.mutedText,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "-- avg",
                    style = Typography.labelLarge,
                    color = colors.mutedText
                )
            }
        }
        
        val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = Typography.labelMedium,
                    color = colors.mutedText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val chunkedDays = uiState.days.chunked(7)
        chunkedDays.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CalendarDayCell(day = day, onClick = { onDayClick(day.date) })
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: CalendarDayUiModel, onClick: () -> Unit) {
    val colors = LocalPresentColors.current
    
    val bgColor = when {
        day.isSelected -> colors.accent
        else -> Color.Transparent
    }
    
    val contentColor = when {
        day.isSelected -> colors.background
        day.isToday -> colors.accent
        !day.inCurrentMonth -> colors.mutedText.copy(alpha = 0.4f)
        else -> colors.primaryText
    }
    
    val dotColor = when (day.status) {
        DayStatus.NONE -> Color.Transparent
        DayStatus.PRESENT_ONLY -> if (day.isSelected) colors.background.copy(alpha = 0.7f) else colors.safe
        DayStatus.ABSENT_ONLY -> if (day.isSelected) colors.background.copy(alpha = 0.7f) else colors.critical
        DayStatus.MIXED -> if (day.isSelected) colors.background.copy(alpha = 0.7f) else colors.warning
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = Typography.labelLarge,
            fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
        if (day.status != DayStatus.NONE) {
            Spacer(modifier = Modifier.height(1.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun CalendarLegend() {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem("Present", colors.safe)
            LegendItem("Absent", colors.critical)
            LegendItem("Mixed", colors.warning)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).border(1.5.dp, colors.accent, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Today", style = Typography.labelSmall, color = colors.mutedText)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    val colors = LocalPresentColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = Typography.labelSmall, color = colors.mutedText)
    }
}

val PillShape = RoundedCornerShape(50)

@Composable
fun SelectedDateSummary(uiState: CalendarUiState) {
    val colors = LocalPresentColors.current
    val fD = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val count = uiState.selectedSessions.size
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = uiState.selectedDate.format(fD),
            style = Typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primaryText
        )
        
        Box(
            modifier = Modifier
                .clip(PillShape)
                .background(colors.surface)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (count == 1) "1 class" else "$count classes",
                style = Typography.labelSmall,
                color = colors.secondaryText,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DailyCalculatedSummary(uiState: CalendarUiState) {
    val colors = LocalPresentColors.current
    val day = uiState.days.find { it.date == uiState.selectedDate } ?: return
    
    val present = day.presentCount
    val absent = day.absentCount
    val total = present + absent
    val rate = if (total > 0) ((present.toFloat() / total.toFloat()) * 100).toInt().toString() + "%" else "--"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Present", style = Typography.labelMedium, color = colors.mutedText)
                Text(present.toString(), style = Typography.titleMedium, color = colors.safe, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Absent", style = Typography.labelMedium, color = colors.mutedText)
                Text(absent.toString(), style = Typography.titleMedium, color = colors.critical, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Rate", style = Typography.labelMedium, color = colors.mutedText)
                Text(rate, style = Typography.titleMedium, color = colors.primaryText, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(colors.elevatedSurface)
        ) {
            if (total > 0) {
                val presentWeight = present.toFloat() / total.toFloat()
                val absentWeight = absent.toFloat() / total.toFloat()
                
                if (presentWeight > 0f) {
                    Box(modifier = Modifier.weight(presentWeight).fillMaxHeight().background(colors.safe))
                }
                if (absentWeight > 0f) {
                    Box(modifier = Modifier.weight(absentWeight).fillMaxHeight().background(colors.critical))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(colors.elevatedSurface))
            }
        }
    }
}


@Composable
fun SessionRowItem(session: CalendarSessionUiModel, onToggle: () -> Unit) {
    val colors = LocalPresentColors.current
    
    val hours = session.startTime / 60
    val mins = session.startTime % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", hours, mins)
    val isPresent = session.record.status == AttendanceStatus.PRESENT

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.subjectName,
                style = Typography.titleMedium,
                color = colors.primaryText,
                fontWeight = FontWeight.Bold
            )
            val details = listOfNotNull(timeFormatted, session.room, session.teacher)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            Text(
                text = details,
                style = Typography.bodySmall,
                color = colors.mutedText
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Status indicator
        Text(
            text = if (isPresent) "Present" else "Absent",
            style = Typography.labelMedium,
            color = if (isPresent) colors.safe else colors.critical,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Edit button
        Icon(Icons.Rounded.Edit, contentDescription = "Edit Attendance", tint = colors.mutedText, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun InformationCard() {
    val colors = LocalPresentColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.elevatedSurface.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = colors.mutedText, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "The Calendar strictly records committed attendance logs. Future classes are not pre-populated until they occur.",
            style = Typography.bodySmall,
            color = colors.mutedText,
            lineHeight = Typography.bodySmall.lineHeight * 1.2
        )
    }
}


