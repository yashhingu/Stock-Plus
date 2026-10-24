package com.yashh.stockpulse.core.network

object NetworkConstants {
    const val DEMO_MODE = true
    const val BASE_URL = "https://finnhub.io/api/v1/"
    const val WS_URL = "wss://ws.finnhub.io?token="
    const val TIMEOUT_SEC = 30L

    val API_KEY: String get() = BuildConfig.FINNHUB_API_KEY
}
