package com.yashh.stockpulse.core.model

data class PortfolioHolding(
    val symbol: String,
    val companyName: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double
) {
    // TODO: add dividend tracking later

    val totalValue: Double get() = quantity * currentPrice
    val investedValue: Double get() = quantity * avgBuyPrice
    val unrealizedPnL: Double get() = totalValue - investedValue
    val unrealizedPnLPercent: Double get() = if (investedValue > 0) (unrealizedPnL / investedValue) * 100 else 0.0
    val isProfit: Boolean get() = unrealizedPnL >= 0
}
