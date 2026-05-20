package com.ian.stockpick.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StockEntity::class, DailyBarEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class StockPickDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun dailyBarDao(): DailyBarDao
}
