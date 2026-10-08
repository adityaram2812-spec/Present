package com.adityaram.present.domain.import

import java.io.File

/**
 * Defines the contract for an AI provider to extract timetable data from a document.
 * This is the pure network/extraction boundary, decoupled from application parsing/validation.
 */
interface TimetableExtractionProvider {
    /** 
     * Extract timetable data from a document file.
     * Returns the raw JSON response and any detected groups/batches.
     */
    suspend fun extract(file: File): Result<ExtractionResult>
}

data class ExtractionResult(
    val responseJson: String,
    val detectedGroups: List<String>,
    val tokenUsageReport: String = ""
)
