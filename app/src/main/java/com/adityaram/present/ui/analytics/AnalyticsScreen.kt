package com.adityaram.present.ui.analytics

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography
import com.adityaram.present.ui.components.PresentTopBar

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    onNavigateToBottomNav: (String) -> Unit,
    viewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = LocalPresentColors.current
    val context = LocalContext.current
    
    var showInfoSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            PresentTopBar(
                title = "Analytics",
                onBack = onBack,
                trailingContent = {
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(imageVector = Icons.Rounded.Info, contentDescription = "Info", tint = colors.mutedText)
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "My overall attendance is ${String.format("%.1f", (uiState.overallPercentage ?: 0f) * 100)}% on Present.")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Attendance"))
                    }) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share", tint = colors.mutedText)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.screenHorizontalPadding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .padding(top = 12.dp, bottom = 16.dp)
                ) {
                    RangeSelector(
                        selectedRange = uiState.selectedRange,
                        onRangeSelected = { viewModel.setRange(it) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // Overall Attendance
            item {
                OverallCard(uiState = uiState)
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Trend Chart
            item {
                SectionHeader(title = "ATTENDANCE TREND", subtitle = uiState.trendIntervalLabel)
                Spacer(modifier = Modifier.height(12.dp))
                TrendChartCard(uiState = uiState)
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // Subject Insights
            if (uiState.subjectModels.isNotEmpty()) {
                item {
                    SectionHeader(title = "TRACKED SUBJECTS")
                    Spacer(modifier = Modifier.height(12.dp))
                }
                val warningSubjects = uiState.subjectModels.filter { it.status == SubjectStatus.WARNING || it.status == SubjectStatus.CRITICAL }
                // Insight Card if needed
                if (warningSubjects.isNotEmpty()) {
                    item {
                        InsightCard(count = warningSubjects.size)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                items(uiState.subjectModels) { model ->
                    SubjectAnalyticsRow(model = model)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text("How Analytics work", style = Typography.titleLarge, color = colors.primaryText)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Margin indicates how many classes you can skip while remaining above your target requirement. Deficit indicates how many you must attend to recover.", style = Typography.bodyMedium, color = colors.secondaryText)
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit
) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(100.dp))
            .background(colors.surface)
            .padding(4.dp)
    ) {
        val ranges = listOf(
            AnalyticsRange.SEVEN_DAYS to "7 days",
            AnalyticsRange.THIRTY_DAYS to "30 days",
            AnalyticsRange.ALL_TIME to "All time"
        )
        ranges.forEach { (range, title) ->
            val isSelected = selectedRange == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) colors.elevatedSurface else Color.Transparent)
                    .clickable { onRangeSelected(range) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = Typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (isSelected) colors.primaryText else colors.mutedText
                )
            }
        }
    }
}

@Composable
private fun OverallCard(uiState: AnalyticsUiState) {
    val colors = LocalPresentColors.current
    val pct = uiState.overallPercentage ?: 0f
    val isSafe = pct >= uiState.targetRequirement

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isSafe) colors.safe else colors.critical)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSafe) "Safe standing" else "Attention required",
                style = Typography.labelMedium,
                color = if (isSafe) colors.safe else colors.critical
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${String.format("%.1f", pct * 100)}",
                style = Typography.titleLarge.copy(fontSize = 42.sp, fontWeight = FontWeight.Bold),
                color = colors.primaryText
            )
            Text(
                text = "%",
                style = Typography.titleLarge.copy(color = colors.mutedText),
                modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(colors.elevatedSurface)
        ) {
            val animatedPct by animateFloatAsState(targetValue = pct.coerceIn(0f, 1f), animationSpec = tween(600), label = "")
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(colors.accent)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Target: ${uiState.targetRequirementPercent}%", style = Typography.bodySmall, color = colors.mutedText)
            Text(text = "${uiState.presentCount} P / ${uiState.absentCount} A", style = Typography.bodySmall, color = colors.mutedText)
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String = "") {
    val colors = LocalPresentColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = Typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.sp),
            color = colors.mutedText
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = subtitle,
                style = Typography.labelSmall,
                color = colors.secondaryText
            )
        }
    }
}

@Composable
private fun InsightCard(count: Int) {
    val colors = LocalPresentColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = colors.critical)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Attention Needed", style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.primaryText)
            Text("$count ${if (count == 1) "subject requires" else "subjects require"} effort to hit margins", style = Typography.bodySmall, color = colors.secondaryText)
        }
    }
}

@Composable
private fun SubjectAnalyticsRow(model: SubjectAnalyticsModel) {
    val colors = LocalPresentColors.current
    val progressColor = when (model.status) {
        SubjectStatus.SAFE -> colors.safe
        SubjectStatus.WARNING -> Color(0xFFFBBF24) // specific amber used in MoreScreen
        SubjectStatus.CRITICAL -> colors.critical
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = model.subject.name, style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.primaryText, modifier = Modifier.weight(1f))
            Text(text = "${String.format("%.1f", model.percentage * 100)}%", style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.primaryText)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = model.contextMessage, style = Typography.labelSmall, color = colors.secondaryText)
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(colors.elevatedSurface)
        ) {
            val animatedPct by animateFloatAsState(targetValue = model.percentage.coerceIn(0f, 1f), animationSpec = tween(600), label = "")
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedPct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(progressColor)
            )
            
            // Target line
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineX = size.width * model.target
                drawLine(
                    color = colors.background,
                    start = Offset(lineX, 0f),
                    end = Offset(lineX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun TrendChartCard(uiState: AnalyticsUiState) {
    val colors = LocalPresentColors.current
    val buckets = uiState.trendBuckets
    val target = uiState.targetRequirement

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(vertical = 20.dp, horizontal = 12.dp)
    ) {
        if (buckets.isEmpty()) {
            Text("No data", style = Typography.bodyMedium, color = colors.mutedText, modifier = Modifier.align(Alignment.Center).padding(32.dp))
        } else {
            var selectedIndex by remember(buckets) { mutableStateOf<Int?>(buckets.indices.last.takeIf { it >= 0 }) }
            var isInteracting by remember { mutableStateOf(false) }

            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(buckets) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        isInteracting = true
                                        val paddingX = 16.dp.toPx()
                                        val graphW = size.width - paddingX * 2
                                        val stepX = if (buckets.size > 1) graphW / (buckets.size - 1) else graphW
                                        val idx = ((offset.x - paddingX + stepX/2) / stepX).toInt().coerceIn(0, buckets.lastIndex)
                                        selectedIndex = idx
                                        tryAwaitRelease()
                                        isInteracting = false
                                    }
                                )
                            }
                            .pointerInput(buckets) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isInteracting = true
                                    },
                                    onDragEnd = { isInteracting = false },
                                    onDragCancel = { isInteracting = false },
                                    onDrag = { change, _ ->
                                        val paddingX = 16.dp.toPx()
                                        val graphW = size.width - paddingX * 2
                                        val stepX = if (buckets.size > 1) graphW / (buckets.size - 1) else graphW
                                        val idx = ((change.position.x - paddingX + stepX/2) / stepX).toInt().coerceIn(0, buckets.lastIndex)
                                        selectedIndex = idx
                                    }
                                )
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        
                        val paddingX = 16.dp.toPx()
                        val graphW = w - paddingX * 2
                        val stepX = if (buckets.size > 1) graphW / (buckets.size - 1) else graphW
                        
                        val targetY = h - (target * h)
                        drawLine(
                            color = colors.mutedText.copy(alpha = 0.4f),
                            start = Offset(x = paddingX, y = targetY),
                            end = Offset(x = w - paddingX, y = targetY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                        drawLine(colors.elevatedSurface, Offset(x = paddingX, y = h), Offset(x = w - paddingX, y = h), strokeWidth = 1.dp.toPx())
                        
                        val path = Path()
                        val points = buckets.mapIndexed { idx, bucket ->
                            val pct = bucket.percentage ?: 0f
                            val x = paddingX + idx * stepX
                            val y = h - (pct * h)
                            Offset(x, y)
                        }
                        
                        if (points.isNotEmpty()) {
                            path.moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]
                                val p1 = points[i]
                                val cx1 = p0.x + (p1.x - p0.x) / 2
                                val cy1 = p0.y
                                val cx2 = p0.x + (p1.x - p0.x) / 2
                                val cy2 = p1.y
                                path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                            }
                            
                            drawPath(
                                path = path,
                                color = colors.accent,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                            
                            points.forEachIndexed { idx, pt ->
                                val isSelected = idx == selectedIndex
                                val dotRadius = if (isSelected && isInteracting) 6.dp.toPx() else 4.dp.toPx()
                                drawCircle(colors.background, radius = dotRadius + 1.dp.toPx(), center = pt)
                                drawCircle(
                                    color = if (isSelected) colors.primaryText else colors.accent,
                                    radius = dotRadius,
                                    center = pt
                                )
                            }
                        }
                    }
                    
                    selectedIndex?.let { idx ->
                        val bucket = buckets[idx]
                        val pct = bucket.percentage ?: 0f
                        val pctText = "${String.format("%.1f", pct * 100)}%"
                        
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val wDp = maxWidth
                            val hDp = maxHeight
                            val paddingXDp = 16.dp
                            val graphWDp = wDp - (paddingXDp * 2)
                            val stepXDp = if (buckets.size > 1) graphWDp / (buckets.size - 1) else graphWDp
                            
                            val ptXDp = paddingXDp + (stepXDp * idx)
                            val ptYDp = hDp - (hDp * pct)
                            
                            Box(
                                modifier = Modifier
                                    .absoluteOffset(
                                        x = ptXDp - 22.dp,
                                        y = ptYDp - 34.dp
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.elevatedSurface)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = pctText,
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.primaryText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    buckets.forEachIndexed { idx, b ->
                        val isSelected = idx == selectedIndex
                        Text(
                            text = b.label,
                            style = Typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = if (isSelected) colors.primaryText else colors.mutedText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
