package com.yashh.stockpulse.feature.stockdetail

import com.yashh.stockpulse.core.model.CandleData
import com.yashh.stockpulse.core.model.OrderBook
import com.yashh.stockpulse.core.model.StockQuote
import com.yashh.stockpulse.core.model.TimeFrame
import com.yashh.stockpulse.core.model.UiText
import kotlinx.collections.immutable.ImmutableList

/**
 * Model-View-Intent (MVI) contract definitions for Stock Detail screen.
 */
sealed interface StockDetailState {
    data object Loading : StockDetailState

    data class Connected(
        val quote: StockQuote,
        val candles: ImmutableList<CandleData>,
        val selectedTimeFrame: TimeFrame = TimeFrame.ONE_DAY,
        val showEma: Boolean = false,
        val showSma: Boolean = true,
        val showVolume: Boolean = true,
        val showRsi: Boolean = false,
        val isInWatchlist: Boolean = false,
        val orderBook: OrderBook? = null,
        val availableCash: Double = 50000.0,
        val heldQuantity: Double = 0.0,
        val orderNotification: String? = null
    ) : StockDetailState

    data object Reconnecting : StockDetailState

    data class Error(val error: UiText) : StockDetailState
}

sealed interface StockDetailIntent {
    data class SelectTimeFrame(val frame: TimeFrame) : StockDetailIntent
    data class ToggleIndicator(val ema: Boolean) : StockDetailIntent
    data object ToggleRsi : StockDetailIntent
    data object ToggleVolume : StockDetailIntent
    data class ToggleWatchlist(val symbol: String) : StockDetailIntent
    data class ExecuteOrder(val isBuy: Boolean, val quantity: Double, val price: Double) : StockDetailIntent
    data object DismissOrderNotification : StockDetailIntent
    data object RetryConnection : StockDetailIntent
}
