package com.adityaram.present.domain.import

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimetableGridInferenceTest {

    private val engine = TimetableGridInference()

    // ==================== HELPER ====================

    private fun block(text: String, left: Int, top: Int, right: Int, bottom: Int): OcrTextBlock {
        return OcrTextBlock(text, Rect(left, top, right, bottom), null, 0)
    }

    private fun standardDayBlocks(): List<OcrTextBlock> {
        return listOf(
            block("MONDAY", 10, 200, 100, 230),
            block("TUESDAY", 10, 280, 100, 310),
            block("WEDNESDAY", 10, 360, 100, 390),
            block("THURSDAY", 10, 440, 100, 470),
            block("FRIDAY", 10, 520, 100, 550)
        )
    }

    // ==================== TIME EXTRACTION ====================

    @Test
    fun `single time range extraction`() {
        val blocks = standardDayBlocks() + listOf(
            block("9:00 to 10:00", 120, 160, 220, 180)
        )
        val result = engine.inferGrid(blocks)
        val accepted = result.timeCandidates.filter { it.isAccepted }
        assertEquals(1, accepted.size)
        assertEquals("9:00", accepted[0].parsedStart)
        assertEquals("10:00", accepted[0].parsedEnd)
    }

    @Test
    fun `multiple time ranges in one OCR block`() {
        val blocks = standardDayBlocks() + listOf(
            block("10:00 to 11:00 11:00 to 11:20 11:20 to 12:20", 200, 160, 600, 180)
        )
        val result = engine.inferGrid(blocks)
        val accepted = result.timeCandidates.filter { it.isAccepted }
        assertEquals(3, accepted.size)
        assertEquals("10:00", accepted[0].parsedStart)
        assertEquals("11:00", accepted[0].parsedEnd)
        assertEquals("11:00", accepted[1].parsedStart)
        assertEquals("11:20", accepted[1].parsedEnd)
        assertEquals("11:20", accepted[2].parsedStart)
        assertEquals("12:20", accepted[2].parsedEnd)
    }

    @Test
    fun `12 to 1 transition extraction`() {
        val blocks = standardDayBlocks() + listOf(
            block("12:20 to 1:20", 500, 160, 600, 180)
        )
        val result = engine.inferGrid(blocks)
        val accepted = result.timeCandidates.filter { it.isAccepted }
        assertEquals(1, accepted.size)
        assertEquals("12:20", accepted[0].parsedStart)
        assertEquals("1:20", accepted[0].parsedEnd)
    }

    @Test
    fun `newline within time header block`() {
        val blocks = standardDayBlocks() + listOf(
            block("12:20\nto1:20", 500, 160, 600, 180)
        )
        val result = engine.inferGrid(blocks)
        val accepted = result.timeCandidates.filter { it.isAccepted }
        assertEquals(1, accepted.size)
        assertEquals("12:20", accepted[0].parsedStart)
        assertEquals("1:20", accepted[0].parsedEnd)
    }

    @Test
    fun `time block below day headers is rejected`() {
        val blocks = standardDayBlocks() + listOf(
            block("9:00 to 10:00", 120, 600, 220, 620) // Way below day headers
        )
        val result = engine.inferGrid(blocks)
        val accepted = result.timeCandidates.filter { it.isAccepted }
        assertEquals(0, accepted.size)
    }

    // ==================== COLUMN INFERENCE ====================

    @Test
    fun `nine columns from nine separate header blocks`() {
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 160, 220, 180),
            block("10:00 to 11:00", 230, 160, 330, 180),
            block("11:00 to 11:20", 340, 160, 420, 180),
            block("11:20 to 12:20", 430, 160, 530, 180),
            block("12:20 to 1:20", 540, 160, 620, 180),
            block("1:20 to 2:00", 630, 160, 710, 180),
            block("2:00 to 3:00", 720, 160, 800, 180),
            block("3:00 to 4:00", 810, 160, 890, 180),
            block("4:00 to 5:00", 900, 160, 980, 180)
        )
        val blocks = standardDayBlocks() + timeBlocks
        val result = engine.inferGrid(blocks)
        assertEquals(9, result.columns.size)
    }

    @Test
    fun `nine columns from merged header blocks`() {
        // Simulates ML Kit merging three adjacent time headers into one block
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 160, 220, 180),
            block("10:00 to 11:00 11:00 to 11:20 11:20 to 12:20", 230, 160, 530, 180),
            block("12:20 to 1:20", 540, 160, 620, 180),
            block("1:20 to 2:00", 630, 160, 710, 180),
            block("2:00 to 3:00", 720, 160, 800, 180),
            block("3:00 to 4:00", 810, 160, 890, 180),
            block("4:00 to 5:00", 900, 160, 980, 180)
        )
        val blocks = standardDayBlocks() + timeBlocks
        val result = engine.inferGrid(blocks)
        assertEquals(9, result.columns.size)
    }

    // ==================== DAY ROW INFERENCE ====================

    @Test
    fun `five day rows detected`() {
        val blocks = standardDayBlocks() + listOf(
            block("9:00 to 10:00", 120, 160, 220, 180)
        )
        val result = engine.inferGrid(blocks)
        assertEquals(5, result.rows.size)
        assertEquals(listOf(1, 2, 3, 4, 5), result.rows.map { it.day })
    }

    // ==================== GRID POSITION ACCOUNTING ====================

    @Test
    fun `theoretical count equals rows times columns`() {
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 160, 220, 180),
            block("10:00 to 11:00", 230, 160, 330, 180)
        )
        val blocks = standardDayBlocks() + timeBlocks
        val result = engine.inferGrid(blocks)
        assertEquals(5 * 2, result.theoreticalCellCount)
    }

    @Test
    fun `physical accounting is consistent`() {
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 160, 220, 180),
            block("10:00 to 11:00", 230, 160, 330, 180)
        )
        val blocks = standardDayBlocks() + timeBlocks
        val result = engine.inferGrid(blocks)
        val accounted = result.normalPositions + result.mergedPositions + result.omittedCells.size
        assertEquals(result.theoreticalCellCount, accounted)
    }
}
