package com.adityaram.present.debug

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.adityaram.present.domain.import.GridInferenceResult
import com.adityaram.present.domain.import.LocalTimetableExtractionProvider
import com.adityaram.present.domain.import.OcrTextBlock
import com.adityaram.present.domain.import.SemanticInferenceResult
import com.adityaram.present.domain.import.SemanticType
import com.adityaram.present.domain.import.TimetableGridInference
import com.adityaram.present.domain.import.TimetableSemanticEngine
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class DebugOcrActivity : ComponentActivity() {

    private lateinit var provider: LocalTimetableExtractionProvider
    private val gridEngine = TimetableGridInference()
    private val semanticEngine = TimetableSemanticEngine()

    private val selectDocument = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val isPdf = contentResolver.getType(uri)?.contains("pdf") == true
                val ext = if (isPdf) "pdf" else "jpg"
                val file = File(cacheDir, "debug_import_temp.$ext")
                
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                
                processFile(file, isPdf)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var debugState by mutableStateOf<DebugState?>(null)
    private var isProcessing by mutableStateOf(false)

    data class DebugState(
        val bitmap: Bitmap?,
        val blocks: List<OcrTextBlock>,
        val grid: GridInferenceResult?,
        val semantic: SemanticInferenceResult?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider = LocalTimetableExtractionProvider(this)

        setContent {
            MaterialTheme {
                Scaffold { padding ->
                    Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(onClick = { selectDocument.launch("*/*") }, enabled = !isProcessing) {
                                Text("Select Document")
                            }
                            if (isProcessing) {
                                Text("Processing ML Kit + Grid Inference...", modifier = Modifier.padding(top = 10.dp))
                            }
                        }
                        
                        val state = debugState
                        if (state != null) {
                            val blocksCount = state.blocks.size
                            val theGrid = state.grid
                            
                            Text(
                                "OCR Blocks: $blocksCount | Cols: ${theGrid?.columns?.size} | Positions: ${theGrid?.theoreticalCellCount}",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            if (state.bitmap != null) {
                                Box(modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .border(1.dp, Color.Gray)) {
                                    
                                    Image(
                                        bitmap = state.bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val scaleX = size.width / state.bitmap.width
                                        val scaleY = size.height / state.bitmap.height
                                        val scale = minOf(scaleX, scaleY)
                                        
                                        val imageDrawWidth = state.bitmap.width * scale
                                        val imageDrawHeight = state.bitmap.height * scale
                                        val startX = (size.width - imageDrawWidth) / 2
                                        val startY = (size.height - imageDrawHeight) / 2

                                        for (block in state.blocks) {
                                            val rect = block.boundingBox ?: continue
                                            drawRect(
                                                color = Color.Gray.copy(alpha = 0.5f),
                                                topLeft = Offset(startX + rect.left * scale, startY + rect.top * scale),
                                                size = Size(rect.width() * scale, rect.height() * scale),
                                                style = Stroke(width = 1.dp.toPx())
                                            )
                                        }

                                        val grid = state.grid ?: return@Canvas
                                        
                                        grid.region?.let { reg -> 
                                            drawRect(
                                                color = Color.Blue.copy(alpha = 0.4f),
                                                topLeft = Offset(startX + reg.left * scale, startY + reg.top * scale),
                                                size = Size((reg.right - reg.left) * scale, (reg.bottom - reg.top) * scale),
                                                style = Stroke(width = 4.dp.toPx())
                                            )
                                        }

                                        grid.rows.forEach { row ->
                                            drawLine(
                                                color = Color.Green.copy(alpha = 0.8f),
                                                start = Offset(startX, startY + row.top * scale),
                                                end = Offset(startX + imageDrawWidth, startY + row.top * scale),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                            drawLine(
                                                color = Color.Green.copy(alpha = 0.8f),
                                                start = Offset(startX, startY + row.bottom * scale),
                                                end = Offset(startX + imageDrawWidth, startY + row.bottom * scale),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }

                                        grid.columns.forEach { col ->
                                            drawLine(
                                                color = Color.Yellow.copy(alpha = 0.5f),
                                                start = Offset(startX + col.left * scale, startY),
                                                end = Offset(startX + col.left * scale, startY + imageDrawHeight),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                            drawContext.canvas.nativeCanvas.drawText(
                                                "C${col.index}", 
                                                startX + col.left * scale + 5f, 
                                                startY + 40f, 
                                                android.graphics.Paint().apply { color = android.graphics.Color.YELLOW; textSize = 30f }
                                            )
                                        }

                                        grid.cells.forEach { cell ->
                                            drawRect(
                                                color = Color.Cyan.copy(alpha = 0.3f),
                                                topLeft = Offset(startX + cell.rect.left * scale, startY + cell.rect.top * scale),
                                                size = Size(cell.rect.width() * scale, cell.rect.height() * scale),
                                                style = Stroke(width = 2.dp.toPx())
                                            )
                                            cell.blocks.forEach { b ->
                                                val br = b.boundingBox ?: return@forEach
                                                drawRect(
                                                    color = Color.Magenta.copy(alpha = 0.8f),
                                                    topLeft = Offset(startX + br.left * scale, startY + br.top * scale),
                                                    size = Size(br.width() * scale, br.height() * scale),
                                                    style = Stroke(width = 2.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                LazyColumn(modifier = Modifier.weight(0.6f).padding(8.dp)) {
                                    val semantic = state.semantic
                                    if (semantic != null) {
                                        val grid = semantic.grid
                                        
                                        // ===== CONCISE DIAGNOSTIC SUMMARY =====
                                        item {
                                            Text("========== TIME EXTRACTION ==========", fontWeight = FontWeight.Bold, color = Color.Magenta)
                                            Text("Candidates detected: ${grid.timeCandidates.size}")
                                            Text("Candidates accepted: ${grid.timeCandidates.count { it.isAccepted }}")
                                            Text("Candidates rejected: ${grid.timeCandidates.count { !it.isAccepted }}")
                                            Text("Columns inferred: ${grid.columns.size}")
                                            
                                            Spacer(Modifier.height(8.dp))
                                            Text("========== GRID ==========", fontWeight = FontWeight.Bold, color = Color.Magenta)
                                            Text("Day rows: ${grid.rows.size}")
                                            Text("Time columns: ${grid.columns.size}")
                                            Text("Physical positions: ${grid.theoreticalCellCount}")
                                            Text("Merged OCR objects: ${grid.mergedObjectsCount}")
                                            
                                            Spacer(Modifier.height(8.dp))
                                            Text("========== SEMANTICS ==========", fontWeight = FontWeight.Bold, color = Color.Magenta)
                                            Text("Normal classes: ${semantic.classCount}")
                                            Text("Merged positions: ${semantic.mergedClassCount}")
                                            Text("Empty positions: ${semantic.emptyCount}")
                                            Text("Non-class positions: ${semantic.nonClassCount}")
                                            Text("Unknown positions: ${semantic.unknownCount}")
                                            
                                            Spacer(Modifier.height(8.dp))
                                            Text("========== ACCOUNTING ==========", fontWeight = FontWeight.Bold, color = Color.Magenta)
                                            Text("Expected positions: ${grid.theoreticalCellCount}")
                                            Text("Accounted positions: ${semantic.totalAccounted}")
                                            val consistent = semantic.totalAccounted == grid.theoreticalCellCount
                                            Text("Consistent: ${if (consistent) "YES" else "NO"}", 
                                                fontWeight = FontWeight.Bold, 
                                                color = if (consistent) Color.Green else Color.Red)
                                        }
                                        
                                        // ===== TIME CANDIDATE DETAILS =====
                                        item {
                                            Spacer(Modifier.height(16.dp))
                                            Text("========== TIME CANDIDATE DETAILS ==========", fontWeight = FontWeight.Bold, color = Color.Blue)
                                        }
                                        items(grid.timeCandidates) { cand ->
                                            Column(Modifier.padding(vertical = 2.dp)) {
                                                val status = if (cand.isAccepted) "✓" else "✗"
                                                Text("$status '${cand.parsedStart ?: "?"} - ${cand.parsedEnd ?: "?"}' estX=${cand.estimatedCenterX}", fontSize = 11.sp,
                                                    color = if (cand.isAccepted) Color.Green else Color.Red)
                                                Text("  Raw: '${cand.rawText.take(60)}' | ${cand.reason.take(50)}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                        
                                        // ===== CLASS SCHEDULE =====
                                        item {
                                            Spacer(Modifier.height(16.dp))
                                            Text("========== CLASS SCHEDULE ==========", fontWeight = FontWeight.Bold, color = Color.Blue)
                                        }
                                        val classes = semantic.classifications.filter { 
                                            it.type == SemanticType.CLASS || it.type == SemanticType.MERGED_CLASS 
                                        }
                                        items(classes) { c ->
                                            Column(Modifier.padding(vertical = 2.dp)) {
                                                val colText = if (c.startColumn == c.endColumn) "${c.startColumn}" else "${c.startColumn}-${c.endColumn}"
                                                Text("Day ${c.day} | Col $colText | ${c.timeRange}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text("  Subject: ${c.extractedSubject ?: "N/A"}", fontSize = 11.sp)
                                                val rawEv = c.blockData.joinToString(" | ") { it.text.take(30) }
                                                Text("  OCR: $rawEv", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                        
                                        // ===== EMPTY / UNKNOWN / NON-CLASS =====
                                        item {
                                            Spacer(Modifier.height(16.dp))
                                            Text("========== NON-CLASS & EMPTY ==========", fontWeight = FontWeight.Bold, color = Color.Blue)
                                        }
                                        val nonClass = semantic.classifications.filter { 
                                            it.type == SemanticType.EMPTY || it.type == SemanticType.NON_CLASS || it.type == SemanticType.UNKNOWN
                                        }
                                        items(nonClass) { c ->
                                            val colText = if (c.startColumn == c.endColumn) "${c.startColumn}" else "${c.startColumn}-${c.endColumn}"
                                            Text("Day ${c.day} Col $colText: ${c.type.name} | ${c.reason.take(40)}", fontSize = 11.sp,
                                                color = when(c.type) { SemanticType.EMPTY -> Color.Gray; SemanticType.NON_CLASS -> Color(0xFFFF9800); else -> Color.Red })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun processFile(file: File, isPdf: Boolean) {
        isProcessing = true
        val blocks = provider.performDebugOcr(file)
        
        var displayBitmap: Bitmap? = null
        if (isPdf) {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                displayBitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(displayBitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(displayBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
            }
            renderer.close()
            fd.close()
        } else {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            displayBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }
        
        val gridResult = gridEngine.inferGrid(blocks)
        val semanticResult = semanticEngine.inferSemantics(gridResult)

        debugState = DebugState(displayBitmap, blocks, gridResult, semanticResult)
        isProcessing = false
    }
}
