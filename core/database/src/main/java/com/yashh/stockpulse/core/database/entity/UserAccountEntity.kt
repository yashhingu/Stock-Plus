package com.yashh.stockpulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity tracking the user's paper trading virtual wallet cash balance.
 */
@Entity(tableName = "user_account")
data class UserAccountEntity(
    @PrimaryKey val id: Int = 1,
    val cashBalance: Double = 50000.00,
    val initialBalance: Double = 50000.00,
    val lastUpdated: Long = System.currentTimeMillis()
)
