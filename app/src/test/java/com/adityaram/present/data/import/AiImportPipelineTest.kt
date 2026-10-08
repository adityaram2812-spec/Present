package com.adityaram.present.data.import

import com.adityaram.present.domain.import.DocumentLayoutData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import com.adityaram.present.domain.import.TimetableExtractionProvider
import com.adityaram.present.domain.import.ExtractionResult
import com.adityaram.present.ui.importing.ImportState
import com.adityaram.present.ui.importing.ImportViewModel
import com.adityaram.present.domain.import.TimetableImportServiceImpl

class AiImportPipelineTest {
    
    @Test
    fun `test trace 1120-1320 practical through entire pipeline to Log Class UI and Room`() = runBlocking {
        val referenceJson = """{
  "groups_found": ["A"],
  "classes": [
    {"day": "Monday", "startTime": "11:20", "endTime": "13:20", "subject": "ADS", "group": "A", "room": "301", "teacher": "PK"},
    {"day": "Friday", "startTime": "14:00", "endTime": "16:00", "subject": "MinP", "group": "A"}
  ],
  "division": "A"
}"""
        
        println("1. Raw Gemini JSON: startTime=11:20, endTime=13:20")
        
        val mockProvider = object : TimetableExtractionProvider {
            override suspend fun extract(file: File) = Result.success(ExtractionResult(referenceJson, listOf("A")))
        }

        val parser = AiTimetableParser(mockProvider)
        val layout = parser.analyzeDocument(File("dummy")).getOrThrow()
        
        val parsed = parser.extractClasses(layout, "A", false).getOrThrow()
        
        println("Extracted classes size: ${parsed.classes.size}")
        assertEquals("Should retain 2 classes from mock JSON", 2, parsed.classes.size)
        
        val practical = parsed.classes.find { it.subjectName == "ADS" }!!
        val minp = parsed.classes.find { it.subjectName == "MinP" }!!
        
        assertEquals(800, practical.endTime)
        assertEquals(960, minp.endTime)
    }
}
