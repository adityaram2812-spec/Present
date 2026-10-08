package com.adityaram.present.ui.planner

import com.adityaram.present.data.model.AttendanceRecord
import com.adityaram.present.data.model.AttendanceStatus
import com.adityaram.present.data.model.Holiday
import com.adityaram.present.data.model.LeavePlan
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class LeavePlannerLogicTest {
    
    private lateinit var subjects: List<Subject>
    private lateinit var entries: List<TimetableEntry>
    private lateinit var records: List<AttendanceRecord>
    
    @Before
    fun setup() {
        // Mock 1 Subject
        subjects = listOf(Subject(id = 1L, name = "Math", teacher = null, minimumAttendance = 75f, createdAt = 0L))
        
        // Mock Timetable: 2 lectures on Mondays, 1 on Tuesdays, none on weekends.
        // Monday = 1, Tuesday = 2
        entries = listOf(
            TimetableEntry(id = 1, subjectId = 1L, dayOfWeek = 1, startTime = 540, endTime = 600, room = "", teacher = null, isRecurring = true),
            TimetableEntry(id = 2, subjectId = 1L, dayOfWeek = 1, startTime = 600, endTime = 660, room = "", teacher = null, isRecurring = true),
            TimetableEntry(id = 3, subjectId = 1L, dayOfWeek = 2, startTime = 540, endTime = 600, room = "", teacher = null, isRecurring = true)
        )
        
        // Mock base attendance: 10 eligible lectures, 8 present.
        // For simplicity, we just inject exact historical dates
        val historicalRecords = mutableListOf<AttendanceRecord>()
        for(i in 1..8) {
            historicalRecords.add(AttendanceRecord(subjectId = 1L, date = i.toLong(), startTime = 540, status = AttendanceStatus.PRESENT, note = null))
        }
        for(i in 9..10) {
            historicalRecords.add(AttendanceRecord(subjectId = 1L, date = i.toLong(), startTime = 600, status = AttendanceStatus.ABSENT, note = null))
        }
        records = historicalRecords
    }

    @Test
    fun `test one day leave projection`() {
        // Simulate a leave strictly on a future Monday
        // Monday date = 2026-10-12 (which is DayOfWeek 1)
        val mondayEpoch = LocalDate.of(2026, 10, 12).toEpochDay()
        
        val plans = listOf(LeavePlan(id = 1L, startDateEpochDays = mondayEpoch, endDateEpochDays = mondayEpoch))
        
        val result = LeavePlannerLogic.calculateProjection(plans, entries, emptyList(), records, subjects, emptyList())
        
        // Baseline: 8/10 = 0.8
        assertEquals(0.8f, result.currentAttendance, 0.001f)
        
        // Monday has 2 lectures, both are in the future and unrecorded. Result should affect 2 lectures.
        assertEquals(2, result.totalAffectedLectures)
        
        // Projected: 8/12 = 0.666
        assertEquals(1, result.planModels.size)
        assertEquals(2, result.planModels[0].affectedLectures)
        assertEquals(8f / 12f, result.projectedAttendance, 0.001f)
    }

    @Test
    fun `test date range containing weekend`() {
        // Leave from Saturday to Tuesday
        // Sat = 2026-10-10, Sun = 2026-10-11, Mon = 2026-10-12, Tue = 2026-10-13
        val startEpoch = LocalDate.of(2026, 10, 10).toEpochDay()
        val endEpoch = LocalDate.of(2026, 10, 13).toEpochDay()
        
        val plans = listOf(LeavePlan(id = 1L, startDateEpochDays = startEpoch, endDateEpochDays = endEpoch))
        
        val result = LeavePlannerLogic.calculateProjection(plans, entries, emptyList(), records, subjects, emptyList())
        
        // Affected: Sat=0, Sun=0, Mon=2, Tue=1 -> Total 3
        assertEquals(3, result.totalAffectedLectures)
        assertEquals(3, result.planModels[0].affectedLectures)
        
        // Projected: 8/13
        assertEquals(8f / 13f, result.projectedAttendance, 0.001f)
    }

    @Test
    fun `test date range containing holiday`() {
        // Leave on Monday (2 lectures)
        val mondayEpoch = LocalDate.of(2026, 10, 12).toEpochDay()
        val plans = listOf(LeavePlan(id = 1L, startDateEpochDays = mondayEpoch, endDateEpochDays = mondayEpoch))
        
        // Monday is declared a holiday
        val holidays = listOf(Holiday(localDateEpochDays = mondayEpoch))
        
        val result = LeavePlannerLogic.calculateProjection(plans, entries, holidays, records, subjects, emptyList())
        
        // Holiday nullifies the expected missed lectures
        assertEquals(0, result.totalAffectedLectures)
        assertEquals(0, result.planModels[0].affectedLectures)
        assertEquals(0.8f, result.projectedAttendance, 0.001f) // Unchanged
    }

    @Test
    fun `test overlapping leave plans without double count`() {
        val mondayEpoch = LocalDate.of(2026, 10, 12).toEpochDay()
        val tuesdayEpoch = LocalDate.of(2026, 10, 13).toEpochDay()
        
        // Plan 1: Monday and Tuesday
        val p1 = LeavePlan(id = 1L, startDateEpochDays = mondayEpoch, endDateEpochDays = tuesdayEpoch)
        // Plan 2: Tuesday only (perfect overlap)
        val p2 = LeavePlan(id = 2L, startDateEpochDays = tuesdayEpoch, endDateEpochDays = tuesdayEpoch)
        
        val result = LeavePlannerLogic.calculateProjection(listOf(p1, p2), entries, emptyList(), records, subjects, emptyList())
        
        // Plan 1 affected: Mon=2, Tue=1 = 3
        // Plan 2 affected: Tue=1 = 1
        assertEquals(3, result.planModels.find { it.entity.id == 1L }?.affectedLectures)
        assertEquals(1, result.planModels.find { it.entity.id == 2L }?.affectedLectures)
        
        // But globally deduplicated, it should be Mon(2) + Tue(1) = 3 total.
        assertEquals(3, result.totalAffectedLectures)
        assertEquals(8f / 13f, result.projectedAttendance, 0.001f)
    }
    
    @Test
    fun `test already recorded attendance is not overwritten`() {
        val mondayEpoch = LocalDate.of(2026, 10, 12).toEpochDay()
        
        // Let's say user marked presence for the 540 startTime on the future Monday.
        val modifiedRecords = records.toMutableList()
        modifiedRecords.add(AttendanceRecord(subjectId = 1L, date = mondayEpoch, startTime = 540, status = AttendanceStatus.PRESENT, note = null))
        
        val plans = listOf(LeavePlan(id = 1L, startDateEpochDays = mondayEpoch, endDateEpochDays = mondayEpoch))
        
        // In this case, 11 total eligible, 9 present. Baseline: 9/11
        val result = LeavePlannerLogic.calculateProjection(plans, entries, emptyList(), modifiedRecords, subjects, emptyList())
        
        // Monday has 2 lectures (540, 600). The 540 is already recorded as PRESENT.
        // Therefore, the Leave Plan should ONLY affect the 600 slot.
        assertEquals(1, result.totalAffectedLectures)
        
        // Projected: 9 / (11 total existing + 1 affected future slot) = 9/12
        assertEquals(9f / 12f, result.projectedAttendance, 0.001f)
    }
}
