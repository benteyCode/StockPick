package com.ian.stockpick.data

import com.ian.stockpick.domain.model.DailyBar
import com.ian.stockpick.domain.model.QuoteSnapshot
import com.ian.stockpick.domain.model.Stock

interface QuoteDataSource {
    suspend fun fetchAllStocks(): List<Stock>
    suspend fun fetchSnapshots(secIds: List<String>): List<QuoteSnapshot>
    suspend fun fetchDailyBars(secId: String, limit: Int = 120): List<DailyBar>
}
