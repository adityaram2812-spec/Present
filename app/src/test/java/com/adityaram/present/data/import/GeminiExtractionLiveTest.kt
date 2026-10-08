package com.adityaram.present.data.import

import com.adityaram.present.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GeminiExtractionLiveTest {

    @Test
    fun `execute live real image extraction`() = runBlocking {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            println("SKIPPED: No API Key provided.")
            return@runBlocking
        }

        val provider = GeminiExtractionProvider()
        val imageFile = File("C:\\Users\\Aditya YR\\.gemini\\antigravity\\brain\\5cedaca3-4b75-4fc8-99bf-ae181905758c\\media__1790400354377.jpg")
        
        if (!imageFile.exists()) {
            println("SKIPPED: Reference image not found at ${imageFile.absolutePath}")
            return@runBlocking
        }

        println("Executing live Gemini 3.1 Flash-Lite extraction...")
        val result = provider.extract(imageFile)
        
        if (result.isSuccess) {
            val extraction = result.getOrThrow()
            println("=== SUCCESS ===")
            println(extraction.tokenUsageReport)
            println("=== JSON EXTRACTED ===")
            println(extraction.responseJson)
            println("=== DETECTED GROUPS ===")
            println(extraction.detectedGroups)
        } else {
            println("=== FAILURE ===")
            println("Error: ${result.exceptionOrNull()?.message}")
        }
    }
}
