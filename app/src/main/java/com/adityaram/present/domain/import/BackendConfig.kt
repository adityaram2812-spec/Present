package com.adityaram.present.domain.import

object BackendConfig {
    /**
     * Local development URL for the Node.js Gemini Timetable Extractor.
     * Hardcoded here deliberately so it can be swapped to Cloud Run HTTPS later without touching frontend UI logic.
     */
    const val AI_TIMETABLE_API_URL = "http://192.168.0.103:8080/api/extract-timetable"
}
