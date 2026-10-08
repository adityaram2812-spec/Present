package com.adityaram.present.data.import

import com.adityaram.present.domain.import.Confidence
import com.adityaram.present.domain.import.DayNormalizer
import com.adityaram.present.domain.import.ParsedTimetable
import com.adityaram.present.domain.import.ParsedTimetableClass
import com.adityaram.present.domain.import.TimeNormalizer
import com.adityaram.present.domain.import.TimetableParser
import com.adityaram.present.domain.import.ExtractedTextLine
import com.adityaram.present.domain.import.DocumentLayoutData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.File
import java.io.FileInputStream

class SpreadsheetTimetableParser : TimetableParser {

    override suspend fun analyzeDocument(file: File): Result<DocumentLayoutData> = withContext(Dispatchers.IO) {
        try {
            val isExcel = file.name.endsWith(".xlsx", ignoreCase = true)
            val lines = mutableListOf<ExtractedTextLine>()
            
            val foundGroups = mutableSetOf<String>()
            val groupRegex = Regex("""(Div(?:ision)?[- ]?[A-Z0-9]+|Batch[- ]?[A-Z0-9]+|Group[- ]?[A-Z0-9]+)""", RegexOption.IGNORE_CASE)
            
            if (isExcel) {
                FileInputStream(file).use { fis ->
                    ReadableWorkbook(fis).use { wb ->
                        val sheet = wb.firstSheet
                        sheet.openStream().use { stream ->
                            var rowIdx = 0
                            stream.forEach { row ->
                                var colIdx = 0
                                for (cell in row) {
                                    val text = cell.text ?: ""
                                    if (text.isNotBlank()) {
                                        lines.add(ExtractedTextLine(text, null, 1.0f, 0, rowIdx, colIdx))
                                        val match = groupRegex.find(text)
                                        if (match != null) foundGroups.add(match.value)
                                    }
                                    colIdx++
                                }
                                rowIdx++
                            }
                        }
                    }
                }
            } else {
                file.useLines { fileLines ->
                    var rowIdx = 0
                    fileLines.forEach { lineText ->
                        val tokens = lineText.split(",")
                        var colIdx = 0
                        for (token in tokens) {
                            if (token.isNotBlank()) {
                                lines.add(ExtractedTextLine(token.trim(), null, 1.0f, 0, rowIdx, colIdx))
                                val match = groupRegex.find(token.trim())
                                if (match != null) foundGroups.add(match.value)
                            }
                            colIdx++
                        }
                        rowIdx++
                    }
                }
            }
            Result.success(DocumentLayoutData(lines, foundGroups.toList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractClasses(layoutData: DocumentLayoutData, selectedGroup: String?, isFullTimetable: Boolean): Result<ParsedTimetable> = withContext(Dispatchers.Default) {
        try {
            val allClasses = mutableListOf<ParsedTimetableClass>()
            
            val rows = layoutData.lines.groupBy { it.row ?: 0 }
            for ((_, cols) in rows) {
                   val rowText = cols.joinToString(" ") { it.text }
                   if (rowText.isNotBlank()) {
                       var allowed = true
                       if (!isFullTimetable && selectedGroup != null) {
                           // Quick exclusion if row starts with a different prefix
                           val selectedPrefixMatch = Regex("""([A-Z0-9]+)""").findAll(selectedGroup).lastOrNull()?.value
                           if (selectedPrefixMatch != null) {
                               val prefixMatch = Regex("""^([A-Z0-9]+)\s*-""").find(cols.first().text)
                               if (prefixMatch != null && !prefixMatch.groupValues[1].equals(selectedPrefixMatch, ignoreCase = true)) {
                                   allowed = false
                               }
                           }
                       }
                       if (allowed) {
                           parseRowText(rowText)?.let { allClasses.add(it) }
                       }
                   }
            }
            Result.success(ParsedTimetable(allClasses))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    private fun parseRowText(fullText: String): ParsedTimetableClass? {
        if (fullText.trim().length <= 3) return null
        if (fullText.contains("Time", ignoreCase = true) && fullText.contains("Day", ignoreCase = true)) return null

        val timePair = TimeNormalizer.extractTimePairs(fullText)
        val day = DayNormalizer.parseDay(fullText)
        
        val timeStrings = "([0-1]?[0-9]|2[0-3])[:.]([0-5][0-9])\\s*(am|pm|a\\.m\\.|p\\.m\\.)?|(\\d+)\\s*-\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
        val dayStrings = "(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)".toRegex(RegexOption.IGNORE_CASE)

        val subjectCand = fullText.replace(timeStrings, "").replace(dayStrings, " ").trim()
        val finalSubject = subjectCand.replace("\\s+".toRegex(), " ").let { s -> if (s.isNotBlank()) s else null }
        
        val conf = if (timePair != null && day != null && finalSubject != null) Confidence.HIGH else Confidence.LOW
        val reason = mutableListOf<String>()
        if (timePair == null) reason.add("Time couldn't be identified")
        if (day == null) reason.add("Day couldn't be identified")
        if (finalSubject == null) reason.add("Subject is unclear")
        
        if (finalSubject != null || timePair != null || day != null) {
            return ParsedTimetableClass(
                extractedText = fullText,
                dayOfWeek = day?.value,
                startTime = timePair?.first,
                endTime = timePair?.second,
                subjectName = finalSubject,
                room = null,
                teacher = null,
                confidence = conf,
                uncertaintyReason = reason.joinToString(", ").takeIf { it.isNotEmpty() }
            )
        }
        return null
    }
}
