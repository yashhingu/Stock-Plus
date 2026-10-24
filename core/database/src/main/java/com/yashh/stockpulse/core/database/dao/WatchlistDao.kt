package com.yashh.stockpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yashh.stockpulse.core.database.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY sortOrder ASC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(entity: WatchlistEntity)

    suspend fun addToWatchlist(symbol: String) {
        addToWatchlist(
            WatchlistEntity(
                symbol = symbol,
                companyName = symbol,
                addedAt = System.currentTimeMillis(),
                sortOrder = 0
            )
        )
    }

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun removeFromWatchlist(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    fun isInWatchlist(symbol: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    suspend fun isSymbolInWatchlist(symbol: String): Boolean

    @Query("UPDATE watchlist SET sortOrder = :newOrder WHERE symbol = :symbol")
    suspend fun updateSortOrder(symbol: String, newOrder: Int)
}
