package com.adityaram.present.domain.import

import android.graphics.Rect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TimetableSemanticEngineTest {

    private val gridEngine = TimetableGridInference()
    private val semanticEngine = TimetableSemanticEngine()

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

    @Test
    fun `semantic accounting equals theoretical positions`() {
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 100, 220, 120),
            block("10:00 to 11:00", 230, 100, 330, 120),
            block("11:00 to 11:20", 340, 100, 420, 120)
        )
        val contentBlocks = listOf(
            block("CM /TN 311", 150, 210, 200, 225),
            block("DSGT /MP 311", 250, 210, 310, 225),
            block("B", 370, 210, 400, 225)
        )
        val allBlocks = standardDayBlocks() + timeBlocks + contentBlocks
        val grid = gridEngine.inferGrid(allBlocks)
        val semantic = semanticEngine.inferSemantics(grid)

        assertEquals(grid.theoreticalCellCount, semantic.totalAccounted)
    }

    @Test
    fun `break keyword classified as non-class`() {
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 100, 220, 120)
        )
        val contentBlocks = listOf(
            block("BREAK", 150, 210, 200, 225)
        )
        val allBlocks = standardDayBlocks() + timeBlocks + contentBlocks
        val grid = gridEngine.inferGrid(allBlocks)
        val semantic = semanticEngine.inferSemantics(grid)

        val mondayCol0 = semantic.classifications.find { it.day == 1 && it.startColumn == 0 }
        assertNotNull(mondayCol0)
        assertEquals(SemanticType.NON_CLASS, mondayCol0!!.type)
    }

    @Test
    fun `merged cell does not duplicate positions`() {
        // Use Y coordinates (100 to 120) so they comfortably sit above Monday's row (top=160)
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 100, 220, 120),
            block("10:00 to 11:00", 230, 100, 330, 120),
            block("11:00 to 11:20", 340, 100, 420, 120)
        )
        // Content that spans columns 0,1,2 for Monday
        val contentBlocks = listOf(
            // Content that spans columns 0,1,2 for Monday - make wide enough to cover all 3 column regions
            block("A-ADS /PK-301 B-OS PMM-303 C-AJ /KS-306", 120, 210, 420, 225)
        )
        val allBlocks = standardDayBlocks() + timeBlocks + contentBlocks
        val grid = gridEngine.inferGrid(allBlocks)
        val semantic = semanticEngine.inferSemantics(grid)

        // Print all classifications to see where the extra one comes from
        println("Theoretical count: ${grid.theoreticalCellCount}")
        semantic.classifications.forEach { c ->
            println("Day ${c.day}, Cols ${c.startColumn}-${c.endColumn}, Type ${c.type}")
        }
        println("Total accounted: ${semantic.totalAccounted}")

        // The key invariant: total accounted must equal theoretical regardless of merging
        assertEquals("Accounting must be consistent", grid.theoreticalCellCount, semantic.totalAccounted)
        // If the block spans multiple columns, the merged count should reflect deduplicated positions
        assertTrue("Merged positions should not exceed theoretical", semantic.totalAccounted <= grid.theoreticalCellCount)
    }

    @Test
    fun `empty cells are correctly identified`() {
        val timeBlocks = listOf(
            block("9:00 to 10:00", 120, 100, 220, 120),
            block("10:00 to 11:00", 230, 100, 330, 120)
        )
        // Only put content in Monday Col 0
        val contentBlocks = listOf(
            block("CM /TN 311", 150, 210, 200, 225)
        )
        val allBlocks = standardDayBlocks() + timeBlocks + contentBlocks
        val grid = gridEngine.inferGrid(allBlocks)
        val semantic = semanticEngine.inferSemantics(grid)

        // Should have some EMPTY classifications
        assertTrue(semantic.emptyCount > 0)
        assertEquals(10, semantic.totalAccounted) // 5 rows * 2 cols
    }

    @Test
    fun `lunch classified as non-class`() {
        val timeBlocks = listOf(
            block("1:20 to 2:00", 120, 100, 220, 120)
        )
        val contentBlocks = listOf(
            block("LUNCH", 150, 210, 200, 225)
        )
        val allBlocks = standardDayBlocks() + timeBlocks + contentBlocks
        val grid = gridEngine.inferGrid(allBlocks)
        val semantic = semanticEngine.inferSemantics(grid)

        val mondayCol0 = semantic.classifications.find { it.day == 1 && it.startColumn == 0 }
        assertNotNull(mondayCol0)
        assertEquals(SemanticType.NON_CLASS, mondayCol0!!.type)
    }
}
