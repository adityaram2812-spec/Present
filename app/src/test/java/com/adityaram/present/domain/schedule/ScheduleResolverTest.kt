package com.adityaram.present.domain.schedule

import com.adityaram.present.data.model.Holiday
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TemporaryLecture
import com.adityaram.present.data.model.TemporaryLectureType
import com.adityaram.present.data.model.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ScheduleResolverTest {

    @Test
    fun `holiday overrides base timetable completely`() {
        val dateEpochDays = LocalDate.of(2026, 10, 12).toEpochDay()
        val baseEntries = listOf(
            TimetableEntry(id = 1, subjectId = 10, startTime = 600, endTime = 660, dayOfWeek = 1, room = null, teacher = null, isRecurring = true)
        )
        val holidays = listOf(Holiday(localDateEpochDays = dateEpochDays))

        val result = ScheduleResolver.getEffectiveScheduleForDate(
            dateEpochDays = dateEpochDays,
            baseEntries = baseEntries,
            temporaryLectures = emptyList(),
            holidays = holidays,
            subjects = emptyList()
        )

        assertEquals(0, result.size)
    }

    @Test
    fun `holiday overrides temporary additions and replacements`() {
        val dateEpochDays = LocalDate.of(2026, 10, 12).toEpochDay()
        val holidays = listOf(Holiday(localDateEpochDays = dateEpochDays))
        val temporaryLectures = listOf(
            TemporaryLecture(id = 1, dateEpochDays = dateEpochDays, startTime = 600, endTime = 660, subjectId = 11, room = null, teacher = null, type = TemporaryLectureType.ADDITION, createdAt = 0)
        )

        val result = ScheduleResolver.getEffectiveScheduleForDate(
            dateEpochDays = dateEpochDays,
            baseEntries = emptyList(),
            temporaryLectures = temporaryLectures,
            holidays = holidays,
            subjects = emptyList()
        )

        assertEquals(0, result.size)
    }

    @Test
    fun `replacement replaces base entry and preserves other entries`() {
        val dateEpochDays = LocalDate.of(2026, 10, 12).toEpochDay()
        val baseEntries = listOf(
            TimetableEntry(id = 1, subjectId = 10, startTime = 600, endTime = 660, dayOfWeek = 1, room = null, teacher = null, isRecurring = true), // To be replaced
            TimetableEntry(id = 2, subjectId = 11, startTime = 700, endTime = 760, dayOfWeek = 1, room = null, teacher = null, isRecurring = true)  // Preserved
        )
        val temporaryLectures = listOf(
            TemporaryLecture(id = 1, dateEpochDays = dateEpochDays, startTime = 600, endTime = 660, subjectId = 12, room = null, teacher = null, type = TemporaryLectureType.REPLACEMENT, replacesEntryId = 1, createdAt = 0)
        )

        val result = ScheduleResolver.getEffectiveScheduleForDate(
            dateEpochDays = dateEpochDays,
            baseEntries = baseEntries,
            temporaryLectures = temporaryLectures,
            holidays = emptyList(),
            subjects = emptyList()
        )

        assertEquals(2, result.size)
        // Check replaced
        assertEquals(12L, result.find { it.startTime == 600 }?.subjectId)
        assertEquals(true, result.find { it.startTime == 600 }?.isTemporary)
        // Check preserved
        assertEquals(11L, result.find { it.startTime == 700 }?.subjectId)
        assertEquals(false, result.find { it.startTime == 700 }?.isTemporary)
    }

    @Test
    fun `addition appends to effective schedule natively`() {
        val dateEpochDays = LocalDate.of(2026, 10, 12).toEpochDay()
        val baseEntries = listOf(
            TimetableEntry(id = 1, subjectId = 10, startTime = 600, endTime = 660, dayOfWeek = 1, room = null, teacher = null, isRecurring = true)
        )
        val temporaryLectures = listOf(
            TemporaryLecture(id = 1, dateEpochDays = dateEpochDays, startTime = 700, endTime = 760, subjectId = 15, room = null, teacher = null, type = TemporaryLectureType.ADDITION, createdAt = 0)
        )

        val result = ScheduleResolver.getEffectiveScheduleForDate(
            dateEpochDays = dateEpochDays,
            baseEntries = baseEntries,
            temporaryLectures = temporaryLectures,
            holidays = emptyList(),
            subjects = emptyList()
        )

        assertEquals(2, result.size)
        assertEquals(15L, result.find { it.startTime == 700 }?.subjectId)
        assertEquals(true, result.find { it.startTime == 700 }?.isTemporary)
    }
}
