package com.yashh.stockpulse.feature.stockdetail.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors
import com.yashh.stockpulse.core.model.CandleData
import kotlinx.collections.immutable.ImmutableList
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance Canvas-based candlestick chart with technical sub-charts and interactive scrubbing.
 *
 * Capabilities:
 * - Real-time OHLC candlestick rendering with pre-allocated Native/Compose Paints (zero GC churn).
 * - Synchronized Volume histogram bars anchored to the lower candle area.
 * - RSI (14-period) sub-chart with 70/30 overbought & oversold threshold bands.
 * - Semantic pinch-to-zoom and horizontal pan gestures.
 * - Tap-to-inspect crosshair, double-tap reset, pinch zoom and horizontal pan.
 * - Floating HUD readout display for active bar metrics (OHLC, Volume, RSI).
 */
@Composable
fun CandlestickChart(
    candles: ImmutableList<CandleData>,
    showSma: Boolean,
    showEma: Boolean,
    showRsi: Boolean = false,
    showVolume: Boolean = true,
    modifier: Modifier = Modifier,
    bullColor: Color = LocalTradingColors.current.bull,
    bearColor: Color = LocalTradingColors.current.bear
) {
    if (candles.isEmpty()) return

    val chartBg = LocalTradingColors.current.chartBg
    val gridColor = LocalTradingColors.current.chartGrid
    val axisWidth = with(LocalDensity.current) { 68.dp.toPx() }

    // Gesture state for visible range
    var visibleStartIndex by remember(candles) { mutableStateOf(max(0, candles.size - 50)) }
    var visibleEndIndex by remember(candles) { mutableStateOf(candles.size - 1) }
    var panRemainder by remember(candles) { mutableFloatStateOf(0f) }

    // Scrubbing state
    var hoveredIndexInVisible by remember { mutableStateOf<Int?>(null) }

    // Pre-allocated rendering objects
    val bullPaint = remember(bullColor) { Paint().apply { color = bullColor; isAntiAlias = true } }
    val bearPaint = remember(bearColor) { Paint().apply { color = bearColor; isAntiAlias = true } }
    val volBullPaint = remember(bullColor) { Paint().apply { color = bullColor.copy(alpha = 0.40f); isAntiAlias = true } }
    val volBearPaint = remember(bearColor) { Paint().apply { color = bearColor.copy(alpha = 0.40f); isAntiAlias = true } }
    val gridPaint = remember(gridColor) { Paint().apply { color = gridColor; strokeWidth = 1f } }

    val smaPaint = remember { Paint().apply { color = Color(0xFF78C9E8); style = PaintingStyle.Stroke; strokeWidth = 3f; isAntiAlias = true } }
    val emaPaint = remember { Paint().apply { color = Color(0xFFFFCA28); style = PaintingStyle.Stroke; strokeWidth = 3f; isAntiAlias = true } }
    val rsiPaint = remember { Paint().apply { color = Color(0xFFB4A4EF); style = PaintingStyle.Stroke; strokeWidth = 2.8f; isAntiAlias = true } }
    val rsiBandPaint = remember { Paint().apply { color = Color(0xFFB4A4EF).copy(alpha = 0.08f); isAntiAlias = true } }

    val crosshairDash = remember { PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) }
    val crosshairPaint = remember(crosshairDash) {
        Paint().apply {
            color = Color.White.copy(alpha = 0.65f)
            strokeWidth = 2f
            style = PaintingStyle.Stroke
            pathEffect = crosshairDash
        }
    }
    val badgeBgPaint = remember { Paint().apply { color = Color(0xFF24515C); isAntiAlias = true } }

    val smaPath = remember { Path() }
    val emaPath = remember { Path() }
    val rsiPath = remember { Path() }

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 28f
            isAntiAlias = true
        }
    }
    val badgeTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    // Technical indicator calculations (cached by dependencies)
    val smaData = remember(candles, showSma) {
        if (showSma) calculateSMA(candles.map { it.close.toFloat() }, 20) else emptyList()
    }
    val emaData = remember(candles, showEma) {
        if (showEma) calculateEMA(candles.map { it.close.toFloat() }, 20) else emptyList()
    }
    val rsiData = remember(candles, showRsi) {
        if (showRsi) calculateRSI(candles.map { it.close.toFloat() }, 14) else emptyList()
    }

    val visibleCandles = remember(candles, visibleStartIndex, visibleEndIndex) {
        candles.subList(visibleStartIndex.coerceIn(0, candles.size - 1), (visibleEndIndex + 1).coerceIn(1, candles.size))
    }
    val timeLabels = remember(visibleCandles) {
        val firstDay = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(visibleCandles.first().timestamp))
        val lastDay = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(visibleCandles.last().timestamp))
        val formatter = SimpleDateFormat(if (firstDay == lastDay) "HH:mm" else "MMM d", Locale.US)
        listOf(0, visibleCandles.lastIndex / 2, visibleCandles.lastIndex).map { index ->
            formatter.format(Date(visibleCandles[index].timestamp))
        }
    }

    val hoveredCandle = hoveredIndexInVisible?.let { idx ->
        visibleCandles.getOrNull(idx)
    }
    val hoveredGlobalIndex = hoveredIndexInVisible?.let { it + visibleStartIndex }
    val hoveredRsi = hoveredGlobalIndex?.let { rsiData.getOrNull(it) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(chartBg)
                .pointerInput(candles) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val count = visibleEndIndex - visibleStartIndex + 1
                        val chartWidth = (size.width - axisWidth).coerceAtLeast(1f)
                        if (zoom != 1f) {
                            val newCount = (count / zoom).toInt().coerceIn(min(12, candles.size), candles.size)
                            val anchor = (centroid.x / chartWidth).coerceIn(0f, 1f)
                            val anchorIndex = visibleStartIndex + (anchor * count).toInt()
                            val newStart = (anchorIndex - (anchor * newCount).toInt())
                                .coerceIn(0, candles.size - newCount)
                            visibleStartIndex = newStart
                            visibleEndIndex = newStart + newCount - 1
                            hoveredIndexInVisible = null
                            panRemainder = 0f
                        } else if (pan.x != 0f) {
                            panRemainder += pan.x
                            val spacing = chartWidth / count
                            val shift = (panRemainder / spacing).toInt()
                            if (shift != 0) {
                                val newStart = (visibleStartIndex - shift)
                                    .coerceIn(0, candles.size - count)
                                visibleStartIndex = newStart
                                visibleEndIndex = newStart + count - 1
                                hoveredIndexInVisible = null
                                panRemainder -= shift * spacing
                            }
                        }
                    }
                }
                .pointerInput(candles, visibleStartIndex, visibleEndIndex) {
                    detectTapGestures(
                        onTap = { offset ->
                            val chartWidth = (size.width - axisWidth).coerceAtLeast(1f)
                            val index = ((offset.x / chartWidth) * visibleCandles.size).toInt()
                                .coerceIn(0, visibleCandles.lastIndex)
                            hoveredIndexInVisible = index
                        },
                        onDoubleTap = {
                            visibleEndIndex = candles.lastIndex
                            visibleStartIndex = max(0, candles.size - 50)
                            hoveredIndexInVisible = null
                            panRemainder = 0f
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val priceLabelWidth = axisWidth
            val chartWidth = (width - priceLabelWidth).coerceAtLeast(1f)

            if (visibleCandles.isEmpty()) return@Canvas

            // Give price, volume and RSI their own vertical space.
            val timeAxisHeight = 32f
            val contentBottom = height - timeAxisHeight
            val rsiHeight = if (showRsi) contentBottom * 0.20f else 0f
            val rsiTop = contentBottom - rsiHeight
            val volumeHeight = if (showVolume) contentBottom * 0.17f else 0f
            val volumeTop = rsiTop - volumeHeight
            val priceHeight = (volumeTop - 12f).coerceAtLeast(1f)

            val rawMin = visibleCandles.minOf { it.low }
            val rawMax = visibleCandles.maxOf { it.high }
            val padding = max(0.01f, (rawMax - rawMin) * 0.08f)
            val minPrice = rawMin - padding
            val maxPrice = rawMax + padding
            val priceRange = max(0.001f, (maxPrice - minPrice).toFloat())

            val candleCount = visibleCandles.size
            val candleSpacing = chartWidth / candleCount
            val candleWidth = candleSpacing * 0.76f

            drawIntoCanvas { canvas ->
                // --- 1. Horizontal Price Grid & Labels ---
                val gridLines = 5
                for (i in 0..gridLines) {
                    val y = priceHeight * i / gridLines
                    val price = maxPrice - (i.toFloat() / gridLines * priceRange)

                    canvas.drawLine(
                        p1 = Offset(0f, y),
                        p2 = Offset(chartWidth, y),
                        paint = gridPaint
                    )

                    canvas.nativeCanvas.drawText(
                        formatPrice(price.toDouble()),
                        chartWidth + 10f,
                        y + 10f,
                        textPaint
                    )
                }

                // --- 2. Volume histogram in its own band ---
                if (showVolume) {
                    val maxVolume = visibleCandles.maxOfOrNull { it.volume }?.coerceAtLeast(1L) ?: 1L
                    canvas.drawLine(Offset(0f, volumeTop), Offset(chartWidth, volumeTop), gridPaint)
                    canvas.nativeCanvas.drawText("VOL", 8f, volumeTop + 25f, textPaint)

                    visibleCandles.forEachIndexed { index, candle ->
                        val x = index * candleSpacing + (candleSpacing / 2f)
                        val barHeight = ((candle.volume.toFloat() / maxVolume) * (volumeHeight - 12f)).coerceAtLeast(2f)
                        val barTop = rsiTop - barHeight
                        val vPaint = if (candle.close >= candle.open) volBullPaint else volBearPaint

                        canvas.drawRect(
                            left = x - candleWidth / 2f,
                            top = barTop,
                            right = x + candleWidth / 2f,
                            bottom = rsiTop,
                            paint = vPaint
                        )
                    }
                }

                // --- 3. Candlesticks (Wicks and Bodies) ---
                visibleCandles.forEachIndexed { index, candle ->
                    val x = index * candleSpacing + (candleSpacing / 2f)
                    val yOpen = priceHeight - ((candle.open - minPrice) / priceRange * priceHeight)
                    val yClose = priceHeight - ((candle.close - minPrice) / priceRange * priceHeight)
                    val yHigh = priceHeight - ((candle.high - minPrice) / priceRange * priceHeight)
                    val yLow = priceHeight - ((candle.low - minPrice) / priceRange * priceHeight)

                    val paint = if (candle.close >= candle.open) bullPaint else bearPaint

                    // Wick
                    canvas.drawLine(
                        p1 = Offset(x, yHigh),
                        p2 = Offset(x, yLow),
                        paint = paint.apply { strokeWidth = 2.2f }
                    )

                    // Candle Body
                    val topY = min(yOpen, yClose)
                    val bottomY = max(yOpen, yClose)
                    val bodyHeight = max(1.5f, bottomY - topY)

                    canvas.drawRect(
                        left = x - candleWidth / 2f,
                        top = topY,
                        right = x + candleWidth / 2f,
                        bottom = topY + bodyHeight,
                        paint = paint
                    )
                }

                // --- 4. Moving Average Overlays (SMA 20 & EMA 20) ---
                if (showSma && smaData.isNotEmpty()) {
                    smaPath.reset()
                    var isFirst = true
                    visibleCandles.forEachIndexed { index, _ ->
                        val gIdx = visibleStartIndex + index
                        val value = smaData.getOrNull(gIdx)
                        if (value != null) {
                            val x = index * candleSpacing + (candleSpacing / 2f)
                            val y = priceHeight - ((value - minPrice) / priceRange * priceHeight)
                            if (isFirst) { smaPath.moveTo(x, y); isFirst = false } else { smaPath.lineTo(x, y) }
                        }
                    }
                    canvas.drawPath(smaPath, smaPaint)
                }

                if (showEma && emaData.isNotEmpty()) {
                    emaPath.reset()
                    var isFirst = true
                    visibleCandles.forEachIndexed { index, _ ->
                        val gIdx = visibleStartIndex + index
                        val value = emaData.getOrNull(gIdx)
                        if (value != null) {
                            val x = index * candleSpacing + (candleSpacing / 2f)
                            val y = priceHeight - ((value - minPrice) / priceRange * priceHeight)
                            if (isFirst) { emaPath.moveTo(x, y); isFirst = false } else { emaPath.lineTo(x, y) }
                        }
                    }
                    canvas.drawPath(emaPath, emaPaint)
                }

                // --- 5. RSI Sub-Chart (Relative Strength Index 14) ---
                if (showRsi) {
                    // Divider line
                    canvas.drawLine(
                        p1 = Offset(0f, rsiTop),
                        p2 = Offset(chartWidth, rsiTop),
                        paint = gridPaint
                    )

                    val y70 = rsiTop + rsiHeight * (1f - 0.70f)
                    val y50 = rsiTop + rsiHeight * (1f - 0.50f)
                    val y30 = rsiTop + rsiHeight * (1f - 0.30f)

                    // Overbought (70) to Oversold (30) shaded band
                    canvas.drawRect(
                        left = 0f,
                        top = y70,
                        right = chartWidth,
                        bottom = y30,
                        paint = rsiBandPaint
                    )

                    // Reference thresholds
                    canvas.drawLine(Offset(0f, y70), Offset(chartWidth, y70), gridPaint)
                    canvas.drawLine(Offset(0f, y50), Offset(chartWidth, y50), gridPaint)
                    canvas.drawLine(Offset(0f, y30), Offset(chartWidth, y30), gridPaint)

                    canvas.nativeCanvas.drawText("70", chartWidth + 10f, y70 + 8f, textPaint)
                    canvas.nativeCanvas.drawText("30", chartWidth + 10f, y30 + 8f, textPaint)
                    canvas.nativeCanvas.drawText("RSI (14)", 8f, rsiTop + 24f, badgeTextPaint)

                    // Draw RSI Curve
                    rsiPath.reset()
                    var isFirstRsi = true
                    visibleCandles.forEachIndexed { index, _ ->
                        val gIdx = visibleStartIndex + index
                        val rsiVal = rsiData.getOrNull(gIdx)
                        if (rsiVal != null) {
                            val x = index * candleSpacing + (candleSpacing / 2f)
                            val y = rsiTop + rsiHeight * (1f - (rsiVal / 100f).coerceIn(0f, 1f))
                            if (isFirstRsi) { rsiPath.moveTo(x, y); isFirstRsi = false } else { rsiPath.lineTo(x, y) }
                        }
                    }
                    canvas.drawPath(rsiPath, rsiPaint)
                }

                // --- 6. Time axis ---
                canvas.drawLine(Offset(0f, contentBottom), Offset(chartWidth, contentBottom), gridPaint)
                canvas.nativeCanvas.drawText(timeLabels[0], 8f, height - 7f, textPaint)
                canvas.nativeCanvas.drawText(timeLabels[1], chartWidth * 0.48f, height - 7f, textPaint)
                val lastLabelWidth = textPaint.measureText(timeLabels[2])
                canvas.nativeCanvas.drawText(timeLabels[2], chartWidth - lastLabelWidth - 8f, height - 7f, textPaint)

                // --- 7. Selected candle crosshair and price badge ---
                hoveredIndexInVisible?.let { idx ->
                    if (idx in visibleCandles.indices) {
                        val c = visibleCandles[idx]
                        val x = idx * candleSpacing + (candleSpacing / 2f)
                        val yClose = priceHeight - ((c.close - minPrice) / priceRange * priceHeight)

                        // Vertical bar line
                        canvas.drawLine(
                            p1 = Offset(x, 0f),
                            p2 = Offset(x, contentBottom),
                            paint = crosshairPaint
                        )

                        // Horizontal price line
                        canvas.drawLine(
                            p1 = Offset(0f, yClose),
                            p2 = Offset(chartWidth, yClose),
                            paint = crosshairPaint
                        )

                        // Right axis price badge
                        val badgeHeight = 36f
                        val badgeWidth = priceLabelWidth - 8f
                        val badgeTop = (yClose - badgeHeight / 2f).coerceIn(4f, (priceHeight - badgeHeight - 4f).coerceAtLeast(4f))

                        canvas.drawRect(
                            left = chartWidth + 4f,
                            top = badgeTop,
                            right = chartWidth + 4f + badgeWidth,
                            bottom = badgeTop + badgeHeight,
                            paint = badgeBgPaint
                        )

                        canvas.nativeCanvas.drawText(
                            formatPrice(c.close.toDouble()),
                            chartWidth + 12f,
                            badgeTop + 26f,
                            badgeTextPaint
                        )
                    }
                }
            }
        }

        // The selected candle readout wraps across rows on a phone screen.
        if (hoveredCandle != null) {
            val dateStr = remember(hoveredCandle.timestamp) {
                SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(hoveredCandle.timestamp))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Vol ${formatVolume(hoveredCandle.volume)}" +
                                (if (showRsi && hoveredRsi != null) "  ·  RSI ${String.format(Locale.US, "%.1f", hoveredRsi)}" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "O ${formatPrice(hoveredCandle.open.toDouble())}",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "H ${formatPrice(hoveredCandle.high.toDouble())}",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "L ${formatPrice(hoveredCandle.low.toDouble())}",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        Text(
                            text = "C ${formatPrice(hoveredCandle.close.toDouble())}",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (hoveredCandle.close >= hoveredCandle.open) bullColor else bearColor
                            )
                        )
                    }
                }
            }
        }
    }
}
