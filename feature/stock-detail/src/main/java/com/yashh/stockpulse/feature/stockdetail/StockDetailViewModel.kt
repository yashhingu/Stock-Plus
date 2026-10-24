package com.yashh.stockpulse.feature.stockdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yashh.stockpulse.core.database.dao.AccountDao
import com.yashh.stockpulse.core.database.dao.CandleDao
import com.yashh.stockpulse.core.database.dao.PortfolioDao
import com.yashh.stockpulse.core.database.dao.WatchlistDao
import com.yashh.stockpulse.core.database.entity.PortfolioEntity
import com.yashh.stockpulse.core.model.CandleData
import com.yashh.stockpulse.core.model.TimeFrame
import com.yashh.stockpulse.core.model.UiText
import com.yashh.stockpulse.core.network.websocket.WebSocketMessage
import com.yashh.stockpulse.core.network.websocket.StockTick
import com.yashh.stockpulse.core.network.websocket.StockWebSocketClient
import com.yashh.stockpulse.feature.stockdetail.chart.formatPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing the Stock Detail screen state, real-time WebSocket tick ingestion,
 * technical indicators, and paper trading order execution.
 *
 * MVI architecture with reactive StateFlow emissions and conflated market tick buffering.
 */
@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val webSocketClient: StockWebSocketClient,
    private val repository: StockDetailRepository,
    private val candleDao: CandleDao,
    private val watchlistDao: WatchlistDao,
    private val accountDao: AccountDao?,
    private val portfolioDao: PortfolioDao?
) : ViewModel() {

    // Secondary constructor for unit testing convenience
    constructor(
        savedStateHandle: SavedStateHandle,
        webSocketClient: StockWebSocketClient,
        repository: StockDetailRepository,
        candleDao: CandleDao,
        watchlistDao: WatchlistDao
    ) : this(
        savedStateHandle = savedStateHandle,
        webSocketClient = webSocketClient,
        repository = repository,
        candleDao = candleDao,
        watchlistDao = watchlistDao,
        accountDao = null,
        portfolioDao = null
    )

    private val symbol: String = checkNotNull(savedStateHandle["symbol"]) {
        "Symbol must be provided"
    }

    private val _state = MutableStateFlow<StockDetailState>(StockDetailState.Loading)

    /**
     * Exposes the current [StockDetailState] to the UI.
     */
    val state: StateFlow<StockDetailState> = _state.asStateFlow()
    private var selectedTimeFrame = TimeFrame.ONE_DAY
    private var messageJob: Job? = null
    private var tickJob: Job? = null

    init {
        initializeData()
        setupWebSocketConnection()
        observeAccountAndPortfolio()
    }

    // ========================================================================
    // Initialization & Reactive Observers
    // ========================================================================

    private fun initializeData() {
        viewModelScope.launch {
            loadHistoricalData(selectedTimeFrame)
        }
    }

    private fun observeAccountAndPortfolio() {
        accountDao?.getAccount()
            ?.onEach { account ->
                val cash = account?.cashBalance ?: 50000.0
                _state.update { current ->
                    if (current is StockDetailState.Connected) current.copy(availableCash = cash) else current
                }
            }
            ?.launchIn(viewModelScope)

        portfolioDao?.getHolding(symbol)
            ?.onEach { holding ->
                val qty = holding?.quantity ?: 0.0
                _state.update { current ->
                    if (current is StockDetailState.Connected) current.copy(heldQuantity = qty) else current
                }
            }
            ?.launchIn(viewModelScope)
    }

    /**
     * Configures the WebSocket connection for real-time stock ticks.
     * Buffers the incoming flow to conflate rapid updates, ensuring the UI
     * rendering thread isn't overwhelmed by high-frequency market data.
     */
    private fun setupWebSocketConnection() {
        messageJob?.cancel()
        tickJob?.cancel()
        webSocketClient.connect(symbol)

        messageJob = webSocketClient.messages
            .filterNot { it is WebSocketMessage.Tick || it is WebSocketMessage.MessageReceived }
            .onEach { message ->
                handleWebSocketMessage(message)
            }
            .catch { error ->
                _state.update {
                    StockDetailState.Error(UiText.DynamicString(error.message ?: "Unknown error"))
                }
            }
            .launchIn(viewModelScope)

        tickJob = webSocketClient.tickFlow
            .buffer(capacity = kotlinx.coroutines.channels.Channel.CONFLATED)
            .onEach { tick -> handleTick(tick) }
            .launchIn(viewModelScope)
    }

    private fun handleTick(tick: StockTick) {
        if (tick.symbol != symbol) return
        _state.update { current ->
            if (current !is StockDetailState.Connected) return@update current
            val change = tick.price - current.quote.previousClose
            current.copy(quote = current.quote.copy(
                currentPrice = tick.price,
                change = change,
                changePercent = if (current.quote.previousClose != 0.0) change / current.quote.previousClose * 100 else 0.0,
                timestamp = tick.timestamp
            ))
        }
    }

    private fun handleWebSocketMessage(message: WebSocketMessage) {
        when (message) {
            is WebSocketMessage.Connected -> {
                if (_state.value is StockDetailState.Reconnecting) {
                    viewModelScope.launch { loadHistoricalData(selectedTimeFrame) }
                }
            }
            is WebSocketMessage.Disconnected -> {
                _state.update { StockDetailState.Reconnecting }
            }
            is WebSocketMessage.Error -> {
                _state.update { StockDetailState.Error(UiText.DynamicString("Connection lost")) }
            }
            is WebSocketMessage.Tick -> Unit

            is WebSocketMessage.OrderBookUpdate -> {
                if (message.symbol != symbol) return
                val currentState = _state.value
                if (currentState is StockDetailState.Connected) {
                    _state.update {
                        currentState.copy(orderBook = message.orderBook)
                    }
                }
            }
            is WebSocketMessage.MessageReceived -> Unit
        }
    }

    // ========================================================================
    // Intent Processing
    // ========================================================================

    /**
     * Processes incoming user intents and executes the corresponding business logic.
     *
     * @param intent The [StockDetailIntent] representing the user action.
     */
    fun processIntent(intent: StockDetailIntent) {
        when (intent) {
            is StockDetailIntent.SelectTimeFrame -> handleSelectTimeFrame(intent.frame)
            is StockDetailIntent.ToggleIndicator -> handleToggleIndicator(intent.ema)
            is StockDetailIntent.ToggleRsi -> handleToggleRsi()
            is StockDetailIntent.ToggleVolume -> handleToggleVolume()
            is StockDetailIntent.ToggleWatchlist -> handleToggleWatchlist(intent.symbol)
            is StockDetailIntent.ExecuteOrder -> handleExecuteOrder(intent.isBuy, intent.quantity, intent.price)
            is StockDetailIntent.DismissOrderNotification -> handleDismissOrderNotification()
            is StockDetailIntent.RetryConnection -> handleRetryConnection()
        }
    }

    private fun handleSelectTimeFrame(frame: TimeFrame) {
        val currentState = _state.value
        if (currentState is StockDetailState.Connected) {
            selectedTimeFrame = frame
            _state.update { currentState.copy(selectedTimeFrame = frame) }
            viewModelScope.launch {
                loadHistoricalData(frame)
            }
        }
    }

    private fun handleToggleIndicator(ema: Boolean) {
        val currentState = _state.value
        if (currentState is StockDetailState.Connected) {
            if (ema) {
                _state.update { currentState.copy(showEma = !currentState.showEma) }
            } else {
                _state.update { currentState.copy(showSma = !currentState.showSma) }
            }
        }
    }

    private fun handleToggleRsi() {
        val currentState = _state.value
        if (currentState is StockDetailState.Connected) {
            _state.update { currentState.copy(showRsi = !currentState.showRsi) }
        }
    }

    private fun handleToggleVolume() {
        val currentState = _state.value
        if (currentState is StockDetailState.Connected) {
            _state.update { currentState.copy(showVolume = !currentState.showVolume) }
        }
    }

    private fun handleDismissOrderNotification() {
        val currentState = _state.value
        if (currentState is StockDetailState.Connected) {
            _state.update { currentState.copy(orderNotification = null) }
        }
    }

    private fun handleExecuteOrder(isBuy: Boolean, quantity: Double, price: Double) {
        if (quantity <= 0.0 || price <= 0.0) return
        val currentState = _state.value as? StockDetailState.Connected ?: return

        viewModelScope.launch {
            if (isBuy) {
                val totalCost = quantity * price
                val cash = accountDao?.getCashBalanceNow() ?: currentState.availableCash
                if (cash < totalCost) return@launch

                accountDao?.updateCash(-totalCost)
                val currentHolding = portfolioDao?.getHolding(symbol)?.firstOrNull()
                val prevQty = currentHolding?.quantity ?: 0.0
                val prevAvg = currentHolding?.avgBuyPrice ?: 0.0
                val newQty = prevQty + quantity
                val newAvgPrice = if (newQty > 0) ((prevQty * prevAvg) + totalCost) / newQty else price

                portfolioDao?.insertHolding(
                    PortfolioEntity(
                        symbol = symbol,
                        companyName = currentState.quote.companyName.ifBlank { symbol },
                        quantity = newQty,
                        avgBuyPrice = newAvgPrice,
                        currentPrice = price,
                        lastUpdated = System.currentTimeMillis()
                    )
                )

                _state.update {
                    currentState.copy(
                        availableCash = (cash - totalCost).coerceAtLeast(0.0),
                        heldQuantity = newQty,
                        orderNotification = "Order Filled: Bought ${quantity.toInt()} $symbol @ $${formatPrice(price)}"
                    )
                }
            } else {
                val currentHolding = portfolioDao?.getHolding(symbol)?.firstOrNull()
                val held = currentHolding?.quantity ?: currentState.heldQuantity
                if (held < quantity) return@launch

                val proceeds = quantity * price
                accountDao?.updateCash(proceeds)
                val remainingQty = held - quantity

                if (remainingQty <= 0.0001) {
                    portfolioDao?.deleteHolding(symbol)
                } else {
                    portfolioDao?.insertHolding(
                        currentHolding!!.copy(
                            quantity = remainingQty,
                            currentPrice = price,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }

                val updatedCash = accountDao?.getCashBalanceNow() ?: (currentState.availableCash + proceeds)
                _state.update {
                    currentState.copy(
                        availableCash = updatedCash,
                        heldQuantity = remainingQty.coerceAtLeast(0.0),
                        orderNotification = "Order Filled: Sold ${quantity.toInt()} $symbol @ $${formatPrice(price)}"
                    )
                }
            }
        }
    }

    private fun handleToggleWatchlist(symbolToToggle: String) {
        val currentState = _state.value
        if (currentState is StockDetailState.Connected) {
            viewModelScope.launch {
                val isCurrentlyInWatchlist = currentState.isInWatchlist
                if (isCurrentlyInWatchlist) {
                    watchlistDao.removeFromWatchlist(symbolToToggle)
                } else {
                    watchlistDao.addToWatchlist(symbolToToggle)
                }
                _state.update { currentState.copy(isInWatchlist = !isCurrentlyInWatchlist) }
            }
        }
    }

    private fun handleRetryConnection() {
        _state.update { StockDetailState.Loading }
        webSocketClient.disconnect()
        setupWebSocketConnection()
        initializeData()
    }

    // ========================================================================
    // Data Loading
    // ========================================================================

    private suspend fun loadHistoricalData(timeFrame: TimeFrame) {
        try {
            repository.getHistoricalCandles(symbol, timeFrame).collect { candlesList ->
                val isInWatchlist = watchlistDao.isSymbolInWatchlist(symbol)
                val cash = accountDao?.getCashBalanceNow() ?: 50000.0
                val holding = portfolioDao?.getHolding(symbol)?.firstOrNull()

                val currentState = _state.value
                if (currentState is StockDetailState.Connected) {
                    _state.update {
                        currentState.copy(
                            candles = candlesList.toImmutableList(),
                            selectedTimeFrame = timeFrame,
                            isInWatchlist = isInWatchlist,
                            availableCash = cash,
                            heldQuantity = holding?.quantity ?: 0.0
                        )
                    }
                } else {
                    val quote = repository.getInitialQuote(symbol)
                    _state.update {
                        StockDetailState.Connected(
                            quote = quote,
                            candles = candlesList.toImmutableList(),
                            selectedTimeFrame = timeFrame,
                            isInWatchlist = isInWatchlist,
                            availableCash = cash,
                            heldQuantity = holding?.quantity ?: 0.0
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _state.update {
                StockDetailState.Error(UiText.DynamicString(e.message ?: "Failed to load data"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketClient.disconnect()
    }
}
