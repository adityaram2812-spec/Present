package com.adityaram.present.domain.import

import com.adityaram.present.data.model.Subject
import com.adityaram.present.data.model.TimetableEntry
import android.graphics.Rect
import java.io.File
import java.util.UUID

enum class Confidence { HIGH, LOW }

data class ParsedTimetableClass(
    val id: String = UUID.randomUUID().toString(),
    val extractedText: String = "", 
    val dayOfWeek: Int? = null,
    val startTime: Int? = null,
    val endTime: Int? = null,
    val subjectName: String? = null,
    val room: String? = null,
    val teacher: String? = null,
    val confidence: Confidence = Confidence.HIGH,
    val uncertaintyReason: String? = null
)

data class ParsedTimetable(
    val classes: List<ParsedTimetableClass>
)
data class ExtractedTextLine(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float = 1.0f,
    val page: Int = 0,
    val row: Int? = null,
    val col: Int? = null
)

data class DocumentLayoutData(
    val lines: List<ExtractedTextLine> = emptyList(),
    val detectedGroups: List<String> = emptyList(),
    val aiResponseJson: String? = null
)

interface TimetableParser {
    suspend fun analyzeDocument(file: File): Result<DocumentLayoutData>
    suspend fun extractClasses(layoutData: DocumentLayoutData, selectedGroup: String?, isFullTimetable: Boolean): Result<ParsedTimetable>
}
