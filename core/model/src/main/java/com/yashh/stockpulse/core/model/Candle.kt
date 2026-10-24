package com.yashh.stockpulse.core.model

// use float instead of double to save memory during heavy charting
data class CandleData(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Long
) {
    val isBullish: Boolean get() = close >= open
    val bodyHeight: Float get() = kotlin.math.abs(close - open)
}
