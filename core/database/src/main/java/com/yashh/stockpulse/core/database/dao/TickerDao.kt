package com.yashh.stockpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yashh.stockpulse.core.database.entity.TickerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TickerDao {
    @Query("SELECT * FROM tickers")
    fun getAllTickers(): Flow<List<TickerEntity>>

    @Query("SELECT * FROM tickers WHERE symbol = :symbol")
    fun getTickerBySymbol(symbol: String): Flow<TickerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickers(vararg tickers: TickerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tickers: List<TickerEntity>)

    @Query("SELECT * FROM tickers ORDER BY changePercent DESC LIMIT :limit")
    fun getTopGainers(limit: Int): Flow<List<TickerEntity>>

    @Query("SELECT * FROM tickers ORDER BY changePercent ASC LIMIT :limit")
    fun getTopLosers(limit: Int): Flow<List<TickerEntity>>

    @Delete
    suspend fun deleteTicker(ticker: TickerEntity)
}
