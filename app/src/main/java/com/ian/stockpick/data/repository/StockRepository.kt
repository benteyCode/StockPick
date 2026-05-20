package com.ian.stockpick.data.repository

import com.ian.stockpick.data.QuoteDataSource
import com.ian.stockpick.data.local.DailyBarDao
import com.ian.stockpick.data.local.QuoteCache
import com.ian.stockpick.data.local.StockDao
import com.ian.stockpick.data.local.toDomain
import com.ian.stockpick.data.local.toEntity
import com.ian.stockpick.domain.model.DailyBar
import com.ian.stockpick.domain.model.QuoteSnapshot
import com.ian.stockpick.domain.model.Stock

interface StockReadRepository {
    suspend fun getStockList(forceRefresh: Boolean = false): List<Stock>
    suspend fun getDailyBars(stock: Stock, forceRefresh: Boolean = false): List<DailyBar>?
    suspend fun getSnapshots(stocks: List<Stock>): List<QuoteSnapshot>
}

class StockRepository(
    private val remote: QuoteDataSource,
    private val cache: QuoteCache,
    private val stockDao: StockDao,
    private val dailyBarDao: DailyBarDao,
) : StockReadRepository {
    override suspend fun getStockList(forceRefresh: Boolean): List<Stock> {
        if (!forceRefresh && cache.isStockListFresh()) {
            return stockDao.getAll().map { it.toDomain() }
        }
        val now = System.currentTimeMillis()
        val stocks = remote.fetchAllStocks().filter { !it.isSt }
        stockDao.upsertAll(stocks.map { it.toEntity(now) })
        return stocks
    }

    override suspend fun getDailyBars(stock: Stock, forceRefresh: Boolean): List<DailyBar>? {
        if (!forceRefresh && cache.isKlineFresh(stock.code)) {
            return dailyBarDao.getByCode(stock.code).map { it.toDomain() }
        }
        return try {
            val now = System.currentTimeMillis()
            val bars = remote.fetchDailyBars(stock.secId)
            dailyBarDao.deleteByCode(stock.code)
            dailyBarDao.upsertAll(bars.map { it.toEntity(stock.code, now) })
            bars
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getSnapshots(stocks: List<Stock>): List<QuoteSnapshot> {
        if (stocks.isEmpty()) return emptyList()
        return remote.fetchSnapshots(stocks.map { it.secId })
    }

    suspend fun refreshCache() {
        getStockList(forceRefresh = true)
    }
}
