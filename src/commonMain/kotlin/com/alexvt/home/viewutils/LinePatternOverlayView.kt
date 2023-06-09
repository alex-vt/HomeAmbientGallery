package com.alexvt.home.viewutils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

@Composable
fun LinePatternOverlayView(
    lineColor: Color,
    backgroundColor: Color = Color.Transparent,
    linePitchDp: Dp,
    lineThicknessDp: Dp,
    isAnimate: Boolean = false,
) {
    /**
     * Tiling line pattern:
     * +--------#--------+
     * |     #           |
     * |  #              |
     * #                 #
     * |              #  |
     * |           #     |
     * +--------#--------+
     */
    val density = LocalDensity.current.density
    val lineThickness = remember { lineThicknessDp.value.roundToInt() }
    val lineHalfGap = remember { ((linePitchDp - lineThicknessDp).value / 2).roundToInt() }
    val gradientList = remember {
        List(lineHalfGap) { backgroundColor } +
                List(lineThickness) { lineColor } +
                List(2 * lineHalfGap) { backgroundColor } +
                List(lineThickness) { lineColor } +
                List(lineHalfGap) { backgroundColor }
    }
    val tileSize = remember { density * gradientList.size }
    val offsetX =
        if (isAnimate) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = tileSize,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing))
            )
        } else {
            mutableStateOf(0f)
        }
    val brush =
        Brush.linearGradient(
            gradientList,
            start = Offset(offsetX.value, 0f),
            end = Offset(tileSize + offsetX.value, tileSize),
            tileMode = TileMode.Repeated
        )
    Box(Modifier.fillMaxSize().background(brush))

}
