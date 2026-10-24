package com.yashh.stockpulse.core.model

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Bundled, deterministic prices used when the app is presented without a market-data service. */
object DemoMarketData {
    private const val BASE_TIME = 1_760_000_000_000L

    val quotes: List<StockQuote> = listOf(
        quote("AAPL", "Apple Inc.", 187.42, 2.31),
        quote("GOOGL", "Alphabet Inc.", 164.18, -1.24),
        quote("MSFT", "Microsoft Corp.", 421.66, 4.85),
        quote("AMZN", "Amazon.com Inc.", 192.35, 1.72),
        quote("TSLA", "Tesla Inc.", 248.90, -6.34),
        quote("NVDA", "NVIDIA Corp.", 129.76, 3.48),
        quote("META", "Meta Platforms Inc.", 574.12, -2.18),
        quote("NFLX", "Netflix Inc.", 692.44, 5.61)
    )

    fun quote(symbol: String): StockQuote? = quotes.firstOrNull { it.symbol == symbol }

    fun candles(symbol: String, timeFrame: TimeFrame): List<CandleData> {
        val base = quote(symbol)?.currentPrice?.toFloat() ?: return emptyList()
        val stepMinutes = when (timeFrame) {
            TimeFrame.ONE_DAY -> 5L
            TimeFrame.ONE_WEEK -> 120L
            TimeFrame.ONE_MONTH -> 480L
            TimeFrame.THREE_MONTHS -> 1_440L
            TimeFrame.ONE_YEAR -> 5_760L
            TimeFrame.ALL -> 17_280L
        }
        val count = 90
        val swing = base * 0.015f
        var previousClose = base - swing
        return List(count) { index ->
            val open = previousClose
            val trend = (index - count + 1) * base * 0.0003f
            val wave = sin(index * 0.28).toFloat() * swing + cos(index * 0.11).toFloat() * swing * 0.5f
            val close = if (index == count - 1) base else base + trend + wave
            val wick = base * (0.002f + (index % 5) * 0.0004f)
            previousClose = close
            CandleData(
                timestamp = BASE_TIME - (count - 1 - index) * stepMinutes * 60_000L,
                open = open,
                high = max(open, close) + wick,
                low = min(open, close) - wick,
                close = close,
                volume = 1_000L + index * 37L
            )
        }
    }

    private fun quote(symbol: String, name: String, price: Double, change: Double): StockQuote {
        val previousClose = price - change
        return StockQuote(
            symbol = symbol,
            companyName = name,
            currentPrice = price,
            change = change,
            changePercent = change / previousClose * 100,
            high = price + 2.4,
            low = price - 3.1,
            open = previousClose + 0.8,
            previousClose = previousClose,
            volume = 1_200_000L,
            timestamp = BASE_TIME
        )
    }
}
