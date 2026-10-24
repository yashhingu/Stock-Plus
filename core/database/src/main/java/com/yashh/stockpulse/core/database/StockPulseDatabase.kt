package com.yashh.stockpulse.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yashh.stockpulse.core.database.dao.AccountDao
import com.yashh.stockpulse.core.database.dao.CandleDao
import com.yashh.stockpulse.core.database.dao.PortfolioDao
import com.yashh.stockpulse.core.database.dao.TickerDao
import com.yashh.stockpulse.core.database.dao.WatchlistDao
import com.yashh.stockpulse.core.database.entity.CandleEntity
import com.yashh.stockpulse.core.database.entity.PortfolioEntity
import com.yashh.stockpulse.core.database.entity.TickerEntity
import com.yashh.stockpulse.core.database.entity.UserAccountEntity
import com.yashh.stockpulse.core.database.entity.WatchlistEntity

@Database(
    entities = [
        TickerEntity::class,
        CandleEntity::class,
        WatchlistEntity::class,
        PortfolioEntity::class,
        UserAccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StockPulseDatabase : RoomDatabase() {
    abstract fun tickerDao(): TickerDao
    abstract fun candleDao(): CandleDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun accountDao(): AccountDao
}
