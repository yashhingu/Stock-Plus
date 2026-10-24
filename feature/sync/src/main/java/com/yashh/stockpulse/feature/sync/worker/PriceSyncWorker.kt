package com.yashh.stockpulse.feature.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.yashh.stockpulse.core.database.dao.TickerDao
import com.yashh.stockpulse.core.database.entity.TickerEntity
import com.yashh.stockpulse.core.model.DemoMarketData
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class PriceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tickerDao: TickerDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("Starting price sync worker")

        return try {
            val cached = tickerDao.getAllTickers().first()
            cached.forEach { ticker ->
                val quote = DemoMarketData.quote(ticker.symbol)
                if (quote != null) {
                    tickerDao.insertTickers(TickerEntity.fromModel(quote))
                }
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Sync worker failed")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
