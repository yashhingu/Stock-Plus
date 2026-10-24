package com.yashh.stockpulse.core.network.api

import com.yashh.stockpulse.core.network.api.model.CandleResponse
import com.yashh.stockpulse.core.network.api.model.QuoteResponse
import com.yashh.stockpulse.core.network.api.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FinnhubApi {
    @GET("quote")
    suspend fun getQuote(@Query("symbol") symbol: String): QuoteResponse

    @GET("stock/candle")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): CandleResponse

    @GET("search")
    suspend fun searchSymbol(@Query("q") query: String): SearchResponse
}
