package com.vasilev.voicedb.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilev.voicedb.R
import com.vasilev.voicedb.ui.theme.AccentAmber
import com.vasilev.voicedb.ui.theme.AccentCyan
import com.vasilev.voicedb.ui.theme.AccentGreen
import com.vasilev.voicedb.ui.theme.AccentRed
import com.vasilev.voicedb.ui.theme.BorderSubtle
import com.vasilev.voicedb.ui.theme.SurfaceCard
import com.vasilev.voicedb.ui.theme.TextMuted
import com.vasilev.voicedb.ui.theme.TextPrimary
import com.vasilev.voicedb.ui.theme.TextSecondary
import com.vasilev.voicedb.ui.theme.colorForDb

private const val GRAPH_MIN_DB = 0f
private const val GRAPH_MAX_DB = 110f
private val GRID_LEVELS = listOf(20, 40, 60, 80, 100)

@Composable
fun DecibelGraph(
    history: List<Float>,
    maxHistorySize: Int,
    maxDb: Float?,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 16)
    val nowLabel = stringResource(R.string.graph_now)
    val unitSec = stringResource(R.string.unit_sec)
    val waitingText = stringResource(R.string.graph_waiting)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.graph_title),
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (maxDb != null) {
                    Text(
                        text = stringResource(R.string.graph_max_line, maxDb),
                        color = AccentAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val leftGutter = 34.dp.toPx()
                    val rightPadding = 10.dp.toPx()
                    val topPadding = 10.dp.toPx()
                    val bottomGutter = 22.dp.toPx()

                    val chartLeft = leftGutter
                    val chartRight = (size.width - rightPadding).coerceAtLeast(chartLeft + 1f)
                    val chartTop = topPadding
                    val chartBottom = (size.height - bottomGutter).coerceAtLeast(chartTop + 1f)
                    val chartWidth = chartRight - chartLeft
                    val chartHeight = chartBottom - chartTop

                    fun dbToY(db: Float): Float {
                        val norm = ((db - GRAPH_MIN_DB) / (GRAPH_MAX_DB - GRAPH_MIN_DB))
                            .coerceIn(0f, 1f)
                        return chartBottom - norm * chartHeight
                    }

                    val axisLabelStyle = TextStyle(
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFeatureSettings = "tnum"
                    )
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))

                    // Horizontal decibel grid lines & Y-axis labels
                    GRID_LEVELS.forEach { level ->
                        val y = dbToY(level.toFloat())
                        val isSpeechRef = level == 60
                        drawLine(
                            color = if (isSpeechRef) Color(0xFF2D436B) else Color(0xFF1D2A44),
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = if (isSpeechRef) 1.3.dp.toPx() else 1.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        val labelResult = textMeasurer.measure(
                            text = level.toString(),
                            style = axisLabelStyle
                        )
                        drawText(
                            textLayoutResult = labelResult,
                            topLeft = Offset(
                                x = (leftGutter - labelResult.size.width - 8.dp.toPx()).coerceAtLeast(0f),
                                y = y - labelResult.size.height / 2f
                            )
                        )
                    }

                    // Bottom baseline
                    drawLine(
                        color = Color(0xFF243452),
                        start = Offset(chartLeft, chartBottom),
                        end = Offset(chartRight, chartBottom),
                        strokeWidth = 1.dp.toPx()
                    )

                    // X-axis time labels (15 second rolling window)
                    val xLabels = listOf(
                        0.0f to "-15 $unitSec",
                        0.333f to "-10 $unitSec",
                        0.666f to "-5 $unitSec",
                        1.0f to nowLabel
                    )
                    xLabels.forEachIndexed { index, (fraction, text) ->
                        val measured = textMeasurer.measure(text = text, style = axisLabelStyle)
                        val anchorX = chartLeft + fraction * chartWidth
                        val x = when (index) {
                            0 -> anchorX
                            xLabels.lastIndex -> (anchorX - measured.size.width).coerceAtLeast(chartLeft)
                            else -> anchorX - measured.size.width / 2f
                        }
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(x, chartBottom + 5.dp.toPx())
                        )
                    }

                    // Peak hold dashed reference line
                    if (maxDb != null && maxDb > 5f) {
                        val peakY = dbToY(maxDb)
                        drawLine(
                            color = AccentAmber.copy(alpha = 0.45f),
                            start = Offset(chartLeft, peakY),
                            end = Offset(chartRight, peakY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    if (history.isEmpty()) return@Canvas

                    val totalSlots = (maxHistorySize - 1).coerceAtLeast(1)
                    val startSlot = (maxHistorySize - history.size).coerceAtLeast(0)

                    val points = ArrayList<Offset>(history.size)
                    for (i in history.indices) {
                        val slot = startSlot + i
                        val x = chartLeft + (slot.toFloat() / totalSlots) * chartWidth
                        val y = dbToY(history[i])
                        points.add(Offset(x, y))
                    }

                    val lineStrokeBrush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to AccentRed,
                            0.25f to AccentAmber,
                            0.52f to AccentGreen,
                            0.82f to AccentCyan,
                            1.00f to AccentCyan
                        ),
                        startY = chartTop,
                        endY = chartBottom
                    )

                    val areaFillBrush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to AccentRed.copy(alpha = 0.36f),
                            0.25f to AccentAmber.copy(alpha = 0.28f),
                            0.52f to AccentGreen.copy(alpha = 0.22f),
                            0.85f to AccentCyan.copy(alpha = 0.08f),
                            1.00f to Color.Transparent
                        ),
                        startY = chartTop,
                        endY = chartBottom
                    )

                    if (points.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val midX = (prev.x + curr.x) / 2f
                                val midY = (prev.y + curr.y) / 2f
                                quadraticTo(prev.x, prev.y, midX, midY)
                            }
                            lineTo(points.last().x, points.last().y)
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().x, chartBottom)
                            lineTo(points.first().x, chartBottom)
                            close()
                        }

                        clipRect(
                            left = chartLeft,
                            top = chartTop - 2.dp.toPx(),
                            right = chartRight + 2.dp.toPx(),
                            bottom = chartBottom
                        ) {
                            drawPath(path = fillPath, brush = areaFillBrush)
                            drawPath(
                                path = linePath,
                                brush = lineStrokeBrush,
                                style = Stroke(
                                    width = 2.6.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Live point marker on the right edge
                    val lastPoint = points.last()
                    val lastDb = history.last()
                    val pointColor = colorForDb(lastDb)
                    if (isListening) {
                        drawCircle(
                            color = pointColor.copy(alpha = 0.26f),
                            radius = 10.dp.toPx(),
                            center = lastPoint
                        )
                    }
                    drawCircle(
                        color = pointColor,
                        radius = 4.5.dp.toPx(),
                        center = lastPoint
                    )
                    drawCircle(
                        color = TextPrimary,
                        radius = 2.dp.toPx(),
                        center = lastPoint
                    )
                }

                if (history.isEmpty() && isListening) {
                    Text(
                        text = waitingText,
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
