package com.yashh.stockpulse.core.model

data class StockQuote(
    val symbol: String,
    val companyName: String,
    val currentPrice: Double,
    val change: Double,
    val changePercent: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val previousClose: Double,
    val volume: Long,
    val timestamp: Long
) {
    constructor(symbol: String, currentPrice: Double, timestamp: Long) : this(
        symbol = symbol,
        companyName = symbol,
        currentPrice = currentPrice,
        change = 0.0,
        changePercent = 0.0,
        high = currentPrice,
        low = currentPrice,
        open = currentPrice,
        previousClose = currentPrice,
        volume = 0L,
        timestamp = timestamp
    )

    val isPositive: Boolean get() = change >= 0

    companion object {
        // dummy helper for previews
        fun preview() = StockQuote(
            symbol = "AAPL",
            companyName = "Apple Inc.",
            currentPrice = 150.0,
            change = 1.5,
            changePercent = 1.01,
            high = 152.0,
            low = 148.0,
            open = 149.0,
            previousClose = 148.5,
            volume = 1000000L,
            timestamp = System.currentTimeMillis()
        )
    }
}
