package com.adityaram.present.domain.import

import android.graphics.Rect

/**
 * Clean internal data model representing an OCR text block extracted from ML Kit.
 */
data class OcrTextBlock(
    val text: String,
    val boundingBox: Rect?,
    val confidence: Float?, // May be null as ML Kit standard API doesn't always expose it natively.
    val pageIndex: Int
)

data class OcrData(
    val blocks: List<OcrTextBlock>
)
