package com.yashh.stockpulse.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yashh.stockpulse.core.model.DemoMarketData
import com.yashh.stockpulse.core.database.StockPulseDatabase
import com.yashh.stockpulse.core.database.dao.AccountDao
import com.yashh.stockpulse.core.database.dao.CandleDao
import com.yashh.stockpulse.core.database.dao.PortfolioDao
import com.yashh.stockpulse.core.database.dao.TickerDao
import com.yashh.stockpulse.core.database.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StockPulseDatabase {
        // Using destructive migration for now.
        // Need to add proper migrations before v2 release!
        return Room.databaseBuilder(
            context,
            StockPulseDatabase::class.java,
            "stockpulse.db"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // Initialize default demo account balance ($50,000 USD)
                db.execSQL(
                    "INSERT OR IGNORE INTO user_account (id, cashBalance, initialBalance, lastUpdated) VALUES (1, 50000.0, 50000.0, ?)",
                    arrayOf(System.currentTimeMillis())
                )
                listOf(
                    Triple("AAPL", 8.0, 181.20),
                    Triple("NVDA", 12.0, 118.50),
                    Triple("TSLA", 3.0, 260.00)
                ).forEach { (symbol, quantity, buyPrice) ->
                    val quote = DemoMarketData.quote(symbol) ?: return@forEach
                    db.execSQL(
                        "INSERT INTO portfolio (symbol, companyName, quantity, avgBuyPrice, currentPrice, lastUpdated) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf(symbol, quote.companyName, quantity, buyPrice, quote.currentPrice, quote.timestamp)
                    )
                }
                listOf("AAPL", "NVDA").forEachIndexed { index, symbol ->
                    val quote = DemoMarketData.quote(symbol) ?: return@forEachIndexed
                    db.execSQL(
                        "INSERT INTO watchlist (symbol, companyName, addedAt, sortOrder) VALUES (?, ?, ?, ?)",
                        arrayOf(symbol, quote.companyName, quote.timestamp, index)
                    )
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Ensure user_account row exists on upgrade or launch
                db.execSQL(
                    "INSERT OR IGNORE INTO user_account (id, cashBalance, initialBalance, lastUpdated) VALUES (1, 50000.0, 50000.0, ?)",
                    arrayOf(System.currentTimeMillis())
                )
            }
        })
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTickerDao(database: StockPulseDatabase): TickerDao = database.tickerDao()

    @Provides
    fun provideCandleDao(database: StockPulseDatabase): CandleDao = database.candleDao()

    @Provides
    fun provideWatchlistDao(database: StockPulseDatabase): WatchlistDao = database.watchlistDao()

    @Provides
    fun providePortfolioDao(database: StockPulseDatabase): PortfolioDao = database.portfolioDao()

    @Provides
    fun provideAccountDao(database: StockPulseDatabase): AccountDao = database.accountDao()
}
