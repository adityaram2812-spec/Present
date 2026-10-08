package com.adityaram.present.domain.import

import java.io.File

interface TimetableImportService {
    suspend fun importFromFile(file: File): Result<Unit>
    suspend fun reviewImport(parsed: ParsedTimetable): Boolean
    suspend fun confirmImport(parsed: ParsedTimetable)
}
