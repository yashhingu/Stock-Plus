package com.yashh.stockpulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yashh.stockpulse.core.model.PortfolioHolding

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey val symbol: String,
    val companyName: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double,
    val lastUpdated: Long
) {
    fun toModel() = PortfolioHolding(
        symbol = symbol,
        companyName = companyName,
        quantity = quantity,
        avgBuyPrice = avgBuyPrice,
        currentPrice = currentPrice
    )

    companion object {
        fun fromModel(model: PortfolioHolding) = PortfolioEntity(
            symbol = model.symbol,
            companyName = model.companyName,
            quantity = model.quantity,
            avgBuyPrice = model.avgBuyPrice,
            currentPrice = model.currentPrice,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
