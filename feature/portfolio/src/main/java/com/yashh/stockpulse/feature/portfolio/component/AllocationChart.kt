package com.yashh.stockpulse.feature.portfolio.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yashh.stockpulse.feature.portfolio.PortfolioHolding

@Composable
fun AllocationChart(
    holdings: List<PortfolioHolding>,
    modifier: Modifier = Modifier
) {
    if (holdings.isEmpty()) return

    val total = holdings.sumOf { it.totalValue }

    // some predefined colors
    val colors = listOf(
        Color(0xFF448AFF), Color(0xFF00C853), Color(0xFFFFAB00),
        Color(0xFFE040FB), Color(0xFF00B0FF), Color(0xFFFF5252)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TODO: add touch interaction to show slice details
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f

            holdings.forEachIndexed { index, holding ->
                val sweepAngle = ((holding.totalValue / total) * 360f).toFloat()
                val color = colors[index % colors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Column(modifier = Modifier.fillMaxWidth()) {
            holdings.forEachIndexed { index, holding ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(colors[index % colors.size])
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = holding.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    val pct = (holding.totalValue / total) * 100
                    Text(
                        text = "${String.format("%.1f", pct)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
