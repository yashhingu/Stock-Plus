package com.yashh.stockpulse.feature.portfolio

import kotlinx.coroutines.flow.Flow
import com.yashh.stockpulse.core.database.dao.PortfolioDao
import com.yashh.stockpulse.core.database.entity.PortfolioEntity
import com.yashh.stockpulse.core.model.DemoMarketData
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface PortfolioRepository {
    fun getAllHoldings(): Flow<List<PortfolioHolding>>
    suspend fun addHolding(symbol: String, qty: Double, price: Double)
    suspend fun removeHolding(symbol: String)
    suspend fun refreshPrices()
}

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val portfolioDao: PortfolioDao
) : PortfolioRepository {
    override fun getAllHoldings(): Flow<List<PortfolioHolding>> = portfolioDao.getAllHoldings().map { entities ->
        entities.map { PortfolioHolding(it.symbol, it.quantity, it.avgBuyPrice, it.currentPrice) }
    }

    override suspend fun addHolding(symbol: String, qty: Double, price: Double) {
        require(symbol.isNotBlank() && qty > 0 && price > 0)
        val existing = portfolioDao.getHolding(symbol).first()
        val newQuantity = (existing?.quantity ?: 0.0) + qty
        val totalCost = (existing?.quantity ?: 0.0) * (existing?.avgBuyPrice ?: 0.0) + qty * price
        portfolioDao.insertHolding(PortfolioEntity(
            symbol, existing?.companyName ?: symbol, newQuantity, totalCost / newQuantity,
            existing?.currentPrice ?: price, System.currentTimeMillis()
        ))
    }

    override suspend fun removeHolding(symbol: String) {
        portfolioDao.deleteHolding(symbol)
    }

    override suspend fun refreshPrices() {
        portfolioDao.getAllHoldings().first().forEach { holding ->
            val price = DemoMarketData.quote(holding.symbol)?.currentPrice
            if (price != null) portfolioDao.updatePrice(holding.symbol, price, System.currentTimeMillis())
        }
    }
}
