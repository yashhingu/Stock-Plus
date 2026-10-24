package com.yashh.stockpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yashh.stockpulse.core.database.entity.PortfolioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio")
    fun getAllHoldings(): Flow<List<PortfolioEntity>>

    @Query("SELECT * FROM portfolio WHERE symbol = :symbol")
    fun getHolding(symbol: String): Flow<PortfolioEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(entity: PortfolioEntity)

    @Query("UPDATE portfolio SET currentPrice = :newPrice, lastUpdated = :timestamp WHERE symbol = :symbol")
    suspend fun updatePrice(symbol: String, newPrice: Double, timestamp: Long)

    @Query("DELETE FROM portfolio WHERE symbol = :symbol")
    suspend fun deleteHolding(symbol: String)
}
