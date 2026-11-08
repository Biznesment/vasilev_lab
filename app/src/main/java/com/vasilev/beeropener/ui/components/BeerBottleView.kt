package com.vasilev.beeropener.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.vasilev.beeropener.model.BeerSort
import kotlin.math.PI
import kotlin.math.sin

/**
 * Интерактивная бутылка с кастомной отрисовкой на Canvas,
 * поддержкой свайпа вверх для срыва крышки и анимацией пены.
 */
@Composable
fun BeerBottleView(
    sort: BeerSort,
    isOpened: Boolean,
    pullProgress: Float,
    onPullCap: (Float) -> Unit,
    onPullRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnPullCap by rememberUpdatedState(onPullCap)
    val currentOnPullRelease by rememberUpdatedState(onPullRelease)
    val dragThresholdPx = with(LocalDensity.current) { 110.dp.toPx() }

    // Анимация полёта сорванной крышки
    val capFlyProgress by animateFloatAsState(
        targetValue = if (isOpened) 1f else 0f,
        animationSpec = if (isOpened) {
            tween(durationMillis = 650, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "capFly"
    )

    // Пружинная анимация шапки пены при открытии
    val foamProgress by animateFloatAsState(
        targetValue = if (isOpened) 1f else 0f,
        animationSpec = if (isOpened) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        } else {
            snap()
        },
        label = "foamRise"
    )

    // Бесконечная анимация пузырьков газа внутри бутылки
    val infiniteTransition = rememberInfiniteTransition(label = "bubbles")
    val bubblePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubblePhase"
    )

    val glassColor = Color(sort.glassColorHex)
    val liquidColor = Color(sort.liquidColorHex)
    val highlightColor = Color(sort.highlightColorHex)
    val foamColor = Color(sort.foamColorHex)
    val capColor = Color(sort.capColorHex)

    Box(
        modifier = modifier.pointerInput(isOpened) {
            if (!isOpened) {
                detectVerticalDragGestures(
                    onDragEnd = { currentOnPullRelease() },
                    onDragCancel = { currentOnPullRelease() },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = (-dragAmount) / dragThresholdPx
                        currentOnPullCap(delta)
                    }
                )
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            val neckTop = h * 0.22f
            val neckBottom = h * 0.42f
            val shoulderBottom = h * 0.55f
            val bottleBottom = h * 0.91f

            val neckHalfW = w * 0.082f
            val bodyHalfW = w * 0.225f
            val bottomRadius = w * 0.045f

            // Мягкая тень под бутылкой
            drawOval(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(cx - bodyHalfW * 0.95f, bottleBottom - h * 0.015f),
                size = Size(bodyHalfW * 1.9f, h * 0.045f)
            )

            // Контур стеклянной бутылки
            val bottlePath = Path().apply {
                moveTo(cx - neckHalfW, neckTop)
                lineTo(cx + neckHalfW, neckTop)
                lineTo(cx + neckHalfW, neckBottom)
                cubicTo(
                    cx + neckHalfW,
                    neckBottom + (shoulderBottom - neckBottom) * 0.45f,
                    cx + bodyHalfW,
                    neckBottom + (shoulderBottom - neckBottom) * 0.45f,
                    cx + bodyHalfW,
                    shoulderBottom
                )
                lineTo(cx + bodyHalfW, bottleBottom - bottomRadius)
                quadraticTo(
                    cx + bodyHalfW,
                    bottleBottom,
                    cx + bodyHalfW - bottomRadius,
                    bottleBottom
                )
                lineTo(cx - bodyHalfW + bottomRadius, bottleBottom)
                quadraticTo(
                    cx - bodyHalfW,
                    bottleBottom,
                    cx - bodyHalfW,
                    bottleBottom - bottomRadius
                )
                lineTo(cx - bodyHalfW, shoulderBottom)
                cubicTo(
                    cx - bodyHalfW,
                    neckBottom + (shoulderBottom - neckBottom) * 0.45f,
                    cx - neckHalfW,
                    neckBottom + (shoulderBottom - neckBottom) * 0.45f,
                    cx - neckHalfW,
                    neckBottom
                )
                close()
            }

            // Внутреннее содержимое бутылки (стекло, напиток, пузырьки, этикетка)
            clipPath(bottlePath) {
                drawRect(color = glassColor)

                val liquidTop = neckTop + (neckBottom - neckTop) * (0.38f - 0.14f * foamProgress)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(highlightColor, liquidColor, glassColor),
                        startY = liquidTop,
                        endY = bottleBottom
                    ),
                    topLeft = Offset(cx - bodyHalfW, liquidTop),
                    size = Size(bodyHalfW * 2f, bottleBottom - liquidTop)
                )

                // Тонкая полоска пены над уровнем жидкости
                drawRect(
                    color = foamColor.copy(alpha = 0.85f),
                    topLeft = Offset(cx - bodyHalfW, liquidTop - h * 0.012f),
                    size = Size(bodyHalfW * 2f, h * 0.016f)
                )

                // Поднимающиеся пузырьки газа
                val bubbleCount = if (isOpened) 14 else 6
                val speedMultiplier = if (isOpened) 1.6f else 0.7f
                for (i in 0 until bubbleCount) {
                    val progress = ((bubblePhase * speedMultiplier + i * 0.13f) % 1f)
                    val bx = cx + sin(i * 2.35f) * (bodyHalfW * 0.68f)
                    val by = bottleBottom - progress * (bottleBottom - liquidTop - h * 0.02f)
                    val radius = (2.2f + (i % 3) * 1.4f) * (if (isOpened) 1.25f else 0.9f)
                    drawCircle(
                        color = foamColor.copy(alpha = if (isOpened) 0.55f else 0.28f),
                        radius = radius.dp.toPx() * 0.45f,
                        center = Offset(bx, by)
                    )
                }

                // Этикетка на корпусе бутылки
                val labelTop = shoulderBottom + (bottleBottom - shoulderBottom) * 0.18f
                val labelHeight = (bottleBottom - shoulderBottom) * 0.44f
                val labelWidth = bodyHalfW * 1.68f
                drawRoundRect(
                    color = Color(0xFFFEF3C7),
                    topLeft = Offset(cx - labelWidth / 2f, labelTop),
                    size = Size(labelWidth, labelHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                // Акцентная полоса на этикетке
                drawRoundRect(
                    color = capColor,
                    topLeft = Offset(cx - labelWidth * 0.42f, labelTop + labelHeight * 0.16f),
                    size = Size(labelWidth * 0.84f, labelHeight * 0.22f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                // Декоративные полоски текста на этикетке
                drawRoundRect(
                    color = Color(0xFF92400E).copy(alpha = 0.55f),
                    topLeft = Offset(cx - labelWidth * 0.32f, labelTop + labelHeight * 0.54f),
                    size = Size(labelWidth * 0.64f, labelHeight * 0.10f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF92400E).copy(alpha = 0.35f),
                    topLeft = Offset(cx - labelWidth * 0.22f, labelTop + labelHeight * 0.72f),
                    size = Size(labelWidth * 0.44f, labelHeight * 0.08f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Световой блик на стекле слева
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.20f),
                    topLeft = Offset(cx - bodyHalfW * 0.78f, shoulderBottom + h * 0.02f),
                    size = Size(bodyHalfW * 0.20f, (bottleBottom - shoulderBottom) * 0.78f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset(cx - neckHalfW * 0.68f, neckTop + h * 0.02f),
                    size = Size(neckHalfW * 0.28f, (neckBottom - neckTop) * 0.8f),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            // Контур стекла и ободок горлышка
            drawPath(
                path = bottlePath,
                color = Color.White.copy(alpha = 0.28f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawRoundRect(
                color = glassColor,
                topLeft = Offset(cx - neckHalfW * 1.12f, neckTop - h * 0.006f),
                size = Size(neckHalfW * 2.24f, h * 0.022f),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            // Пышная пена после открытия бутылки
            if (foamProgress > 0.01f) {
                drawFoamHead(
                    cx = cx,
                    neckTop = neckTop,
                    neckHalfW = neckHalfW,
                    foamProgress = foamProgress,
                    foamColor = foamColor
                )
            }

            // Брызги газа в момент срыва крышки
            if (capFlyProgress in 0.02f..0.75f) {
                val burstAlpha = (1f - (capFlyProgress / 0.75f)).coerceIn(0f, 1f)
                for (i in -2..2) {
                    val dropX = cx + i * neckHalfW * 0.55f * (0.4f + capFlyProgress)
                    val dropY = neckTop - h * 0.11f * capFlyProgress - kotlin.math.abs(i) * 4f
                    drawCircle(
                        color = foamColor.copy(alpha = burstAlpha * 0.75f),
                        radius = 3.2.dp.toPx(),
                        center = Offset(dropX, dropY)
                    )
                }
            }

            // Рычаг открывашки при свайпе вверх
            if (!isOpened && pullProgress > 0.02f) {
                withTransform({
                    translate(left = cx - neckHalfW * 0.9f, top = neckTop)
                    rotate(degrees = -18f - pullProgress * 26f, pivot = Offset.Zero)
                }) {
                    drawRoundRect(
                        color = Color(0xFF9CA3AF),
                        topLeft = Offset(-neckHalfW * 2.4f, -5.dp.toPx()),
                        size = Size(neckHalfW * 2.8f, 10.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }

            // Металлическая корончатая крышка (на горлышке или в полёте)
            if (capFlyProgress < 0.98f) {
                val capWidth = neckHalfW * 2.36f
                val capHeight = h * 0.036f
                val capAlpha = if (capFlyProgress > 0.75f) {
                    ((1f - capFlyProgress) / 0.25f).coerceIn(0f, 1f)
                } else {
                    1f
                }

                val offsetX = if (isOpened) w * 0.34f * capFlyProgress else 0f
                val offsetY = if (isOpened) {
                    -h * 0.20f * sin(capFlyProgress * PI).toFloat() +
                        h * 0.10f * capFlyProgress * capFlyProgress
                } else {
                    -pullProgress * 8.dp.toPx()
                }
                val rotation = if (isOpened) {
                    480f * capFlyProgress
                } else {
                    pullProgress * 22f
                }

                withTransform({
                    translate(left = cx + offsetX, top = neckTop + offsetY)
                    rotate(degrees = rotation, pivot = Offset.Zero)
                }) {
                    drawCrownCap(
                        capWidth = capWidth,
                        capHeight = capHeight,
                        capColor = capColor.copy(alpha = capAlpha)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFoamHead(
    cx: Float,
    neckTop: Float,
    neckHalfW: Float,
    foamProgress: Float,
    foamColor: Color
) {
    val scale = foamProgress.coerceAtLeast(0f)

    // Потёк пены по горлышку
    drawRoundRect(
        color = foamColor,
        topLeft = Offset(cx - neckHalfW * 0.55f, neckTop - 2.dp.toPx()),
        size = Size(neckHalfW * 0.65f, neckHalfW * 2.1f * scale),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )
    drawRoundRect(
        color = foamColor.copy(alpha = 0.92f),
        topLeft = Offset(cx + neckHalfW * 0.12f, neckTop - 2.dp.toPx()),
        size = Size(neckHalfW * 0.48f, neckHalfW * 1.45f * scale),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    // Пушистые шарики пены сверху горлышка
    val puffs = listOf(
        Triple(-0.78f, -0.15f, 0.62f),
        Triple(0.78f, -0.12f, 0.60f),
        Triple(-0.35f, -0.58f, 0.72f),
        Triple(0.38f, -0.52f, 0.68f),
        Triple(0.0f, -0.82f, 0.78f)
    )
    for ((dxFactor, dyFactor, radiusFactor) in puffs) {
        drawCircle(
            color = foamColor,
            radius = neckHalfW * radiusFactor * scale,
            center = Offset(
                x = cx + neckHalfW * dxFactor * scale,
                y = neckTop + neckHalfW * dyFactor * scale
            )
        )
    }
}

private fun DrawScope.drawCrownCap(
    capWidth: Float,
    capHeight: Float,
    capColor: Color
) {
    // Основная часть крышки
    drawRoundRect(
        color = capColor,
        topLeft = Offset(-capWidth / 2f, -capHeight * 0.75f),
        size = Size(capWidth, capHeight),
        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
    )
    // Блик сверху крышки
    drawRoundRect(
        color = Color.White.copy(alpha = 0.28f * capColor.alpha),
        topLeft = Offset(-capWidth * 0.36f, -capHeight * 0.62f),
        size = Size(capWidth * 0.72f, capHeight * 0.25f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    // Зубчики металлической юбки крышки
    val teeth = 6
    val step = capWidth / teeth
    for (i in 0 until teeth) {
        val toothX = -capWidth / 2f + i * step + step * 0.15f
        drawRoundRect(
            color = Color(0xFFD1D5DB).copy(alpha = capColor.alpha),
            topLeft = Offset(toothX, capHeight * 0.08f),
            size = Size(step * 0.7f, capHeight * 0.28f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}
