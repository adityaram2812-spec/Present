package com.adityaram.present.domain.import

import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TimetableEntry
import com.adityaram.present.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import android.content.Context
import android.net.Uri

class TimetableImportServiceImpl(
    private val timetableRepository: TimetableRepository
) {

    fun hasExistingTimetable() = timetableRepository.hasTimetableEntries()

    suspend fun confirmImport(parsed: ParsedTimetable, runAsReplacement: Boolean = false) {
        val existingSubjects = timetableRepository.getAllSubjects().first()
        val generatedSubjects = mutableMapOf<String, Long>()
        val finalEntries = mutableListOf<TimetableEntry>()

        // 1. Process subjects with exact whitespace trimming deduplication matching
        for (item in parsed.classes) {
            val rawSubject = item.subjectName?.trim()?.replace("\\s+".toRegex(), " ") ?: continue
            val existingSubjectId = existingSubjects.find { it.name.equals(rawSubject, ignoreCase = true) }?.id
                ?: generatedSubjects[rawSubject]
                ?: run {
                    val newSubject = Subject(
                        name = rawSubject,
                        teacher = null,
                        minimumAttendance = 75f, // Default standard policy for new imported subjects natively
                        createdAt = System.currentTimeMillis()
                    )
                    val insertedId = timetableRepository.insertSubject(newSubject)
                    generatedSubjects[rawSubject] = insertedId
                    insertedId
                }
            
            // 2. Build entry if vital fields present
            val day = item.dayOfWeek ?: continue
            val startTime = item.startTime ?: continue
            val endTime = item.endTime ?: continue

            finalEntries.add(
                TimetableEntry(
                    subjectId = existingSubjectId,
                    dayOfWeek = day,
                    startTime = startTime,
                    endTime = endTime,
                    room = item.room?.takeIf { it.isNotBlank() },
                    teacher = item.teacher?.takeIf { it.isNotBlank() },
                    isRecurring = true,
                    specificDateMillis = null
                )
            )
        }

        // 3. Persist natively inside Room boundary
        println("DEBUG FLOW: Classes actually persisted in Room = ${finalEntries.size}")
        if (runAsReplacement) {
            timetableRepository.replaceTimetable {
                timetableRepository.insertEntries(finalEntries)
            }
        } else {
            timetableRepository.insertEntries(finalEntries)
        }
    }
}
