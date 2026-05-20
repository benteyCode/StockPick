package com.ian.stockpick

import android.app.Application
import androidx.room.Room
import com.ian.stockpick.data.local.QuoteCache
import com.ian.stockpick.data.local.StockPickDatabase
import com.ian.stockpick.data.remote.EastMoneyQuoteDataSource
import com.ian.stockpick.data.repository.StockRepository
import com.ian.stockpick.screen.RunScreenUseCase

class StockPickApp : Application() {

    lateinit var repository: StockRepository
        private set

    lateinit var runScreenUseCase: RunScreenUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(
            this,
            StockPickDatabase::class.java,
            "stockpick.db",
        ).build()
        val remote = EastMoneyQuoteDataSource.create()
        val cache = QuoteCache(db.stockDao(), db.dailyBarDao())
        repository = StockRepository(remote, cache, db.stockDao(), db.dailyBarDao())
        runScreenUseCase = RunScreenUseCase(repository)
    }
}
