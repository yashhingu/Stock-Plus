package com.yashh.stockpulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yashh.stockpulse.core.model.WatchlistItem

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val companyName: String,
    val addedAt: Long,
    val sortOrder: Int
) {
    fun toModel(): WatchlistItem {
        return WatchlistItem(
            symbol = symbol,
            companyName = companyName,
            addedAt = addedAt,
            sortOrder = sortOrder
        )
    }

    companion object {
        fun fromModel(model: WatchlistItem) = WatchlistEntity(
            symbol = model.symbol,
            companyName = model.companyName,
            addedAt = model.addedAt,
            sortOrder = model.sortOrder
        )
    }
}
