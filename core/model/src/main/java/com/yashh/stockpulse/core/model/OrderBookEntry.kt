package com.yashh.stockpulse.core.model

enum class OrderSide { BID, ASK }

data class OrderBookEntry(
    val price: Double,
    val quantity: Double,
    val side: OrderSide
)
