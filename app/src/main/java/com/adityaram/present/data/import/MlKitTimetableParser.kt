package com.adityaram.present.data.import

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.adityaram.present.domain.import.Confidence
import com.adityaram.present.domain.import.DayNormalizer
import com.adityaram.present.domain.import.DocumentLayoutData
import com.adityaram.present.domain.import.ExtractedTextLine
import com.adityaram.present.domain.import.TimeNormalizer
import com.adityaram.present.domain.import.TimetableParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MlKitTimetableParser(
    private val context: Context
) : TimetableParser {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyzeDocument(file: File): Result<DocumentLayoutData> = withContext(Dispatchers.IO) {
        try {
            val isPdf = file.extension.equals("pdf", ignoreCase = true)
            val inputs = if (isPdf) extractPdfPages(file) else listOf(InputImage.fromFilePath(context, Uri.fromFile(file)))
            
            val lines = mutableListOf<ExtractedTextLine>()
            val foundGroups = mutableSetOf<String>()
            val groupRegex = Regex("""(Div(?:ision)?[- ]?[A-Z0-9]+|Batch[- ]?[A-Z0-9]+|Group[- ]?[A-Z0-9]+)""", RegexOption.IGNORE_CASE)
            
            for (i in inputs.indices) {
                val input = inputs[i]
                val visionText = recognizer.process(input).await()
                
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        lines.add(ExtractedTextLine(line.text, line.boundingBox, 1.0f, i))
                        val groupMatch = groupRegex.find(line.text)
                        if (groupMatch != null) {
                            foundGroups.add(groupMatch.value)
                        } else {
                            // Also check for prefix formats commonly found inside cells like 'A-' 'B-'
                            val prefixMatch = Regex("""^([A-F])\s*-\s*[A-Z]""").find(line.text)
                            if (prefixMatch != null) {
                                foundGroups.add("Batch ${prefixMatch.groupValues[1]}")
                            }
                        }
                    }
                }
            }
            Result.success(DocumentLayoutData(lines, foundGroups.toList()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun extractClasses(layoutData: DocumentLayoutData, selectedGroup: String?, isFullTimetable: Boolean): Result<com.adityaram.present.domain.import.ParsedTimetable> = withContext(Dispatchers.Default) {
        try {
            val allClasses = mutableListOf<com.adityaram.present.domain.import.ParsedTimetableClass>()
            val pages = layoutData.lines.groupBy { it.page }
            
            for ((pageIdx, pageLines) in pages) {
                val rawTimeNodes = pageLines.mapNotNull { line ->
                    val pair = TimeNormalizer.extractTimePairs(line.text)
                    if (pair != null) Pair(line.boundingBox, pair) else null
                }
                val rawDayNodes = pageLines.mapNotNull { line ->
                    val day = DayNormalizer.parseDay(line.text)
                    if (day != null) Pair(line.boundingBox, day) else null
                }

                if (rawTimeNodes.size < 2 || rawDayNodes.size < 2) continue

                // 1. Dynamic Orientation Checking
                val dayMaxXDiff = (rawDayNodes.maxOfOrNull { it.first?.left ?: 0 } ?: 0) - (rawDayNodes.minOfOrNull { it.first?.left ?: 0 } ?: 0)
                val dayMaxYDiff = (rawDayNodes.maxOfOrNull { it.first?.top ?: 0 } ?: 0) - (rawDayNodes.minOfOrNull { it.first?.top ?: 0 } ?: 0)
                val daysAreRows = dayMaxYDiff > dayMaxXDiff

                // 2. Outlier Rejection via Medians
                val timeAxis: List<Pair<android.graphics.Rect?, Pair<Int, Int>>>
                val dayAxis: List<Pair<android.graphics.Rect?, java.time.DayOfWeek>>

                if (daysAreRows) {
                    // Days align on X (left), Times align on Y (top)
                    val medianDayLeft = rawDayNodes.map { it.first?.left ?: 0 }.sorted().let { if (it.isEmpty()) 0 else it[it.size / 2] }
                    val medianTimeTop = rawTimeNodes.map { it.first?.top ?: 0 }.sorted().let { if (it.isEmpty()) 0 else it[it.size / 2] }
                    
                    dayAxis = rawDayNodes.filter { Math.abs((it.first?.left ?: 0) - medianDayLeft) < 150 }.sortedBy { it.first?.top ?: 0 }
                    timeAxis = rawTimeNodes.filter { Math.abs((it.first?.top ?: 0) - medianTimeTop) < 150 }.sortedBy { it.first?.left ?: 0 }
                } else {
                    // Days align on Y (top), Times align on X (left)
                    val medianDayTop = rawDayNodes.map { it.first?.top ?: 0 }.sorted().let { if (it.isEmpty()) 0 else it[it.size / 2] }
                    val medianTimeLeft = rawTimeNodes.map { it.first?.left ?: 0 }.sorted().let { if (it.isEmpty()) 0 else it[it.size / 2] }
                    
                    dayAxis = rawDayNodes.filter { Math.abs((it.first?.top ?: 0) - medianDayTop) < 150 }.sortedBy { it.first?.left ?: 0 }
                    timeAxis = rawTimeNodes.filter { Math.abs((it.first?.left ?: 0) - medianTimeLeft) < 150 }.sortedBy { it.first?.top ?: 0 }
                }

                if (timeAxis.isEmpty() || dayAxis.isEmpty()) continue
                
                // --- NEW MIDPOINT GEOMETRY CALCULATIONS ---
                val rCount = dayAxis.size
                val cCount = timeAxis.size
                
                val yBounds = IntArray(rCount + 1)
                val xBounds = IntArray(cCount + 1)
                
                val pageMaxX = pageLines.maxOfOrNull { it.boundingBox?.right ?: 0 } ?: 2000
                val pageMaxY = pageLines.maxOfOrNull { it.boundingBox?.bottom ?: 0 } ?: 2000
                
                if (daysAreRows) {
                    val baseTimeY = timeAxis.map { it.first?.bottom ?: 0 }.average().toInt()
                    yBounds[0] = if (baseTimeY > 0) baseTimeY else (dayAxis[0].first?.top ?: 0) - 20
                    for (i in 1 until rCount) {
                        yBounds[i] = ((dayAxis[i-1].first?.centerY() ?: 0) + (dayAxis[i].first?.centerY() ?: 0)) / 2
                    }
                    val lastDayHeight = (dayAxis[rCount-1].first?.height() ?: 40)
                    yBounds[rCount] = (dayAxis[rCount-1].first?.bottom ?: 0) + (lastDayHeight * 2)

                    val baseDayX = dayAxis.map { it.first?.right ?: 0 }.average().toInt()
                    xBounds[0] = if (baseDayX > 0) baseDayX else (timeAxis[0].first?.left ?: 0) - 20
                    for (j in 1 until cCount) {
                        xBounds[j] = ((timeAxis[j-1].first?.centerX() ?: 0) + (timeAxis[j].first?.centerX() ?: 0)) / 2
                    }
                    xBounds[cCount] = pageMaxX
                } else {
                    val baseDayY = dayAxis.map { it.first?.bottom ?: 0 }.average().toInt()
                    yBounds[0] = if (baseDayY > 0) baseDayY else (timeAxis[0].first?.top ?: 0) - 20
                    for (i in 1 until cCount) { // Since timeAxis serves as Y
                        yBounds[i] = ((timeAxis[i-1].first?.centerY() ?: 0) + (timeAxis[i].first?.centerY() ?: 0)) / 2
                    }
                    val lastTimeHeight = (timeAxis[cCount-1].first?.height() ?: 40)
                    yBounds[cCount] = (timeAxis[cCount-1].first?.bottom ?: 0) + (lastTimeHeight * 2)

                    val baseTimeX = timeAxis.map { it.first?.right ?: 0 }.average().toInt()
                    xBounds[0] = if (baseTimeX > 0) baseTimeX else (dayAxis[0].first?.left ?: 0) - 20
                    for (j in 1 until rCount) { // Since dayAxis serves as X
                        xBounds[j] = ((dayAxis[j-1].first?.centerX() ?: 0) + (dayAxis[j].first?.centerX() ?: 0)) / 2
                    }
                    xBounds[rCount] = pageMaxX
                }
                
                android.util.Log.d("TIMETABLE_DEBUG", "Page $pageIdx: DaysAreRows = $daysAreRows, Grid: ${rCount}x${cCount}")

                val breakRegex = Regex("""(?i)\b(break|lunch|recess)\b""")

                // 3. Grid Iteration
                for (i in 0 until rCount) {
                    val dayNode = dayAxis[i]
                    val cellStartY = if (daysAreRows) yBounds[i] else xBounds[i]
                    val cellEndY = if (daysAreRows) yBounds[i+1] else xBounds[i+1]
                    
                    for (j in 0 until cCount) {
                        val timeNode = timeAxis[j]
                        val cellStartX = if (daysAreRows) xBounds[j] else yBounds[j]
                        val cellEndX = if (daysAreRows) xBounds[j+1] else yBounds[j+1]
                        
                        val cellLines = pageLines.filter { line ->
                            val cX = line.boundingBox?.centerX() ?: -1
                            val cY = line.boundingBox?.centerY() ?: -1
                            cX >= cellStartX && cX <= cellEndX && cY >= cellStartY && cY <= cellEndY
                        }

                        if (cellLines.isNotEmpty()) {
                            val rawText = cellLines.joinToString("\n") { it.text }
                            val alphanumericOnly = rawText.replace(Regex("""[^a-zA-Z0-9]"""), "")

                            // Ignore vertical column text (LUNCH, BREAK) or extremely short noise
                            if (alphanumericOnly.length <= 2 || breakRegex.containsMatchIn(rawText)) {
                                continue
                            }

                            // Calculate merged cell spanning
                            val maxLineX = cellLines.maxOf { it.boundingBox?.right ?: 0 }
                            val maxLineY = cellLines.maxOf { it.boundingBox?.bottom ?: 0 }
                            
                            var spannedEndTime = timeNode.second.second
                            var spanIndex = j
                            if (daysAreRows) {
                                while (spanIndex + 1 < timeAxis.size && maxLineX > xBounds[spanIndex + 1]) {
                                    spanIndex++
                                    spannedEndTime = timeAxis[spanIndex].second.second
                                }
                            } else {
                                while (spanIndex + 1 < timeAxis.size && maxLineY > yBounds[spanIndex + 1]) {
                                    spanIndex++
                                    spannedEndTime = timeAxis[spanIndex].second.second
                                }
                            }

                            // Context Group Filtering
                            var validLines = cellLines.map { it.text }
                            var isMixedCell = false
                            if (!isFullTimetable && selectedGroup != null) {
                                // Find e.g., 'A' from 'Batch A' or 'Div-A'
                                val selectedPrefixMatch = Regex("""([A-Z0-9]+)""").findAll(selectedGroup).lastOrNull()?.value
                                if (selectedPrefixMatch != null) {
                                    // A cell is "Mixed" if ANY line starts with something like "A-", "B-", "C-"
                                    val hasBatchSpecifics = validLines.any { Regex("""^([A-Z0-9]+)\s*[-/]""").containsMatchIn(it) }
                                    if (hasBatchSpecifics) {
                                        isMixedCell = true
                                        validLines = validLines.filter { line ->
                                            val linePrefixMatch = Regex("""^([A-Z0-9]+)\s*[-/]""").find(line)
                                            if (linePrefixMatch != null) {
                                                linePrefixMatch.groupValues[1].equals(selectedPrefixMatch, ignoreCase = true)
                                            } else {
                                                true // Keep lines without prefix (e.g. trailing info)
                                            }
                                        }
                                    }
                                }
                            }

                            if (validLines.isNotEmpty()) {
                                // Semantic Tokenization
                                val joinedText = validLines.joinToString(" ")
                                val normalizedLineText = if (isMixedCell) {
                                    joinedText.replace(Regex("""^([A-Z0-9]+)\s*[-/]"""), "")
                                } else {
                                    joinedText
                                }
                                
                                val tokens = normalizedLineText.split(Regex("""[\s/:-]+""")).map { it.trim() }.filter { it.isNotEmpty() }
                                
                                val subject = tokens.firstOrNull()?.takeIf { it.isNotEmpty() }
                                val room = if (tokens.size >= 3) tokens.last() else null
                                val teacher = if (tokens.size >= 3) {
                                    tokens.drop(1).dropLast(1).joinToString(" ").takeIf { it.isNotEmpty() }
                                } else if (tokens.size == 2) {
                                    tokens[1].takeIf { it.isNotEmpty() }
                                } else null
                                
                                val conf = if (subject != null) com.adityaram.present.domain.import.Confidence.HIGH else com.adityaram.present.domain.import.Confidence.LOW
                                val reason = if (subject == null) "Subject couldn't be definitively isolated" else null

                                allClasses.add(
                                    com.adityaram.present.domain.import.ParsedTimetableClass(
                                        extractedText = rawText,
                                        dayOfWeek = dayNode.second.value,
                                        startTime = timeNode.second.first,
                                        endTime = spannedEndTime,
                                        subjectName = subject ?: "Unknown",
                                        room = room,
                                        teacher = teacher,
                                        confidence = conf,
                                        uncertaintyReason = reason
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Result.success(com.adityaram.present.domain.import.ParsedTimetable(allClasses))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun extractPdfPages(file: File): List<InputImage> = withContext(Dispatchers.IO) {
        val images = mutableListOf<InputImage>()
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            // Scale dynamically (minimum resolution for OCR readability)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            images.add(InputImage.fromBitmap(bitmap, 0))
            page.close()
        }
        renderer.close()
        descriptor.close()
        images
    }
}
