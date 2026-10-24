package com.yashh.stockpulse.core.network.websocket

import com.yashh.stockpulse.core.model.DemoMarketData
import com.yashh.stockpulse.core.network.NetworkConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.sin

/**
 * A robust WebSocket connection manager for handling real-time stock ticks.
 *
 * This client manages the lifecycle of a WebSocket connection to the Finnhub API,
 * including automatic reconnection with exponential backoff, subscription management,
 * and robust error handling.
 *
 * @param okHttpClient The OkHttpClient instance to use for creating the WebSocket connection.
 */
@Singleton
class StockWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    // --- State Management ---

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var isOpen = false
    private val connectionGeneration = AtomicInteger(0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var reconnectJob: Job? = null
    private var demoJob: Job? = null

    private val shouldReconnect = AtomicBoolean(false)
    private var reconnectAttempt = 0
    private val subscribedSymbols = mutableSetOf<String>()

    private val _messageFlow = MutableSharedFlow<WebSocketMessage>(extraBufferCapacity = 64)
    /**
     * Flow of all raw or connection-level WebSocket events.
     */
    val messageFlow: SharedFlow<WebSocketMessage> = _messageFlow.asSharedFlow()
    /** Alias for messageFlow for convenience */
    val messages: SharedFlow<WebSocketMessage> get() = messageFlow

    private val _tickFlow = MutableSharedFlow<StockTick>(extraBufferCapacity = 64)
    /**
     * Flow of parsed real-time stock ticks.
     */
    val tickFlow: SharedFlow<StockTick> = _tickFlow.asSharedFlow()

    // --- Connection Management ---

    /**
     * Connects to the WebSocket server and subscribes to a single symbol.
     */
    fun connect(symbol: String) {
        connect(listOf(symbol))
    }

    /**
     * Connects to the WebSocket server and subscribes to the provided symbols.
     * If already connected or attempting to connect, this call will merge new symbols
     * and subscribe to them if the connection is active.
     *
     * @param symbols A list of stock symbols to subscribe to upon successful connection.
     */
    fun connect(symbols: List<String> = emptyList()) {
        synchronized(subscribedSymbols) { subscribedSymbols.addAll(symbols) }
        shouldReconnect.set(true)
        if (NetworkConstants.DEMO_MODE) {
            startDemoStream()
            return
        }

        if (webSocket != null) {
            Timber.d("WebSocket already connected or connecting. Subscribing to new symbols if active.")
            if (isOpen) symbols.forEach { subscribe(it) }
            return
        }

        establishConnection()
    }

    private fun startDemoStream() {
        if (demoJob?.isActive == true) return
        isOpen = true
        demoJob = scope.launch {
            _messageFlow.emit(WebSocketMessage.Connected)
            var step = 0L
            while (shouldReconnect.get()) {
                delay(1_500)
                val symbols = synchronized(subscribedSymbols) { subscribedSymbols.toList() }
                symbols.forEach { symbol ->
                    val base = DemoMarketData.quote(symbol) ?: return@forEach
                    val price = base.currentPrice + sin(step * 0.35 + symbol.length) * base.currentPrice * 0.002
                    val tick = StockTick(symbol, price, 100.0, System.currentTimeMillis())
                    _tickFlow.emit(tick)
                    _messageFlow.tryEmit(WebSocketMessage.Tick(symbol, price, tick.timestamp, tick.volume))
                }
                step++
            }
        }
    }

    private fun establishConnection() {
        Timber.i("Establishing WebSocket connection...")
        val generation = connectionGeneration.incrementAndGet()
        val request = Request.Builder()
            .url(com.yashh.stockpulse.core.network.NetworkConstants.WS_URL + com.yashh.stockpulse.core.network.NetworkConstants.API_KEY)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!shouldReconnect.get() || connectionGeneration.get() != generation) {
                    webSocket.close(1000, "No active subscribers")
                    return
                }
                isOpen = true
                Timber.i("WebSocket Connected")
                reconnectAttempt = 0
                _messageFlow.tryEmit(WebSocketMessage.Connected)

                // Resubscribe to all tracked symbols upon successful connection
                synchronized(subscribedSymbols) {
                    subscribedSymbols.forEach { symbol ->
                        sendSubscriptionMessage(webSocket, symbol, "subscribe")
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    _messageFlow.tryEmit(WebSocketMessage.MessageReceived(text))
                    parseIncomingMessage(text)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.w("WebSocket Closed: Code=$code, Reason=$reason")
                if (connectionGeneration.get() == generation) {
                    _messageFlow.tryEmit(WebSocketMessage.Disconnected(code, reason))
                    handleDisconnection()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket Failure")
                if (connectionGeneration.get() == generation) {
                    _messageFlow.tryEmit(WebSocketMessage.Error(t))
                    handleDisconnection()
                }
            }
        })
    }

    /**
     * Disconnects the WebSocket and prevents any further automatic reconnection attempts.
     */
    fun disconnect() {
        Timber.i("Disconnecting WebSocket...")
        shouldReconnect.set(false)
        connectionGeneration.incrementAndGet()
        isOpen = false
        reconnectJob?.cancel()
        demoJob?.cancel()
        demoJob = null
        synchronized(subscribedSymbols) { subscribedSymbols.clear() }
        webSocket?.close(1000, "Client initiated disconnect")
        webSocket = null
    }

    // --- Subscription Management ---

    /**
     * Subscribes to real-time trades for a specific symbol.
     *
     * @param symbol The stock ticker symbol.
     */
    fun subscribe(symbol: String) {
        synchronized(subscribedSymbols) {
            subscribedSymbols.add(symbol)
        }
        if (isOpen) webSocket?.let { sendSubscriptionMessage(it, symbol, "subscribe") }
    }

    /**
     * Unsubscribes from real-time trades for a specific symbol.
     *
     * @param symbol The stock ticker symbol.
     */
    fun unsubscribe(symbol: String) {
        synchronized(subscribedSymbols) {
            subscribedSymbols.remove(symbol)
        }
        if (isOpen) webSocket?.let { sendSubscriptionMessage(it, symbol, "unsubscribe") }
    }

    private fun sendSubscriptionMessage(ws: WebSocket, symbol: String, type: String) {
        val json = """{"type":"$type","symbol":"$symbol"}"""
        val sent = ws.send(json)
        if (sent) {
            Timber.d("Sent $type for $symbol")
        } else {
            Timber.w("Failed to send $type for $symbol")
        }
    }

    // --- Backoff Logic ---

    private fun handleDisconnection() {
        webSocket = null
        isOpen = false
        if (!shouldReconnect.get()) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val backoffSeconds = calculateBackoffSeconds(reconnectAttempt)
            Timber.i("Attempting to reconnect in $backoffSeconds seconds (Attempt ${reconnectAttempt + 1})")
            delay(backoffSeconds * 1000L)
            if (!shouldReconnect.get()) return@launch
            reconnectAttempt++
            establishConnection()
        }
    }

    /**
     * Calculates the exponential backoff delay.
     * Initial delay: 1s, Max delay: 30s, Factor: 2.0
     */
    private fun calculateBackoffSeconds(attempt: Int): Long {
        return min(30L, 1L shl attempt.coerceIn(0, 5))
    }

    // --- Parsing Logic ---

    private fun parseIncomingMessage(jsonText: String) {
        try {
            val jsonObject = JSONObject(jsonText)
            if (jsonObject.optString("type") == "trade") {
                val dataArray = jsonObject.optJSONArray("data") ?: return
                for (i in 0 until dataArray.length()) {
                    val trade = dataArray.getJSONObject(i)
                    val price = trade.optDouble("p")
                    val symbol = trade.optString("s")
                    val timestamp = trade.optLong("t")
                    val volume = trade.optDouble("v")

                    val tick = StockTick(symbol = symbol, price = price, volume = volume, timestamp = timestamp)
                    _tickFlow.tryEmit(tick)
                    _messageFlow.tryEmit(WebSocketMessage.Tick(symbol, price, timestamp, volume))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing WebSocket message")
        }
    }
}
