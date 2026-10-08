package com.adityaram.present.data.import

import com.adityaram.present.domain.import.ExtractionResult
import com.adityaram.present.domain.import.TimetableExtractionProvider
import com.google.gson.Gson
import java.io.File

/**
 * DEBUG-ONLY Provider for Claude/Opus Evaluation.
 * This class only exists in the `debug` source set and is entirely excluded from release APKs.
 * It bypasses network calls and allows injecting a predefined Claude JSON result directly.
 */
class ClaudeExtractionProvider(
    private val predefinedJson: String
) : TimetableExtractionProvider {
    
    private val gson = Gson()

    override suspend fun extract(file: File): Result<ExtractionResult> {
        return try {
            val aiData = gson.fromJson(predefinedJson, AiBackendResponse::class.java)
            
            if (aiData.groupsFound == null || aiData.classes == null) {
                Result.failure(Exception("Invalid Claude JSON. Vital fields missing."))
            } else if (aiData.classes.isEmpty()) {
                Result.failure(Exception("No timetable classes from Claude JSON."))
            } else {
                Result.success(ExtractionResult(
                    responseJson = predefinedJson,
                    detectedGroups = aiData.groupsFound
                ))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Invalid Claude JSON format."))
        }
    }
}
