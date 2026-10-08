package com.adityaram.present.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onBack: () -> Unit,
    viewModel: PlannerViewModel = viewModel(factory = PlannerViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current
    
    var showSubjectSelector by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            PresentTopBar(
                title = "Attendance Planner",
                subtitle = "Simulate your attendance",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Dimens.screenHorizontalPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. SUBJECT SELECTOR
            val selectedItem = uiState.selectedSubject
            if (selectedItem != null) {
                SubjectSelectorPill(
                    subjectName = selectedItem.subject.name,
                    percentageString = selectedItem.percentage?.let { "${(it * 100).toInt()}%" } ?: "No records",
                    onClick = { showSubjectSelector = true }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 2. CURRENT ATTENDANCE SUMMARY
                CurrentAttendanceCard(selectedItem)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 3. WHAT IF SIMULATOR
                Text("What if?", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.primaryText)
                Text("Plan upcoming classes", style = Typography.labelMedium, color = colors.secondaryText)
                Spacer(modifier = Modifier.height(16.dp))
                
                SimulatorCard(
                    attends = uiState.simulatedAttends,
                    skips = uiState.simulatedSkips,
                    onAttendChange = { viewModel.updateAttend(it) },
                    onSkipChange = { viewModel.updateSkip(it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 4. PROJECTED RESULT
                Text("Projected attendance", style = Typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.primaryText)
                Spacer(modifier = Modifier.height(16.dp))
                
                ProjectedResultCard(uiState = uiState)
                
                Spacer(modifier = Modifier.height(16.dp))

                // 5. RESET ACTION
                if (uiState.simulatedAttends > 0 || uiState.simulatedSkips > 0) {
                    TextButton(
                        onClick = { viewModel.resetSimulation() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Reset Simulation", color = colors.secondaryText, style = Typography.bodyMedium)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No subjects available.", color = colors.secondaryText)
                }
            }
        }
    }

    if (showSubjectSelector) {
        ModalBottomSheet(
            onDismissRequest = { showSubjectSelector = false },
            containerColor = colors.surface
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                item {
                    Text("Select a subject", style = Typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.primaryText)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                items(uiState.subjects) { subj ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.selectSubject(subj.subject.id)
                                showSubjectSelector = false
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(subj.subject.name, color = colors.primaryText, style = Typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            subj.percentage?.let { "${(it * 100).toInt()}%" } ?: "--",
                            color = colors.secondaryText,
                            style = Typography.bodyMedium
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(48.dp)) }
            }
        }
    }
}

@Composable
fun SubjectSelectorPill(subjectName: String, percentageString: String, onClick: () -> Unit) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(subjectName, style = Typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.primaryText)
            Spacer(modifier = Modifier.width(8.dp))
            Text(percentageString, style = Typography.bodyMedium, color = colors.secondaryText)
        }
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Change Subject", tint = colors.mutedText)
    }
}

@Composable
fun CurrentAttendanceCard(subject: PlannerSubjectModel) {
    val colors = LocalPresentColors.current
    
    val isSafe = (subject.percentage ?: 1f) >= subject.targetRequirement
    val isWarning = (subject.percentage ?: 1f) < subject.targetRequirement && (subject.percentage ?: 1f) >= (subject.targetRequirement - 0.05f)
    val statusColor = when {
        subject.percentage == null -> colors.accent
        isSafe -> colors.safe
        isWarning -> Color(0xFFFBBF24) // Warning amber
        else -> colors.critical
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Current", style = Typography.labelMedium, color = colors.secondaryText)
            Spacer(modifier = Modifier.height(4.dp))
            if (subject.percentage != null) {
                Text("${(subject.percentage!! * 100).toInt()}%", style = Typography.titleLarge, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = statusColor)
            } else {
                Text("--", style = Typography.titleLarge, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.mutedText)
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text("${subject.presentCount} Present · ${subject.absentCount} Absent", style = Typography.bodyMedium, color = colors.primaryText)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Target ${(subject.targetRequirement * 100).toInt()}%", style = Typography.labelMedium, color = colors.mutedText)
        }
    }
}

@Composable
fun SimulatorCard(
    attends: Int,
    skips: Int,
    onAttendChange: (Int) -> Unit,
    onSkipChange: (Int) -> Unit
) {
    val colors = LocalPresentColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(20.dp)
    ) {
        SimulatorControl(label = "ATTEND", value = attends, onDecrease = { onAttendChange(-1) }, onIncrease = { onAttendChange(1) })
        Spacer(modifier = Modifier.height(16.dp))
        SimulatorControl(label = "SKIP", value = skips, onDecrease = { onSkipChange(-1) }, onIncrease = { onSkipChange(1) })
    }
}

@Composable
fun SimulatorControl(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = Typography.bodyLarge, color = colors.primaryText, fontWeight = FontWeight.Bold)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.elevatedSurface)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                    val stroke = 2.dp.toPx()
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val r = size.width * 0.25f
                    drawLine(
                        color = colors.accent,
                        start = androidx.compose.ui.geometry.Offset(cx - r, cy),
                        end = androidx.compose.ui.geometry.Offset(cx + r, cy),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            
            Text(
                text = value.toString(),
                style = Typography.titleLarge,
                color = colors.primaryText,
                modifier = Modifier.padding(horizontal = 28.dp),
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = onIncrease,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.elevatedSurface)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = colors.accent)
            }
        }
    }
}

@Composable
fun ProjectedResultCard(uiState: PlannerUiState) {
    val colors = LocalPresentColors.current
    val pct = uiState.projectedPercentage

    val isSafe = (pct ?: 1f) >= uiState.targetRequirement
    val isWarning = (pct ?: 1f) < uiState.targetRequirement && (pct ?: 1f) >= (uiState.targetRequirement - 0.05f)
    val statusColor = when {
        pct == null -> colors.accent
        isSafe -> colors.safe
        isWarning -> Color(0xFFFBBF24)
        else -> colors.critical
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pct != null) {
            Text(
                text = "${String.format("%.1f", pct * 100)}%",
                style = Typography.titleLarge,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val attendsTxt = if (uiState.simulatedAttends > 0) "${uiState.simulatedAttends} attend${if(uiState.simulatedAttends > 1) "s" else ""}" else ""
            val skipsTxt = if (uiState.simulatedSkips > 0) "${uiState.simulatedSkips} skip${if(uiState.simulatedSkips > 1) "s" else ""}" else ""
            val joinText = if (attendsTxt.isNotEmpty() && skipsTxt.isNotEmpty()) " + " else ""
            val simulationState = if (attendsTxt.isNotEmpty() || skipsTxt.isNotEmpty()) "\nafter $attendsTxt$joinText$skipsTxt" else ""

            Text(
                text = "${uiState.projectedPresent} Present · ${uiState.projectedAbsent} Absent$simulationState",
                style = Typography.bodyMedium,
                color = colors.secondaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            if (uiState.contextMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                // Contextual message as one concise line
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(statusColor.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = uiState.contextMessage!!,
                        style = Typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        } else {
            Text(
                text = "No attendance recorded yet",
                style = Typography.titleMedium,
                color = colors.mutedText,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
}
