package com.adityaram.present.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.adityaram.present.ui.theme.Dimens
import com.adityaram.present.ui.theme.LocalPresentColors
import com.adityaram.present.ui.theme.Typography

@Composable
fun WelcomeScreen(
    onNavigateNext: () -> Unit
) {
    val colors = LocalPresentColors.current

    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(Dimens.spacing24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // Hero Graphic & Title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier
                .size(80.dp)
                .offset(y = floatY.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                drawCircle(
                    color = colors.accent,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 8.dp.toPx())
                )
                drawCircle(
                    color = colors.accent,
                    radius = size.minDimension / 6
                )
            }
            Spacer(modifier = Modifier.height(Dimens.spacing32))
            Text(
                text = "Welcome to Present",
                style = Typography.displayMedium,
                color = colors.primaryText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Dimens.spacing12))
            Text(
                text = "Smart, private attendance tracking.",
                style = Typography.bodyLarge,
                color = colors.secondaryText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(0.7f))

        // Action Button
        Button(
            onClick = onNavigateNext,
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color(0xFF101012)),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Get started",
                style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        
        Spacer(modifier = Modifier.height(Dimens.spacing24))
    }
}
