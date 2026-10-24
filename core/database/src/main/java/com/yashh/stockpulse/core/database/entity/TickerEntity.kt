package com.yashh.stockpulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yashh.stockpulse.core.model.StockQuote

@Entity(tableName = "tickers")
data class TickerEntity(
    @PrimaryKey
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
    val lastUpdated: Long
) {
    fun toModel() = StockQuote(
        symbol = symbol,
        companyName = companyName,
        currentPrice = currentPrice,
        change = change,
        changePercent = changePercent,
        high = high,
        low = low,
        open = open,
        previousClose = previousClose,
        volume = volume,
        timestamp = lastUpdated
    )

    companion object {
        fun fromModel(model: StockQuote): TickerEntity {
            return TickerEntity(
                symbol = model.symbol,
                companyName = model.companyName,
                currentPrice = model.currentPrice,
                change = model.change,
                changePercent = model.changePercent,
                high = model.high,
                low = model.low,
                open = model.open,
                previousClose = model.previousClose,
                volume = model.volume,
                lastUpdated = model.timestamp
            )
        }
    }
}
