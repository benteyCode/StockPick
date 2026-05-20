package com.ian.stockpick.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DailyBarDao {
    @Upsert
    suspend fun upsertAll(bars: List<DailyBarEntity>)

    @Query("SELECT * FROM daily_bars WHERE code = :code ORDER BY date ASC")
    suspend fun getByCode(code: String): List<DailyBarEntity>

    @Query("DELETE FROM daily_bars WHERE code = :code")
    suspend fun deleteByCode(code: String)
}
