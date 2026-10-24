package com.yashh.stockpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yashh.stockpulse.core.database.entity.CandleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CandleDao {
    @Query("SELECT * FROM candles WHERE symbol = :symbol AND timeFrame = :timeFrame ORDER BY timestamp ASC")
    fun getCandlesForSymbol(symbol: String, timeFrame: String): Flow<List<CandleEntity>>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandles(candles: List<CandleEntity>)

    @Query("DELETE FROM candles WHERE symbol = :symbol AND timestamp < :beforeTimestamp")
    suspend fun deleteOldCandles(symbol: String, beforeTimestamp: Long)
}
