package com.yashh.stockpulse.core.database.entity

import androidx.room.Entity
import com.yashh.stockpulse.core.model.CandleData

@Entity(
    tableName = "candles",
    primaryKeys = ["symbol", "timestamp"]
)
data class CandleEntity(
    val symbol: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val timeFrame: String
) {
    fun toModel() = CandleData(
        timestamp = timestamp,
        open = open.toFloat(),
        high = high.toFloat(),
        low = low.toFloat(),
        close = close.toFloat(),
        volume = volume
    )

    // mapper from model
    companion object {
        fun fromModel(symbol: String, timeFrame: String, model: CandleData) = CandleEntity(
            symbol = symbol,
            timestamp = model.timestamp,
            open = model.open.toDouble(),
            high = model.high.toDouble(),
            low = model.low.toDouble(),
            close = model.close.toDouble(),
            volume = model.volume,
            timeFrame = timeFrame
        )
    }
}
