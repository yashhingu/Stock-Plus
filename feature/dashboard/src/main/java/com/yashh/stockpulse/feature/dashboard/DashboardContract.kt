package com.yashh.stockpulse.feature.dashboard

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

// Dummy types for compilation assuming they are in core:model
data class StockQuote(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val chartData: List<Float> = emptyList()
)

enum class MarketStatus { OPEN, CLOSED, PRE_MARKET, AFTER_HOURS }
typealias UiText = String

data class DashboardState(
    val isLoading: Boolean = true,
    val marketStatus: MarketStatus = MarketStatus.CLOSED,
    val topGainers: ImmutableList<StockQuote> = persistentListOf(),
    val topLosers: ImmutableList<StockQuote> = persistentListOf(),
    val watchlist: ImmutableList<StockQuote> = persistentListOf(),
    val tickerTape: ImmutableList<StockQuote> = persistentListOf(),
    val error: UiText? = null
)

sealed interface DashboardIntent {
    data object LoadMarketData : DashboardIntent
    data object RefreshData : DashboardIntent
    data class SearchStock(val query: String) : DashboardIntent
    data class AddToWatchlist(val symbol: String) : DashboardIntent
    data class RemoveFromWatchlist(val symbol: String) : DashboardIntent
}

sealed interface DashboardEffect {
    data class NavigateToDetail(val symbol: String) : DashboardEffect
    data class ShowSnackbar(val message: UiText) : DashboardEffect
}
