package com.adityaram.present.ui.planner

import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.Holiday
import com.adityaram.present.data.model.LeavePlan
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TemporaryLecture
import com.adityaram.present.data.model.TimetableEntry
import java.time.LocalDate

object LeavePlannerLogic {
    data class ProjectionResult(
        val planModels: List<LeavePlanModel>,
        val currentAttendance: Float,
        val totalAffectedLectures: Int,
        val projectedAttendance: Float
    )
    
    fun calculateProjection(
        plans: List<LeavePlan>,
        entries: List<TimetableEntry>,
        holidays: List<Holiday>,
        records: List<AttendanceRecord>,
        subjects: List<Subject>,
        temporaryLectures: List<TemporaryLecture>
    ): ProjectionResult {
        val holidayDates = holidays.map { it.localDateEpochDays }.toSet()
        
        var globalPresent = 0
        var globalTotal = 0
        
        val validRecords = records.filter { it.status != AttendanceStatus.CANCELLED }
        
        for (subject in subjects) {
            val subjRecords = validRecords.filter { it.subjectId == subject.id }
            val presentCount = subjRecords.count { it.status == AttendanceStatus.PRESENT }
            
            globalPresent += presentCount
            globalTotal += subjRecords.size
        }
        
        val currentAttendance = if (globalTotal > 0) globalPresent.toFloat() / globalTotal.toFloat() else 1f
        
        val allProjectedMisses = mutableSetOf<Triple<Long, Long, Int>>()
        
        val planModels = plans.map { plan ->
            var planAffectedCount = 0
            val duration = (plan.endDateEpochDays - plan.startDateEpochDays + 1).toInt()
            
            for (d in plan.startDateEpochDays..plan.endDateEpochDays) {
                // Let ScheduleResolver handle holidays natively! (It returns emptyList if holiday)
                val dailyLectures = com.adityaram.present.domain.schedule.ScheduleResolver.getEffectiveScheduleForDate(
                    dateEpochDays = d,
                    baseEntries = entries,
                    temporaryLectures = temporaryLectures,
                    holidays = holidays,
                    subjects = subjects
                )
                
                for (lecture in dailyLectures) {
                    val existing = records.find { it.date == d && it.subjectId == lecture.subjectId && it.startTime == lecture.startTime }
                    if (existing == null) {
                        planAffectedCount++
                        allProjectedMisses.add(Triple(d, lecture.subjectId, lecture.startTime))
                    }
                }
            }
            
            LeavePlanModel(
                entity = plan,
                durationDays = duration,
                affectedLectures = planAffectedCount
            )
        }
        
        val totalAffected = allProjectedMisses.size
        val projectedTotal = globalTotal + totalAffected
        val projectedAttendance = if (projectedTotal > 0) globalPresent.toFloat() / projectedTotal.toFloat() else 1f

        return ProjectionResult(
            planModels = planModels,
            currentAttendance = currentAttendance,
            totalAffectedLectures = totalAffected,
            projectedAttendance = projectedAttendance
        )
    }
}
