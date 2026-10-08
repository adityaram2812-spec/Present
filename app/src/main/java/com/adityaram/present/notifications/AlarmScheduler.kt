package com.adityaram.present.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.AttendanceStatus
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object AlarmScheduler {
    const val TYPE_UPCOMING = "upcoming"
    const val TYPE_ATTENDANCE = "attendance"
    
    suspend fun scheduleAlarms(context: Context) {
        val app = context.applicationContext as PresentApplication
        val prefs = app.container.userPreferencesRepository
        val timetable = app.container.timetableRepository
        val attendanceRepo = app.container.attendanceRepository
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return // Fail gracefully without crashing
            }
        }
        
        val isMasterEnabled = prefs.notificationsEnabled.first()
        if (!isMasterEnabled) return
        
        val upcomingEnabled = prefs.upcomingClassEnabled.first()
        val upcomingTiming = UpcomingClassTiming.fromLabel(prefs.upcomingClassTiming.first())
        
        val attendanceEnabled = prefs.attendanceCheckEnabled.first()
        val attendanceTiming = AttendanceCheckTiming.fromLabel(prefs.attendanceCheckTiming.first())
        
        val entries = timetable.getAllEntries().first()
        val subjects = timetable.getAllSubjects().first()
        val records = attendanceRepo.getAllRecords().first()
        
        entries.forEach { entry ->
            val subject = subjects.find { it.id == entry.subjectId } ?: return@forEach
            
            // Single source of truth calculation reusing LocalDate logic 
            val nextDate = if (entry.specificDateMillis != null) {
                val date = java.time.Instant.ofEpochMilli(entry.specificDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                if (date.isBefore(LocalDate.now())) return@forEach
                date
            } else {
                var d = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(entry.dayOfWeek)))
                val classDateTime = d.atTime(entry.startTime / 60, entry.startTime % 60)
                if (classDateTime.isBefore(LocalDateTime.now())) {
                    d = d.plusWeeks(1)
                }
                d
            }
            
            val targetStartEpoch = nextDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val record = records.find { it.subjectId == entry.subjectId && it.date == targetStartEpoch && it.startTime == entry.startTime }
            if (record?.status == AttendanceStatus.CANCELLED) return@forEach
            
            // Generate deterministic reqHash to update/replace intent successfully (no accumulating duplicates)
            val reqHashUpcoming = (entry.id * 31 + entry.subjectId).toInt() + targetStartEpoch.hashCode()
            val reqHashAttendance = reqHashUpcoming + 1
            
            if (upcomingEnabled) {
                val classDateTime = nextDate.atTime(entry.startTime / 60, entry.startTime % 60)
                val alarmTime = classDateTime.minusMinutes(upcomingTiming.offsetMinutes.toLong())
                if (alarmTime.isAfter(LocalDateTime.now())) {
                    scheduleExact(context, alarmManager, alarmTime, reqHashUpcoming, TYPE_UPCOMING, subject.name, "Upcoming class in ${upcomingTiming.label}")
                }
            }
            
            if (attendanceEnabled) {
                val alarmTime = if (attendanceTiming.isEvening) {
                    nextDate.atTime(18, 0) // Explicit 18:00 mapping
                } else {
                    nextDate.atTime(entry.endTime / 60, entry.endTime % 60).plusMinutes(attendanceTiming.offsetMinutes.toLong())
                }
                
                if (alarmTime.isAfter(LocalDateTime.now())) {
                    scheduleExact(context, alarmManager, alarmTime, reqHashAttendance, TYPE_ATTENDANCE, subject.name, "Did you attend your recent class?")
                }
            }
        }
    }
    
    private fun scheduleExact(context: Context, alarmManager: AlarmManager, time: LocalDateTime, reqCode: Int, type: String, title: String, message: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("type", type)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("reqCode", reqCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: SecurityException) {
            // Ignored gracefully as requested
        }
    }
}
