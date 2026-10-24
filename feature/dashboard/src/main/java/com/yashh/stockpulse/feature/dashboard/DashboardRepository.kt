package com.yashh.stockpulse.feature.dashboard

import com.yashh.stockpulse.core.database.dao.TickerDao
import com.yashh.stockpulse.core.database.entity.TickerEntity
import com.yashh.stockpulse.core.model.DemoMarketData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface DashboardRepository {
    fun fetchMarketOverview(symbols: List<String>): Flow<List<StockQuote>>
}

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val tickerDao: TickerDao
) : DashboardRepository {

    override fun fetchMarketOverview(symbols: List<String>): Flow<List<StockQuote>> = flow {
        val demoQuotes = symbols.mapNotNull { DemoMarketData.quote(it) }
        tickerDao.insertAll(demoQuotes.map { TickerEntity.fromModel(it) })
        emit(demoQuotes.map { TickerEntity.fromModel(it).toDashboardQuote() })
    }
}

private fun TickerEntity.toDashboardQuote() = StockQuote(
    symbol = symbol,
    name = companyName,
    price = currentPrice,
    change = change,
    changePercent = changePercent
)
