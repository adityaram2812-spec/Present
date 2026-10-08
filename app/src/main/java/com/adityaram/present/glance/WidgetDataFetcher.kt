package com.adityaram.present.glance

import android.content.Context
import com.adityaram.present.PresentApplication
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.parseOpeningBalances
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

data class WidgetClassModel(
    val entryId: Long,
    val subjectId: Long,
    val subjectName: String,
    val startTime: Int,
    val endTime: Int,
    val attendanceStatus: AttendanceStatus?,
    val recordId: Long?
)

data class PresentWidgetData(
    val hasTimetable: Boolean,
    val targetPercentage: Int,
    val overallPercentage: Float,
    val isSafe: Boolean,
    val todayClasses: List<WidgetClassModel>,
    val nextClass: WidgetClassModel?
)

object WidgetDataFetcher {
    suspend fun fetchWidgetData(context: Context): PresentWidgetData {
        val app = (context.applicationContext as PresentApplication)
        val timetableRepo = app.container.timetableRepository
        val attendanceRepo = app.container.attendanceRepository
        val prefs = app.container.userPreferencesRepository

        val requirement = prefs.attendanceRequirement.first()
        val balancesJson = prefs.openingBalances.first()
        val records = attendanceRepo.getAllRecords().first()
        val subjects = timetableRepo.getAllSubjects().first()
        val entries = timetableRepo.getAllEntries().first()
        
        val openingBalances = parseOpeningBalances(balancesJson)
        val hasTimetable = entries.isNotEmpty()
        
        var globalPresentCount = 0
        var overallTotal = 0
        
        subjects.forEach { subject ->
            val ob = openingBalances[subject.id]
            val validSubjectRecords = records.filter { it.subjectId == subject.id && it.status != AttendanceStatus.CANCELLED }
            
            val presentCount = validSubjectRecords.count { it.status == AttendanceStatus.PRESENT } + (ob?.present ?: 0)
            val absentCount = validSubjectRecords.count { it.status == AttendanceStatus.ABSENT } + (ob?.absent ?: 0)
            val totalEligible = presentCount + absentCount
            
            globalPresentCount += presentCount
            overallTotal += totalEligible
        }
        
        val overallPercentage = attendanceRepo.calculateAttendanceRatio(globalPresentCount, overallTotal)
        val reqFraction = requirement / 100f
        val isSafe = overallPercentage >= reqFraction

        val today = LocalDate.now(ZoneId.systemDefault())
        val currentDayOfWeek = today.dayOfWeek.value
        val todayStartEpoch = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

        val todayEntries = entries
            .filter { it.dayOfWeek == currentDayOfWeek }
            .distinctBy { Pair(it.subjectId, it.startTime) }
            .sortedBy { it.startTime }
        
        val todayRecords = records.filter { it.date == todayStartEpoch }
        
        val nowMinutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        var nextClass: WidgetClassModel? = null

        val classesToday = todayEntries.map { entry ->
            val subject = subjects.find { it.id == entry.subjectId }
            val record = todayRecords.find { it.subjectId == entry.subjectId && it.startTime == entry.startTime }
            
            val model = WidgetClassModel(
                entryId = entry.id,
                subjectId = entry.subjectId,
                subjectName = subject?.name ?: "Unknown Subject",
                startTime = entry.startTime,
                endTime = entry.endTime,
                attendanceStatus = record?.status,
                recordId = record?.id
            )
            
            if (nextClass == null && entry.startTime > nowMinutes) {
                nextClass = model
            }
            
            model
        }

        return PresentWidgetData(
            hasTimetable = hasTimetable,
            targetPercentage = requirement,
            overallPercentage = overallPercentage,
            isSafe = isSafe,
            todayClasses = classesToday,
            nextClass = nextClass
        )
    }
}
