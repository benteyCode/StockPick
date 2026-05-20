package com.ian.stockpick.data.local

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class QuoteCache(
    private val stockDao: StockDao,
    private val dailyBarDao: DailyBarDao,
) {
    suspend fun isStockListFresh(now: Long = System.currentTimeMillis()): Boolean {
        val cachedAt = stockDao.getCachedAt() ?: return false
        return now - cachedAt < 24 * 60 * 60 * 1000
    }

    suspend fun isKlineFresh(code: String, now: Long = System.currentTimeMillis()): Boolean {
        val bars = dailyBarDao.getByCode(code)
        if (bars.isEmpty()) return false
        val sameDay = Instant.ofEpochMilli(now)
            .atZone(ZoneId.of("Asia/Shanghai"))
            .toLocalDate()
            .toString()
        return bars.first().cachedAt >= startOfDayMillis(sameDay)
    }

    private fun startOfDayMillis(date: String): Long {
        val localDate = LocalDate.parse(date)
        return localDate.atStartOfDay(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()
    }
}
