package com.adityaram.present.ui.about

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random

data class Particle(
    val id: Int,
    val xRatio: Float,
    var yRatio: Float,
    val size: Float,
    val speed: Float,
    val drift: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val delayMs: Long,
    var opacity: Float = 0f,
    var isActive: Boolean = false
)

@Composable
fun HeartEasterEggOverlay(
    trigger: Long,
    accentColor: Color,
    onComplete: () -> Unit
) {
    if (trigger == 0L) return

    val particles = remember(trigger) {
        List(35) {
            Particle(
                id = it,
                xRatio = Random.nextFloat() * 0.6f + 0.2f, // Centered distribution
                yRatio = 1.05f, // Start slightly below viewport
                size = Random.nextFloat() * 40f + 15f,
                speed = Random.nextFloat() * 0.006f + 0.005f,
                drift = (Random.nextFloat() - 0.5f) * 0.003f, 
                rotation = Random.nextFloat() * 60f - 30f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 1.5f,
                delayMs = Random.nextLong(0, 400)
            )
        }.toMutableStateList()
    }

    LaunchedEffect(trigger) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 2000) {
            val timeSinceStart = System.currentTimeMillis() - startTime
            var allDead = true

            for (i in particles.indices) {
                val p = particles[i]
                if (timeSinceStart >= p.delayMs) {
                    if (!p.isActive && p.yRatio > -0.2f) {
                        particles[i] = p.copy(isActive = true, opacity = 1f)
                    }
                    if (particles[i].isActive) {
                        val newY = particles[i].yRatio - particles[i].speed
                        val newX = particles[i].xRatio + particles[i].drift
                        val newRot = particles[i].rotation + particles[i].rotationSpeed
                        
                        // Fade out near the top
                        val newOpacity = if (newY < 0.3f) (newY / 0.3f).coerceIn(0f, 1f) else 1f
                        
                        particles[i] = particles[i].copy(
                            yRatio = newY,
                            xRatio = newX,
                            rotation = newRot,
                            opacity = newOpacity
                        )
                    }
                }
                if (particles[i].yRatio > -0.2f) {
                    allDead = false
                }
            }
            if (allDead && timeSinceStart > 1000) break
            
            withFrameNanos { } 
        }
        onComplete()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            if (p.isActive && p.opacity > 0f) {
                withTransform({
                    translate(p.xRatio * w, p.yRatio * h)
                    rotate(p.rotation)
                }) {
                    val cw = p.size
                    val ch = p.size
                    val path = Path().apply {
                        moveTo(cw / 2, ch / 5)
                        cubicTo(cw * 5 / 14, 0f, 0f, cw / 15, cw / 28, ch * 2 / 5)
                        cubicTo(cw / 14, ch * 2 / 3, cw * 3 / 7, ch * 5 / 6, cw / 2, ch)
                        cubicTo(cw * 4 / 7, ch * 5 / 6, cw * 13 / 14, ch * 2 / 3, cw * 27 / 28, ch * 2 / 5)
                        cubicTo(cw, cw / 15, cw * 9 / 14, 0f, cw / 2, ch / 5)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = accentColor.copy(alpha = p.opacity * 0.8f) // Slight native transparency
                    )
                }
            }
        }
    }
}
