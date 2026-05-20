package com.ian.stockpick.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface StockDao {
    @Upsert
    suspend fun upsertAll(stocks: List<StockEntity>)

    @Query("SELECT * FROM stocks")
    suspend fun getAll(): List<StockEntity>

    @Query("SELECT MAX(cachedAt) FROM stocks")
    suspend fun getCachedAt(): Long?
}
