package com.yashh.stockpulse.core.model

data class WatchlistItem(
    val symbol: String,
    val companyName: String,
    val addedAt: Long,
    val sortOrder: Int
)
