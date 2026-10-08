package com.adityaram.present.domain.import

import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

data class GridRegion(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun containsCenter(rect: Rect): Boolean = rect.centerX() in left..right && rect.centerY() in top..bottom
}

data class DayAnchor(val mappedDay: Int, val block: OcrTextBlock)
data class TimeAnchor(val block: OcrTextBlock, val centerX: Int)

data class TimeCandidate(
    val block: OcrTextBlock,
    val rawText: String,
    val normalizedText: String,
    val parsedStart: String?,
    val parsedEnd: String?,
    val isAccepted: Boolean,
    val reason: String,
    /** Estimated horizontal center for this specific time range within the parent block */
    val estimatedCenterX: Int = block.boundingBox?.centerX() ?: 0
)

data class DayRowBoundary(val day: Int, val top: Int, val bottom: Int)
data class TimeColumnBoundary(val index: Int, val left: Int, val right: Int)

data class CandidateCell(val day: Int, val columnIndices: List<Int>, val rect: Rect, val blocks: List<OcrTextBlock>)
data class OmittedCell(val day: Int, val columnIndices: List<Int>, val reason: String)

data class GridInferenceResult(
    val region: GridRegion?,
    val dayAnchors: List<DayAnchor>,
    val timeAnchors: List<TimeAnchor>,
    val timeCandidates: List<TimeCandidate>,
    val rows: List<DayRowBoundary>,
    val columns: List<TimeColumnBoundary>,
    val cells: List<CandidateCell>,
    val theoreticalCellCount: Int,
    val normalPositions: Int,
    val mergedPositions: Int,
    val mergedObjectsCount: Int,
    val omittedCells: List<OmittedCell>,
    val unknownBlocks: List<OcrTextBlock>
)

class TimetableGridInference {

    private val dayPatterns = mapOf(
        1 to Regex(".*(MON|MONDAY).*"),
        2 to Regex(".*(TUE|TUESDAY).*"),
        3 to Regex(".*(WED|WEDNESDAY).*"),
        4 to Regex(".*(THU|THURSDAY).*"),
        5 to Regex(".*(FRI|FRIDAY).*"),
        6 to Regex(".*(SAT|SATURDAY).*")
    )
    
    // Liberal regex for anything that contains something looking like a time HH:MM, robust to newlines
    private val timeLikePattern = Regex(".*\\d{1,2}\\s*[:.]\\s*\\d{2}.*", RegexOption.DOT_MATCHES_ALL)

    // Pattern to match a single time range like "9:00to10:00" or "11:20to12:20"
    // After whitespace normalization: digits, colon/dot, digits, "to", digits, colon/dot, digits
    private val timeRangePattern = Regex("(\\d{1,2})[:.](\\d{2})to(\\d{1,2})[:.](\\d{2})")

    fun inferGrid(blocks: List<OcrTextBlock>): GridInferenceResult {
        val dayAnchors = findDayAnchors(blocks)
        
        val topBoundY = dayAnchors.minOfOrNull { it.block.boundingBox?.top ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
        val timeCandidates = generateTimeCandidates(blocks, topBoundY)
        val timeAnchors = timeCandidates
            .filter { it.isAccepted }
            .map { TimeAnchor(it.block, it.estimatedCenterX) }
            .sortedBy { it.centerX }
        
        val region = inferRegion(dayAnchors, timeAnchors, blocks)
        
        val gridBlocks = blocks.filter { block ->
            val r = block.boundingBox ?: return@filter false
            region != null && region.containsCenter(r)
        }
        
        val rows = inferDayRows(dayAnchors, region)
        
        val columns = inferTimeColumns(timeAnchors, dayAnchors, region, gridBlocks)
        
        // Cell Generation
        val cells = generateCells(gridBlocks, rows, columns)
        
        val theoreticalCellCount = rows.size * columns.size
        
        var normalPositions = 0
        var mergedPositions = 0
        val omitted = mutableListOf<OmittedCell>()
        
        for (row in rows) {
            for (col in columns) {
                val overlapping = cells.filter { it.day == row.day && it.columnIndices.contains(col.index) }
                if (overlapping.isEmpty()) {
                    omitted.add(OmittedCell(row.day, listOf(col.index), "Empty / No OCR blocks overlapping"))
                } else if (overlapping.any { it.columnIndices.size > 1 }) {
                    mergedPositions++
                } else {
                    normalPositions++
                }
            }
        }
        
        val mergedObjectsCount = cells.count { it.columnIndices.size > 1 }

        val assignedBlocks = cells.flatMap { it.blocks }.toSet()
        val unknown = blocks.filterNot { 
            it in assignedBlocks || dayAnchors.any { d -> d.block == it } || timeCandidates.any { t -> t.block == it }
        }

        return GridInferenceResult(
            region = region,
            dayAnchors = dayAnchors,
            timeAnchors = timeAnchors,
            timeCandidates = timeCandidates,
            rows = rows,
            columns = columns,
            cells = cells,
            theoreticalCellCount = theoreticalCellCount,
            normalPositions = normalPositions,
            mergedPositions = mergedPositions,
            mergedObjectsCount = mergedObjectsCount,
            omittedCells = omitted,
            unknownBlocks = unknown
        )
    }

    private fun findDayAnchors(blocks: List<OcrTextBlock>): List<DayAnchor> {
        val anchors = mutableListOf<DayAnchor>()
        for (block in blocks) {
            val text = block.text.uppercase().replace(Regex("\\s+"), "")
            dayPatterns.forEach { (day, regex) ->
                if (regex.matches(text)) {
                    anchors.add(DayAnchor(day, block))
                }
            }
        }
        val grouped = anchors.groupBy { it.mappedDay }
        val filtered = grouped.map { (_, list) -> 
            list.minByOrNull { it.block.boundingBox?.left ?: Int.MAX_VALUE }!!
        }
        return filtered.sortedBy { it.block.boundingBox?.centerY() ?: 0 }
    }

    /**
     * Generates time candidates from OCR blocks. CRITICAL FIX: Extracts MULTIPLE time ranges
     * from a single OCR block when ML Kit merges adjacent time headers.
     */
    private fun generateTimeCandidates(blocks: List<OcrTextBlock>, topBoundY: Int): List<TimeCandidate> {
        val candidates = mutableListOf<TimeCandidate>()
        
        for (block in blocks) {
            val raw = block.text.lowercase()
            val normalized = raw.replace(Regex("\\s+"), "")
            
            val isTimeLike = timeLikePattern.matches(normalized)
            
            val cy = block.boundingBox?.centerY() ?: Int.MAX_VALUE
            val isHeaderRowPosition = cy < topBoundY + 150
            
            if (!isTimeLike) {
                // Not time-like at all. Check if it's in header row with "to" or "-"
                if (isHeaderRowPosition && (raw.contains("to") || raw.contains("-"))) {
                    candidates.add(TimeCandidate(block, raw, normalized, null, null, false,
                        "Top-row position but failed time regex."))
                }
                continue
            }
            
            // Extract ALL time ranges from this block
            val matches = timeRangePattern.findAll(normalized).toList()
            
            if (matches.isEmpty()) {
                // Has time-like digits but no complete range pattern
                val singleTimeMatch = Regex("(\\d{1,2})[:.](\\d{2})").findAll(normalized).toList()
                val start = singleTimeMatch.getOrNull(0)?.value
                val end = singleTimeMatch.getOrNull(1)?.value
                
                if (isHeaderRowPosition) {
                    candidates.add(TimeCandidate(block, raw, normalized, start, end, start != null && end != null,
                        if (start != null && end != null) "Valid format and top-row position." else "Incomplete time range in header row."))
                } else {
                    candidates.add(TimeCandidate(block, raw, normalized, start, end, false,
                        "Looks like time but Y position ($cy) is too far below day header ($topBoundY)."))
                }
                continue
            }
            
            // MULTI-RANGE EXTRACTION: Each match is an independent time range
            val blockBox = block.boundingBox
            val blockLeft = blockBox?.left ?: 0
            val blockRight = blockBox?.right ?: 0
            val blockTextWidth = normalized.length.coerceAtLeast(1)
            
            for (match in matches) {
                val startTime = "${match.groupValues[1]}:${match.groupValues[2]}"
                val endTime = "${match.groupValues[3]}:${match.groupValues[4]}"
                
                // Estimate horizontal position of this specific range within the parent block
                // using character offset proportional mapping
                val charStart = match.range.first
                val charEnd = match.range.last
                val charCenter = (charStart + charEnd) / 2.0
                val proportionalX = blockLeft + ((charCenter / blockTextWidth) * (blockRight - blockLeft)).toInt()
                val estimatedX = proportionalX.coerceIn(blockLeft, blockRight)
                
                if (isHeaderRowPosition) {
                    candidates.add(TimeCandidate(block, raw, normalized, startTime, endTime, true,
                        "Valid multi-range extraction (${matches.size} ranges in block). Top-row position.",
                        estimatedCenterX = estimatedX))
                } else {
                    candidates.add(TimeCandidate(block, raw, normalized, startTime, endTime, false,
                        "Multi-range extracted but Y position ($cy) is too far below day header ($topBoundY).",
                        estimatedCenterX = estimatedX))
                }
            }
        }
        return candidates
    }

    private fun inferRegion(dayAnchors: List<DayAnchor>, timeAnchors: List<TimeAnchor>, allBlocks: List<OcrTextBlock>): GridRegion? {
        if (dayAnchors.isEmpty() || timeAnchors.isEmpty()) return null
        
        val maxDayY = dayAnchors.maxOf { it.block.boundingBox?.bottom ?: 0 }
        val minDayX = dayAnchors.minOf { it.block.boundingBox?.left ?: Int.MAX_VALUE }
        val minTimeY = timeAnchors.minOf { it.block.boundingBox?.top ?: Int.MAX_VALUE }
        
        val maxTimeX = timeAnchors.maxOf { it.block.boundingBox?.right ?: 0 }
        
        val estTop = minTimeY - 20
        val estLeft = minDayX - 20
        
        var avgRowGap = 80
        if (dayAnchors.size > 1) {
            val yDiffs = dayAnchors.windowed(2).map { (it[1].block.boundingBox?.centerY() ?: 0) - (it[0].block.boundingBox?.centerY() ?: 0) }
            avgRowGap = max(40, yDiffs.average().toInt())
        }
        val estBottom = maxDayY + avgRowGap
        
        return GridRegion(
            left = estLeft, 
            top = estTop,
            right = maxTimeX + 80, 
            bottom = estBottom
        )
    }

    private fun inferDayRows(dayAnchors: List<DayAnchor>, region: GridRegion?): List<DayRowBoundary> {
        val rows = mutableListOf<DayRowBoundary>()
        if (dayAnchors.isEmpty()) return rows
        
        for (i in dayAnchors.indices) {
            val top = if (i == 0) {
                (dayAnchors[i].block.boundingBox?.top ?: 0) - 40
            } else {
                val prevCenter = dayAnchors[i-1].block.boundingBox?.centerY() ?: 0
                val currCenter = dayAnchors[i].block.boundingBox?.centerY() ?: 0
                (prevCenter + currCenter) / 2
            }
            
            val bottom = if (i == dayAnchors.size - 1) {
                region?.bottom ?: (dayAnchors[i].block.boundingBox?.bottom ?: 0) + 120
            } else {
                val currCenter = dayAnchors[i].block.boundingBox?.centerY() ?: 0
                val nextCenter = dayAnchors[i+1].block.boundingBox?.centerY() ?: 0
                (currCenter + nextCenter) / 2
            }
            rows.add(DayRowBoundary(dayAnchors[i].mappedDay, top, bottom))
        }
        return rows
    }

    private fun inferTimeColumns(timeAnchors: List<TimeAnchor>, dayAnchors: List<DayAnchor>, region: GridRegion?, gridBlocks: List<OcrTextBlock>): List<TimeColumnBoundary> {
        val columns = mutableListOf<TimeColumnBoundary>()
        if (timeAnchors.isEmpty()) return columns
        
        val maxDayRight = dayAnchors.maxOfOrNull { it.block.boundingBox?.right ?: 0 } ?: 0
        
        for (i in timeAnchors.indices) {
            val anchorCenterX = timeAnchors[i].centerX
            val anchorBox = timeAnchors[i].block.boundingBox
            
            val left = if (i == 0) {
                // Ensure column 0 starts after the day column
                val candidateLeft = if (anchorBox != null) anchorBox.left - 40 else anchorCenterX - 60
                max(maxDayRight + 10, candidateLeft)
            } else {
                val prevCenter = timeAnchors[i-1].centerX
                (prevCenter + anchorCenterX) / 2
            }
            
            val right = if (i == timeAnchors.size - 1) {
                region?.right ?: (anchorCenterX + 100)
            } else {
                val nextCenter = timeAnchors[i+1].centerX
                (anchorCenterX + nextCenter) / 2
            }
            
            columns.add(TimeColumnBoundary(i, left, right))
        }
        
        return columns
    }

    private fun generateCells(
        blocks: List<OcrTextBlock>, 
        rows: List<DayRowBoundary>, 
        columns: List<TimeColumnBoundary>
    ): List<CandidateCell> {
        val cells = mutableListOf<CandidateCell>()
        if (rows.isEmpty() || columns.isEmpty()) return cells
        
        val gridOccupancy = mutableMapOf<Pair<Int, List<Int>>, MutableList<OcrTextBlock>>()
        
        for (block in blocks) {
            val box = block.boundingBox ?: continue
            val cy = box.centerY()
            val row = rows.find { cy in it.top..it.bottom } ?: continue
            
            val spanningColumns = columns.filter { col ->
                val overlapLeft = max(box.left, col.left)
                val overlapRight = min(box.right, col.right)
                val overlapWidth = overlapRight - overlapLeft
                val colWidth = col.right - col.left
                (overlapWidth > 0 && (overlapWidth > colWidth * 0.35f || overlapWidth > box.width() * 0.40f))
            }
            
            if (spanningColumns.isNotEmpty()) {
                val indices = spanningColumns.map { it.index }.sorted()
                val key = Pair(row.day, indices)
                gridOccupancy.getOrPut(key) { mutableListOf() }.add(block)
            }
        }
        
        for ((key, blockList) in gridOccupancy) {
            val (day, indices) = key
            val row = rows.find { it.day == day }!!
            val cols = columns.filter { it.index in indices }
            
            val effectiveLeft = cols.minOf { it.left }
            val effectiveRight = cols.maxOf { it.right }
            val effectiveRect = Rect(effectiveLeft, row.top, effectiveRight, row.bottom)
            
            cells.add(CandidateCell(day, indices, effectiveRect, blockList))
        }
        
        return cells.sortedWith(compareBy({ it.day }, { it.columnIndices.firstOrNull() ?: 0 }))
    }
}
