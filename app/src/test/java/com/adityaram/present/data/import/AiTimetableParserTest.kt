package com.adityaram.present.data.import

import com.adityaram.present.domain.import.DocumentLayoutData
import com.adityaram.present.domain.import.ExtractionResult
import com.adityaram.present.domain.import.TimetableExtractionProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AiTimetableParserTest {

    private val mockProvider = object : TimetableExtractionProvider {
        var mockResult: Result<ExtractionResult> = Result.failure(Exception("Not initialized"))
        override suspend fun extract(file: File): Result<ExtractionResult> = mockResult
    }

    private val parser = AiTimetableParser(provider = mockProvider)

    @Test
    fun `test valid JSON parsing and batch filtering`() = runBlocking {
        val json = """
        {
          "division": "CS",
          "groups_found": ["A", "B"],
          "classes": [
            { "day": "MON", "startTime": "09:00", "endTime": "10:00", "subject": "Math", "teacher": "JD", "room": "101", "group": "shared" },
            { "day": "MON", "startTime": "10:00", "endTime": "11:00", "subject": "Physics", "teacher": "AB", "room": "102", "group": "A" },
            { "day": "MON", "startTime": "10:00", "endTime": "11:00", "subject": "Chemistry", "teacher": "CD", "room": "103", "group": "B" }
          ]
        }
        """
        
        mockProvider.mockResult = Result.success(ExtractionResult(json, listOf("A", "B")))
        
        val layout = parser.analyzeDocument(File("dummy")).getOrThrow()
        val parsedA = parser.extractClasses(layout, "A", false).getOrThrow()
        
        assertEquals(2, parsedA.classes.size)
        // Group 'shared' should be included for A
        assertTrue(parsedA.classes.any { it.subjectName == "Math" })
        assertTrue(parsedA.classes.any { it.subjectName == "Physics" })
        assertFalse(parsedA.classes.any { it.subjectName == "Chemistry" })
    }

    @Test
    fun `test merged session parsing`() = runBlocking {
        val json = """
        {
          "division": "CS",
          "groups_found": ["shared"],
          "classes": [
            { "day": "WED", "startTime": "11:20", "endTime": "13:20", "subject": "Practical", "teacher": "TS", "room": "Lab", "group": "shared" }
          ]
        }
        """
        
        val layout = DocumentLayoutData(detectedGroups = listOf("shared"), aiResponseJson = json)
        val parsed = parser.extractClasses(layout, null, true).getOrThrow()
        
        assertEquals(1, parsed.classes.size)
        val cls = parsed.classes.first()
        assertEquals("Practical", cls.subjectName)
        assertEquals(11 * 60 + 20, cls.startTime)
        assertEquals(13 * 60 + 20, cls.endTime)
    }

    @Test
    fun `test incomplete response or missing fields`() = runBlocking {
        val json = """
        {
          "division": "CS",
          "groups_found": ["shared"],
          "classes": [
            { "day": "WED", "startTime": "11:20", "subject": "Practical", "teacher": "TS", "room": "Lab", "group": "shared" }
          ]
        }
        """
        
        val layout = DocumentLayoutData(detectedGroups = listOf("shared"), aiResponseJson = json)
        val parsed = parser.extractClasses(layout, null, true)
        
        // Missing endTime should cause it to skip that class, resulting in empty parsed list, 
        // which throws Exception "No timetable classes matched"
        assertTrue(parsed.isFailure)
        assertEquals("No timetable classes matched the selected criteria.", parsed.exceptionOrNull()?.message)
    }
}
