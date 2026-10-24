package com.yashh.stockpulse.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yashh.stockpulse.core.database.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM user_account WHERE id = 1")
    fun getAccount(): Flow<UserAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setAccount(account: UserAccountEntity)

    @Query("UPDATE user_account SET cashBalance = cashBalance + :delta, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateCash(delta: Double, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT cashBalance FROM user_account WHERE id = 1")
    suspend fun getCashBalanceNow(): Double?
}
