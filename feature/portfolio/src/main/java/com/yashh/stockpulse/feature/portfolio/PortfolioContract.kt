package com.yashh.stockpulse.feature.portfolio

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PortfolioHolding(
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double
) {
    val totalValue: Double get() = quantity * currentPrice
    val totalCost: Double get() = quantity * averagePrice
    val pnl: Double get() = totalValue - totalCost
}

typealias UiText = String

data class PortfolioState(
    val isLoading: Boolean = true,
    val holdings: ImmutableList<PortfolioHolding> = persistentListOf(),
    val totalValue: Double = 0.0,
    val stockEquity: Double = 0.0,
    val availableCash: Double = 50000.0,
    val totalInvested: Double = 0.0,
    val totalPnL: Double = 0.0,
    val totalPnLPercent: Double = 0.0,
    val dailyChange: Double = 0.0,
    val error: UiText? = null
)

sealed interface PortfolioIntent {
    data object LoadPortfolio : PortfolioIntent
    data object RefreshPrices : PortfolioIntent
    data class AddHolding(val symbol: String, val qty: Double, val price: Double) : PortfolioIntent
    data class RemoveHolding(val symbol: String) : PortfolioIntent
}
