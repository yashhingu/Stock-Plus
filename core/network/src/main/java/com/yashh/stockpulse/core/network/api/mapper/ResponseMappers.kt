package com.yashh.stockpulse.core.network.api.mapper

import com.yashh.stockpulse.core.model.CandleData
import com.yashh.stockpulse.core.model.StockQuote
import com.yashh.stockpulse.core.network.api.model.CandleResponse
import com.yashh.stockpulse.core.network.api.model.QuoteResponse

// TODO: properly handle nulls if api acts up

fun QuoteResponse.toDomain(symbol: String): StockQuote {
    return StockQuote(
        symbol = symbol,
        companyName = symbol,
        currentPrice = this.c,
        change = this.d,
        changePercent = this.dp,
        high = this.h,
        low = this.l,
        open = this.o,
        previousClose = this.pc,
        volume = 0L,
        timestamp = this.t
    )
}

fun CandleResponse.toDomain(): List<CandleData> {
    // If response is empty or status is not ok, return empty list
    if (this.s != "ok" || this.t == null) return emptyList()

    val size = this.t.size
    val result = mutableListOf<CandleData>()

    for (i in 0 until size) {
        result.add(
            CandleData(
                timestamp = this.t[i],
                open = this.o?.getOrNull(i)?.toFloat() ?: 0f,
                high = this.h?.getOrNull(i)?.toFloat() ?: 0f,
                low = this.l?.getOrNull(i)?.toFloat() ?: 0f,
                close = this.c?.getOrNull(i)?.toFloat() ?: 0f,
                volume = this.v?.getOrNull(i) ?: 0L
            )
        )
    }

    return result
}
