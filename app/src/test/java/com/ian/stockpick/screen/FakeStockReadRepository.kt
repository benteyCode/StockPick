package com.ian.stockpick.screen

import com.ian.stockpick.data.repository.StockReadRepository
import com.ian.stockpick.domain.model.DailyBar
import com.ian.stockpick.domain.model.QuoteSnapshot
import com.ian.stockpick.domain.model.Stock

class FakeStockReadRepository(
    private val stocks: List<Stock>,
    private val snapshots: List<QuoteSnapshot>,
    private val barsByCode: Map<String, List<DailyBar>>,
) : StockReadRepository {
    override suspend fun getStockList(forceRefresh: Boolean): List<Stock> = stocks

    override suspend fun getSnapshots(stocks: List<Stock>): List<QuoteSnapshot> = snapshots

    override suspend fun getDailyBars(stock: Stock, forceRefresh: Boolean): List<DailyBar>? =
        barsByCode[stock.code]
}
