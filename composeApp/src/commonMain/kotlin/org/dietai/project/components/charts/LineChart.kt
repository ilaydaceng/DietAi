package org.dietai.project.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val textColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
    ) {
        val maxVal = data.maxOrNull() ?: 1.0
        val minVal = data.minOrNull() ?: 0.0
        val range = if (maxVal == minVal) 1.0 else maxVal - minVal

        val width = size.width
        val height = size.height
        val stepX = if (data.size > 1) width / (data.size - 1) else width

        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val normalizedY = 1 - ((value - minVal) / range)
            val y = (normalizedY * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            // Draw points
            drawCircle(
                color = lineColor,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )

            // Draw text values above points
            val valueText = ((value * 10).toInt() / 10.0).toString()
            val textLayoutResult = textMeasurer.measure(text = valueText)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x - textLayoutResult.size.width / 2, y - textLayoutResult.size.height - 10.dp.toPx()),
                color = textColor
            )
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
