package com.adityaram.present.domain.schedule

import com.adityaram.present.data.model.Holiday
import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TemporaryLecture
import com.adityaram.present.data.model.TemporaryLectureType
import com.adityaram.present.data.model.TimetableEntry
import java.time.LocalDate

object ScheduleResolver {
    
    /**
     * Resolves the effective schedule for a specific date given the raw data.
     * Order of precedence:
     * 1. Base timetable (resolved via dayOfWeek of the passed date)
     * 2. Holiday override (empties schedule)
     * 3. Temporary replacements (substitutes specific occurrences)
     * 4. Temporary additions (adds new occurrences)
     *
     * @param dateEpochDays The specific date to resolve.
     * @param baseEntries The full list of base timetable entries (typically all, or pre-filtered to the day of week).
     * @param temporaryLectures The list of temporary lectures for this specific date.
     * @param holidays The full list of holidays (or pre-filtered).
     * @param subjects The full list of subjects to resolve labels.
     */
    fun getEffectiveScheduleForDate(
        dateEpochDays: Long,
        baseEntries: List<TimetableEntry>,
        temporaryLectures: List<TemporaryLecture>,
        holidays: List<Holiday>,
        subjects: List<Subject>
    ): List<EffectiveLecture> {
        
        // 1. Holiday Check
        val isHoliday = holidays.any { it.localDateEpochDays == dateEpochDays }
        if (isHoliday) {
            return emptyList()
        }

        // 2. Base Timetable Mapping
        val date = LocalDate.ofEpochDay(dateEpochDays)
        val dayOfWeek = date.dayOfWeek.value
        
        // Match base entries for this day of week, distinct by subject+startTime logic used previously
        val validBaseEntries = baseEntries
            .filter { it.dayOfWeek == dayOfWeek }
            .distinctBy { Pair(it.subjectId, it.startTime) }

        // Filter temporary lectures for this date specifically
        val dateTemporaryLectures = temporaryLectures.filter { it.dateEpochDays == dateEpochDays }
        
        val additions = dateTemporaryLectures.filter { it.type == TemporaryLectureType.ADDITION }
        val replacements = dateTemporaryLectures.filter { it.type == TemporaryLectureType.REPLACEMENT }
        
        val effectiveList = mutableListOf<EffectiveLecture>()
        
        // 3. Process Base and apply Replacements
        for (baseEntry in validBaseEntries) {
            // Check if there is a replacement targeting this specific base entry
            val replacement = replacements.find { it.replacesEntryId == baseEntry.id }
            
            if (replacement != null) {
                // Replaced
                val originalSubject = subjects.find { it.id == baseEntry.subjectId }
                
                effectiveList.add(
                    EffectiveLecture(
                        baseEntry = null, // The replacement takes its place fully
                        temporaryLecture = replacement,
                        subjectId = replacement.subjectId,
                        startTime = replacement.startTime,
                        endTime = replacement.endTime,
                        isTemporary = true,
                        temporaryLabel = "Replaces ${originalSubject?.name ?: "scheduled class"}"
                    )
                )
            } else {
                // Keep base entry intact
                effectiveList.add(
                    EffectiveLecture(
                        baseEntry = baseEntry,
                        temporaryLecture = null,
                        subjectId = baseEntry.subjectId,
                        startTime = baseEntry.startTime,
                        endTime = baseEntry.endTime,
                        isTemporary = false,
                        temporaryLabel = null
                    )
                )
            }
        }
        
        // 4. Process Additions
        for (addition in additions) {
            effectiveList.add(
                EffectiveLecture(
                    baseEntry = null,
                    temporaryLecture = addition,
                    subjectId = addition.subjectId,
                    startTime = addition.startTime,
                    endTime = addition.endTime,
                    isTemporary = true,
                    temporaryLabel = "Extra lecture"
                )
            )
        }
        
        // 5. Sort chronologically
        return effectiveList.sortedBy { it.startTime }
    }
}
