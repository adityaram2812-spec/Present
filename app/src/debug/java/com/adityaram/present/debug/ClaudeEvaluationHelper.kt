package com.adityaram.present.debug

import android.content.Context
import com.adityaram.present.data.import.AiTimetableParser
import com.adityaram.present.data.import.ClaudeExtractionProvider
import com.adityaram.present.ui.importing.ImportViewModel
import java.io.File

/**
 * DEBUG-ONLY helper to inject Claude payloads without modifying the actual UI.
 * This ensures the normal Gemini flow remains identical while allowing Antigravity/Devs
 * to evaluate the Opus JSON output seamlessly.
 */
object ClaudeEvaluationHelper {
    
    /**
     * Injects a predefined Claude Opus JSON payload into the existing pipeline.
     */
    fun injectClaudeJson(context: Context, viewModel: ImportViewModel, jsonPayload: String) {
        val provider = ClaudeExtractionProvider(jsonPayload)
        val testParser = AiTimetableParser(provider)
        
        // A dummy file is passed to satisfy the TimetableExtractionProvider signature,
        // although ClaudeExtractionProvider completely ignores this file and returns the payload.
        val dummyFile = File(context.cacheDir, "claude_dummy.txt")
        if (!dummyFile.exists()) {
            dummyFile.writeText("dummy")
        }
        
        // Expose the hook via the test-only public method on ImportViewModel
        viewModel.evaluateParserForDevelopment(testParser, dummyFile)
    }
}
