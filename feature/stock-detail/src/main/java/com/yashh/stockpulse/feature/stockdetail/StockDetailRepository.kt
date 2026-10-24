package com.yashh.stockpulse.feature.stockdetail

import com.yashh.stockpulse.core.model.CandleData
import com.yashh.stockpulse.core.model.StockQuote
import com.yashh.stockpulse.core.model.TimeFrame
import com.yashh.stockpulse.core.model.DemoMarketData
import com.yashh.stockpulse.core.database.dao.CandleDao
import com.yashh.stockpulse.core.database.dao.TickerDao
import com.yashh.stockpulse.core.database.entity.CandleEntity
import com.yashh.stockpulse.core.database.entity.TickerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface StockDetailRepository {
    fun getHistoricalCandles(symbol: String, timeFrame: TimeFrame): Flow<List<CandleData>>
    suspend fun getInitialQuote(symbol: String): StockQuote
}

class StockDetailRepositoryImpl @Inject constructor(
    private val candleDao: CandleDao,
    private val tickerDao: TickerDao
) : StockDetailRepository {
    override fun getHistoricalCandles(symbol: String, timeFrame: TimeFrame): Flow<List<CandleData>> = flow {
        val cached = candleDao.getCandlesForSymbol(symbol, timeFrame.name).first().map { it.toModel() }
        if (cached.isNotEmpty()) emit(cached)
        val demoCandles = DemoMarketData.candles(symbol, timeFrame)
        if (demoCandles.isNotEmpty()) {
            candleDao.insertCandles(demoCandles.map { CandleEntity.fromModel(symbol, timeFrame.name, it) })
        }
        if (cached.isEmpty() || cached != demoCandles) emit(demoCandles)
    }

    override suspend fun getInitialQuote(symbol: String): StockQuote {
        val cached = tickerDao.getTickerBySymbol(symbol).first()?.toModel()
        val quote = DemoMarketData.quote(symbol) ?: cached ?: error("No demo quote for $symbol")
        tickerDao.insertTickers(TickerEntity.fromModel(quote))
        return quote
    }
}
