package com.mj.screenslayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

/**
 * A slider that responds correctly to both touch and mouse drag (including the Android emulator).
 *
 * The standard Material3 Slider relies on internal touch gesture detection that doesn't
 * translate to mouse drag in the emulator. This implementation drives the slider purely
 * from raw [awaitPointerEvent] calls, which handle touch, mouse, and stylus uniformly.
 *
 * - Press anywhere on the track to jump there immediately.
 * - Drag smoothly; value updates on every pointer-move event.
 * - [onValueChangeFinished] fires on pointer release.
 */
@Composable
fun SmoothSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val trackActive   = MaterialTheme.colorScheme.primary
    val trackInactive = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor    = MaterialTheme.colorScheme.primary

    var widthPx by remember { mutableFloatStateOf(1f) }

    val fraction = ((value - valueRange.start) /
            (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange, widthPx) {
                val thumbRadius = 10.dp.toPx()

                fun xToValue(x: Float): Float {
                    val trackStart  = thumbRadius
                    val trackLength = (widthPx - 2f * thumbRadius).coerceAtLeast(1f)
                    val frac        = ((x - trackStart) / trackLength).coerceIn(0f, 1f)
                    return valueRange.start +
                            frac * (valueRange.endInclusive - valueRange.start)
                }

                awaitEachGesture {
                    // ── Wait for an initial press (skip hover/move events) ────
                    var down = awaitPointerEvent().changes
                        .firstOrNull { !it.previousPressed && it.pressed }
                    while (down == null) {
                        down = awaitPointerEvent().changes
                            .firstOrNull { !it.previousPressed && it.pressed }
                    }
                    down.consume()
                    onValueChange(xToValue(down.position.x))
                    val pointerId = down.id

                    // ── Track the same pointer until release ──────────────────
                    while (true) {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        change.consume()
                        if (!change.pressed) break          // finger/button lifted
                        onValueChange(xToValue(change.position.x))
                    }

                    onValueChangeFinished?.invoke()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 4.dp.toPx()
            val thumbRadius = 10.dp.toPx()
            val trackStart  = thumbRadius
            val trackLength = size.width - 2f * thumbRadius
            val trackY      = size.height / 2f
            val thumbX      = trackStart + fraction * trackLength

            // Inactive track
            drawRoundRect(
                color        = trackInactive,
                topLeft      = Offset(trackStart, trackY - trackHeight / 2f),
                size         = Size(trackLength, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f)
            )

            // Active track
            val activeWidth = (thumbX - trackStart).coerceAtLeast(0f)
            if (activeWidth > 0f) {
                drawRoundRect(
                    color        = trackActive,
                    topLeft      = Offset(trackStart, trackY - trackHeight / 2f),
                    size         = Size(activeWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f)
                )
            }

            // Thumb ripple (press-state halo)
            drawCircle(
                color  = thumbColor.copy(alpha = 0.12f),
                radius = thumbRadius * 2f,
                center = Offset(thumbX, trackY)
            )

            // Thumb
            drawCircle(
                color  = thumbColor,
                radius = thumbRadius,
                center = Offset(thumbX, trackY)
            )
        }
    }
}
