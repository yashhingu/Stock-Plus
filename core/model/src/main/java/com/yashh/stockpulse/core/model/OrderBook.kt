package com.yashh.stockpulse.core.model

data class OrderBook(
    val symbol: String,
    val bids: List<OrderBookEntry>,
    val asks: List<OrderBookEntry>,
    val lastUpdated: Long
) {
    val spread: Double
        get() = if (bids.isNotEmpty() && asks.isNotEmpty()) asks.first().price - bids.first().price else 0.0
}
