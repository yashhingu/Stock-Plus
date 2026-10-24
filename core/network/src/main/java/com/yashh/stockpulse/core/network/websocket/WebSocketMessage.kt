package com.yashh.stockpulse.core.network.websocket

/**
 * Represents the various states and messages that can be emitted by the [StockWebSocketClient].
 */
sealed class WebSocketMessage {
    /** Indicates that the WebSocket connection has been successfully established. */
    object Connected : WebSocketMessage()

    /**
     * Represents a raw text message received from the server.
     * @property text The raw JSON string received.
     */
    data class MessageReceived(val text: String) : WebSocketMessage()

    /**
     * Indicates that the WebSocket connection has been closed.
     * @property code The closure code provided by the server or client.
     * @property reason A string detailing the reason for disconnection.
     */
    data class Disconnected(val code: Int = 1000, val reason: String = "Normal closure") : WebSocketMessage()

    /**
     * Indicates that an error occurred on the WebSocket connection.
     * @property throwable The underlying exception that caused the error.
     */
    data class Error(val throwable: Throwable) : WebSocketMessage() {
        constructor(message: String) : this(Exception(message))
    }

    /**
     * Real-time trade tick emitted from stream.
     */
    data class Tick(
        val symbol: String,
        val price: Double,
        val timestamp: Long,
        val volume: Double = 0.0
    ) : WebSocketMessage()

    /**
     * Real-time order book update.
     */
    data class OrderBookUpdate(
        val symbol: String,
        val orderBook: com.yashh.stockpulse.core.model.OrderBook
    ) : WebSocketMessage()
}
