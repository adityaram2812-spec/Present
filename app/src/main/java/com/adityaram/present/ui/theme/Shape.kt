package com.adityaram.present.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(14.dp), // Buttons and interactive blocks
    medium = RoundedCornerShape(18.dp), // Standard course cards
    large = RoundedCornerShape(24.dp) // Hero and summary containers
)

val PillShape = RoundedCornerShape(9999.dp)

object Dimens {
    val spacing4 = 4.dp
    val spacing8 = 8.dp
    val spacing12 = 12.dp
    val spacing16 = 16.dp
    val spacing20 = 20.dp
    val spacing24 = 24.dp
    val spacing32 = 32.dp
    val spacing40 = 40.dp
    
    val screenHorizontalPadding = 20.dp
    val buttonHeight = 52.dp // Primary action target height
}
