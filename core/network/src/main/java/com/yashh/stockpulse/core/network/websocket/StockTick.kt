package com.yashh.stockpulse.core.network.websocket

data class StockTick(
    val symbol: String,
    val price: Double,
    val volume: Double,
    val timestamp: Long
)
