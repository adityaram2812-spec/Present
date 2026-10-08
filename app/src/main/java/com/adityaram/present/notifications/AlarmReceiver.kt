package com.adityaram.present.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adityaram.present.PresentApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalTime

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("type") ?: return
        val title = intent.getStringExtra("title") ?: return
        val message = intent.getStringExtra("message") ?: return
        val reqCode = intent.getIntExtra("reqCode", 0)

        val app = context.applicationContext as PresentApplication
        val prefs = app.container.userPreferencesRepository

        CoroutineScope(Dispatchers.IO).launch {
            val isMasterEnabled = prefs.notificationsEnabled.first()
            if (!isMasterEnabled) return@launch

            val isQuietMode = prefs.quietModeEnabled.first()
            if (isQuietMode) {
                // Fixed 22:00 -> 07:00 rule
                val now = LocalTime.now()
                val isInsideWindow = now.isAfter(LocalTime.of(21, 59)) || now.isBefore(LocalTime.of(7, 1))
                if (isInsideWindow) return@launch // Suppressed
            }

            // Verify specific toggles matching logic from scheduling
            if (type == AlarmScheduler.TYPE_UPCOMING) {
                if (!prefs.upcomingClassEnabled.first()) return@launch
            } else if (type == AlarmScheduler.TYPE_ATTENDANCE) {
                if (!prefs.attendanceCheckEnabled.first()) return@launch
            }
            
            // Does not mutate AttendanceRecord DB here.
            NotificationHelper.setupChannel(context)
            NotificationHelper.sendNotification(context, title, message, reqCode)
        }
    }
}
