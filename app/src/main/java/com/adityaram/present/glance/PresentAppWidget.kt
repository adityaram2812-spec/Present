package com.adityaram.present.glance

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adityaram.present.MainActivity
import java.util.Locale

class PresentAppWidget : GlanceAppWidget() {
    
    override val sizeMode = SizeMode.Responsive(
        setOf(
            androidx.compose.ui.unit.DpSize(110.dp, 110.dp), // Small 2x2 Target
            androidx.compose.ui.unit.DpSize(250.dp, 110.dp)  // Medium 4x2 Target
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = try {
            WidgetDataFetcher.fetchWidgetData(context)
        } catch (e: Exception) {
            PresentWidgetData(false, 75, 0f, false, emptyList(), null)
        }

        provideContent {
            val size = LocalSize.current
            val isSmall = size.width < 200.dp
            
            Column(
                modifier = GlanceModifier.fillMaxSize()
                    .appWidgetBackground()
                    .background(Color(0xFFFFFFFF))
                    .cornerRadius(18.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java)))
            ) {
                if (isSmall) {
                    SmallWidgetLayout(widgetData)
                } else {
                    MediumWidgetLayout(widgetData)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun SmallWidgetLayout(data: PresentWidgetData) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Present", 
                    style = TextStyle(color = ColorProvider(Color(0xFF7566E8)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = String.format(Locale.getDefault(), "%.1f%%", data.overallPercentage * 100),
                style = TextStyle(color = ColorProvider(Color(0xFF1C1B1F)), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Attendance",
                style = TextStyle(color = ColorProvider(Color(0xFF49454F)), fontSize = 12.sp)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            Text(
                text = "${data.todayClasses.size} lectures today",
                style = TextStyle(color = ColorProvider(Color(0xFF49454F)), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            val safeColor = if (data.isSafe) Color(0xFF22C55E) else Color(0xFFEF4444)
            val safeText = if (data.isSafe) "✓ On track" else "! Needs attention"
            Text(
                text = safeText,
                style = TextStyle(color = ColorProvider(safeColor), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun MediumWidgetLayout(data: PresentWidgetData) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Present", 
                    style = TextStyle(color = ColorProvider(Color(0xFF7566E8)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", data.overallPercentage * 100),
                    style = TextStyle(color = ColorProvider(Color(0xFF1C1B1F)), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "Attendance",
                    style = TextStyle(color = ColorProvider(Color(0xFF49454F)), fontSize = 12.sp)
                )
            }
            
            Spacer(modifier = GlanceModifier.height(16.dp))

            if (data.todayClasses.isEmpty()) {
                Spacer(modifier = GlanceModifier.defaultWeight())
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No lectures today.",
                        style = TextStyle(color = ColorProvider(Color(0xFF49454F)), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
            } else {
                Text(
                    text = "Today's lectures",
                    style = TextStyle(color = ColorProvider(Color(0xFF49454F)), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                
                data.todayClasses.take(3).forEach { cls ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", cls.startTime / 60, cls.startTime % 60)
                        Text(
                            text = timeStr, 
                            style = TextStyle(color = ColorProvider(Color(0xFF7566E8)), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Text(
                            text = cls.subjectName, 
                            style = TextStyle(color = ColorProvider(Color(0xFF1C1B1F)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
