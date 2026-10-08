package com.adityaram.present.domain.import

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class LocalTimetableExtractionProvider(private val context: Context) : TimetableExtractionProvider {
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extract(file: File): Result<ExtractionResult> {
        // Not implemented for production yet. 
        // Returning empty data to satisfy the interface for Phase 1.
        return Result.success(ExtractionResult("{}", emptyList()))
    }

    /**
     * DEBUG ONLY: Diagnostic pathway for testing ML Kit raw output.
     */
    suspend fun performDebugOcr(file: File): List<OcrTextBlock> = withContext(Dispatchers.IO) {
        val blocks = mutableListOf<OcrTextBlock>()
        
        try {
            if (file.extension.equals("pdf", ignoreCase = true)) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(fd)
                for (pageIndex in 0 until pdfRenderer.pageCount) {
                    val page = pdfRenderer.openPage(pageIndex)
                    
                    // Render for ML kit. Use scale factor 2 to improve OCR quality (simulating ~144 DPI)
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    // Fill background white explicitly just in case PDF has transparent bg
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val visionText = recognizer.process(image).await()
                    
                    for (block in visionText.textBlocks) {
                        blocks.add(
                            OcrTextBlock(
                                text = block.text,
                                boundingBox = block.boundingBox,
                                confidence = null, // Standard ML Kit doesn't expose confidence directly on blocks.
                                pageIndex = pageIndex
                            )
                        )
                    }
                }
                pdfRenderer.close()
                fd.close()
            } else {
                // Image handling with correct EXIF orientation preservation
                var bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val exif = ExifInterface(file.absolutePath)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                
                bitmap = rotateBitmapIfNeeded(bitmap, orientation)
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val visionText = recognizer.process(image).await()
                    
                    for (block in visionText.textBlocks) {
                        blocks.add(
                            OcrTextBlock(
                                text = block.text,
                                boundingBox = block.boundingBox,
                                confidence = null,
                                pageIndex = 0
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext blocks
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap?, orientation: Int): Bitmap? {
        if (bitmap == null) return null
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
