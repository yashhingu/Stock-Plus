package com.yashh.stockpulse.feature.stockdetail.chart

import java.text.DecimalFormat

/**
 * Calculates the Simple Moving Average (SMA) for a given dataset and period.
 */
fun calculateSMA(data: List<Float>, period: Int): List<Float?> {
    if (data.isEmpty() || period <= 0) return emptyList()
    val result = MutableList<Float?>(data.size) { null }

    var sum = 0f
    for (i in data.indices) {
        sum += data[i]
        if (i >= period) {
            sum -= data[i - period]
            result[i] = sum / period
        } else if (i == period - 1) {
            result[i] = sum / period
        }
    }
    return result
}

/**
 * Calculates the Exponential Moving Average (EMA) for a given dataset and period.
 */
fun calculateEMA(data: List<Float>, period: Int): List<Float?> {
    if (data.isEmpty() || period <= 0) return emptyList()
    val result = MutableList<Float?>(data.size) { null }

    val multiplier = 2f / (period + 1)

    // SMA for the first EMA value
    var sma = 0f
    for (i in 0 until minOf(period, data.size)) {
        sma += data[i]
    }
    sma /= period

    if (data.size >= period) {
        result[period - 1] = sma
        var prevEma = sma

        for (i in period until data.size) {
            val currentEma = (data[i] - prevEma) * multiplier + prevEma
            result[i] = currentEma
            prevEma = currentEma
        }
    }

    return result
}

/**
 * Calculates the Relative Strength Index (RSI) using Wilder's smoothing technique.
 * Default period is 14 bars. Values range from 0.0 to 100.0.
 */
fun calculateRSI(data: List<Float>, period: Int = 14): List<Float?> {
    if (data.size <= period || period <= 0) return List(data.size) { null }
    val result = MutableList<Float?>(data.size) { null }

    var gainSum = 0f
    var lossSum = 0f

    // Calculate initial average gain and loss over first 'period' changes
    for (i in 1..period) {
        val diff = data[i] - data[i - 1]
        if (diff > 0f) gainSum += diff else lossSum += -diff
    }

    var avgGain = gainSum / period
    var avgLoss = lossSum / period

    result[period] = if (avgLoss == 0f) 100f else (100f - (100f / (1f + (avgGain / avgLoss))))

    // Smooth subsequent points
    for (i in (period + 1) until data.size) {
        val diff = data[i] - data[i - 1]
        val gain = if (diff > 0f) diff else 0f
        val loss = if (diff < 0f) -diff else 0f

        avgGain = (avgGain * (period - 1) + gain) / period
        avgLoss = (avgLoss * (period - 1) + loss) / period

        result[i] = if (avgLoss == 0f) 100f else (100f - (100f / (1f + (avgGain / avgLoss))))
    }

    return result
}

private val priceFormatter = DecimalFormat("#,##0.00")
fun formatPrice(price: Double): String {
    return priceFormatter.format(price)
}

fun formatVolume(volume: Long): String {
    return when {
        volume >= 1_000_000_000 -> String.format("%.2fB", volume / 1_000_000_000.0)
        volume >= 1_000_000 -> String.format("%.2fM", volume / 1_000_000.0)
        volume >= 1_000 -> String.format("%.2fK", volume / 1_000.0)
        else -> volume.toString()
    }
}
