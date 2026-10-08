package com.adityaram.present.data.import

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.adityaram.present.domain.import.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class AiTimetableParser(
    private val provider: TimetableExtractionProvider = GeminiExtractionProvider()
) : TimetableParser {

    private val gson = Gson()

    override suspend fun analyzeDocument(file: File): Result<DocumentLayoutData> {
        try { android.util.Log.d("PresentImport", "IMPORT_EXTRACTION_STARTED") } catch (e: Exception) {}
        
        if (com.adityaram.present.BuildConfig.DEBUG) {
            val traceModeEnabled = false // Set to false to avoid breaking normal app runs and tests
            if (traceModeEnabled) {
                try { android.util.Log.d("PresentImport", "IMPORT_PROVIDER_CALL_WOULD_HAPPEN") } catch (e: Exception) {}
                return Result.failure(Exception("DEBUG TRACE MODE: Network request skipped to save quota."))
            }
        }
        
        return provider.extract(file).map { result ->
            DocumentLayoutData(
                detectedGroups = result.detectedGroups,
                aiResponseJson = result.responseJson
            )
        }
    }
    override suspend fun extractClasses(
        layoutData: DocumentLayoutData,
        selectedGroup: String?,
        isFullTimetable: Boolean
    ): Result<ParsedTimetable> = withContext(Dispatchers.Default) {
        try {
            val json = layoutData.aiResponseJson ?: return@withContext Result.failure(Exception("Missing AI extraction data."))
            val aiData = gson.fromJson(json, AiBackendResponse::class.java)
            
            val filteredList = mutableListOf<ParsedTimetableClass>()
            
            for (c in (aiData.classes ?: emptyList())) {
                // Validation checks
                if (c.subject.isNullOrBlank()) continue
                if (c.day.isNullOrBlank() || c.startTime.isNullOrBlank() || c.endTime.isNullOrBlank()) continue
                
                val dayInt = mapDayString(c.day) ?: continue
                val startMins = parseTime(c.startTime) ?: continue
                val endMins = parseTime(c.endTime) ?: continue
                
                if (startMins >= endMins) continue

                // Filtering by Group
                val classGroup = if (c.group.isNullOrBlank()) "shared" else c.group.trim()
                val matchesBatch = isFullTimetable || classGroup.equals("shared", ignoreCase = true) || classGroup.equals(selectedGroup, ignoreCase = true)
                
                if (matchesBatch) {
                    if (startMins == 680) { // 11:20
                        println("DEBUG FLOW Parser: Raw = ${c.startTime}-${c.endTime}, Parsed = $startMins-$endMins, Subject = ${c.subject}, Group=${c.group}")
                    }
                    filteredList.add(
                        ParsedTimetableClass(
                            extractedText = "${c.subject} (${c.teacher})",
                            dayOfWeek = dayInt,
                            startTime = startMins,
                            endTime = endMins,
                            subjectName = c.subject,
                            room = c.room,
                            teacher = c.teacher,
                            confidence = Confidence.HIGH
                        )
                    )
                } else {
                    println("DEBUG OCR: Retained failed match for ${c.subject} - Group was $classGroup but selected was $selectedGroup")
                }
            }
            
            println("DEBUG OCR: Final parser retention: ${filteredList.size} out of ${aiData.classes?.size}")
            
            if (filteredList.isEmpty()) {
                return@withContext Result.failure(Exception("No timetable classes matched the selected criteria."))
            }
            
            Result.success(ParsedTimetable(filteredList))
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to parse extracted AI classes."))
        }
    }

    private fun mapDayString(dayRaw: String): Int? {
        val normalized = dayRaw.trim().lowercase()
        return when {
            normalized.contains("mon") -> 1
            normalized.contains("tue") -> 2
            normalized.contains("wed") -> 3
            normalized.contains("thu") -> 4
            normalized.contains("fri") -> 5
            normalized.contains("sat") -> 6
            normalized.contains("sun") -> 7
            else -> null
        }
    }

    private fun parseTime(timeRaw: String): Int? {
        return try {
            val parts = timeRaw.trim().split(":")
            if (parts.size >= 2) {
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                (h * 60) + m
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
