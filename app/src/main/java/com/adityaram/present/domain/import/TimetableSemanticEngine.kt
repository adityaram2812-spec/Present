package com.adityaram.present.domain.import

enum class SemanticType {
    CLASS, MERGED_CLASS, EMPTY, NON_CLASS, UNKNOWN
}

enum class SemanticConfidence { HIGH, MEDIUM, LOW }

data class SemanticClassification(
    val day: Int,
    val startColumn: Int,   
    val endColumn: Int,     
    val timeRange: String,
    val type: SemanticType,
    val confidence: SemanticConfidence,
    val reason: String,
    val blockData: List<OcrTextBlock>,
    val extractedSubject: String? = null
) {
    val physicalPositionsCovered: Int
        get() = endColumn - startColumn + 1
}

data class SemanticInferenceResult(
    val grid: GridInferenceResult,
    val classifications: List<SemanticClassification>,
    val classCount: Int,
    val mergedClassCount: Int,
    val emptyCount: Int,
    val nonClassCount: Int,
    val unknownCount: Int
) {
    val totalAccounted: Int
        get() = classCount + mergedClassCount + emptyCount + nonClassCount + unknownCount
}

class TimetableSemanticEngine {

    fun inferSemantics(grid: GridInferenceResult): SemanticInferenceResult {
        val mappings = mutableListOf<SemanticClassification>()
        
        // Build column -> time string lookup
        val colTimes = grid.columns.associate { col ->
            val overlappingCandidate = grid.timeCandidates.find {
                it.isAccepted && it.estimatedCenterX in (col.left - 30)..(col.right + 30)
            }
            val timeString = if (overlappingCandidate != null) {
                "${overlappingCandidate.parsedStart ?: "?"}-${overlappingCandidate.parsedEnd ?: "?"}"
            } else {
                "Col ${col.index}"
            }
            col.index to timeString
        }

        // Track which physical (day, colIndex) coordinates have already been classified
        val coveredPositions = mutableSetOf<Pair<Int, Int>>()

        for (row in grid.rows) {
            for (col in grid.columns) {
                val posKey = Pair(row.day, col.index)
                if (posKey in coveredPositions) continue

                val overlapping = grid.cells.filter { it.day == row.day && it.columnIndices.contains(col.index) }
                
                if (overlapping.isEmpty()) {
                    val time = colTimes[col.index] ?: "Unknown"
                    mappings.add(SemanticClassification(
                        day = row.day,
                        startColumn = col.index,
                        endColumn = col.index,
                        timeRange = time,
                        type = SemanticType.EMPTY,
                        confidence = SemanticConfidence.HIGH,
                        reason = "No meaningful OCR content",
                        blockData = emptyList(),
                        extractedSubject = null
                    ))
                    coveredPositions.add(posKey)
                } else {
                    val targetCell = overlapping.first()
                    val cols = targetCell.columnIndices
                    val isMerged = cols.size > 1
                    
                    val timeStart = colTimes[cols.first()]?.split("-")?.firstOrNull() ?: "Start"
                    val timeEnd = colTimes[cols.last()]?.split("-")?.lastOrNull() ?: "End"
                    val combinedTime = if (isMerged) "$timeStart - $timeEnd" else (colTimes[cols.first()] ?: "Unknown")
                    
                    val allText = targetCell.blocks.joinToString(" ") { it.text }.lowercase()
                    
                    var type = SemanticType.UNKNOWN
                    var conf = SemanticConfidence.LOW
                    var subject: String? = null
                    var reason = ""
                    
                    if (allText.contains("break") || allText.contains("lunch") || allText.replace(" ", "").contains("break")) {
                        type = SemanticType.NON_CLASS
                        conf = SemanticConfidence.HIGH
                        reason = "Explicit NON_CLASS keyword match (break/lunch)"
                    } else if (allText.isBlank() || (allText.length < 3 && !allText.any { it.isDigit() })) {
                        type = SemanticType.UNKNOWN
                        conf = SemanticConfidence.MEDIUM
                        reason = "Content too short to confidently identify as class"
                    } else {
                        // Check for single-letter codes that are likely not classes on their own
                        val trimmed = allText.trim()
                        val looksLikeClassContent = trimmed.length >= 2 && trimmed.any { it.isLetter() }
                        
                        if (looksLikeClassContent) {
                            type = if (isMerged) SemanticType.MERGED_CLASS else SemanticType.CLASS
                            conf = SemanticConfidence.MEDIUM
                            reason = "Contains sufficient text/numbers for scheduled subject"
                            
                            val candidateSubjects = targetCell.blocks.filter { b -> 
                                val t = b.text.uppercase()
                                t.length > 2 && t.any { c -> c.isLetter() }
                            }
                            subject = candidateSubjects.maxByOrNull { it.text.length }?.text?.uppercase()
                        } else {
                            type = SemanticType.UNKNOWN
                            conf = SemanticConfidence.LOW
                            reason = "Insufficient evidence to classify as class (isolated number/symbol)"
                        }
                    }
                    
                    mappings.add(SemanticClassification(
                        day = targetCell.day,
                        startColumn = cols.first(),
                        endColumn = cols.last(),
                        timeRange = combinedTime,
                        type = type,
                        confidence = conf,
                        reason = reason,
                        blockData = targetCell.blocks,
                        extractedSubject = subject
                    ))
                    // Mark ALL physical positions covered by this cell
                    for (ci in cols) {
                        coveredPositions.add(Pair(row.day, ci))
                    }
                }
            }
        }
        
        // Count using PHYSICAL POSITIONS, not classification objects
        // Each classification covers physicalPositionsCovered positions
        var classCount = 0
        var mergedCount = 0
        var emptyCount = 0
        var nonClassCount = 0
        var unknownCount = 0
        
        for (m in mappings) {
            val positions = m.physicalPositionsCovered
            when (m.type) {
                SemanticType.CLASS -> classCount += positions
                SemanticType.MERGED_CLASS -> mergedCount += positions
                SemanticType.EMPTY -> emptyCount += positions
                SemanticType.NON_CLASS -> nonClassCount += positions
                SemanticType.UNKNOWN -> unknownCount += positions
            }
        }
        
        return SemanticInferenceResult(
            grid,
            mappings.sortedWith(compareBy({ it.day }, { it.startColumn })),
            classCount, mergedCount, emptyCount, nonClassCount, unknownCount
        )
    }
}
