package com.ian.stockpick.screen

import com.ian.stockpick.domain.model.MarketBoard
import com.ian.stockpick.domain.model.QuoteSnapshot
import com.ian.stockpick.domain.model.Stock
import com.ian.stockpick.testutil.FixtureBars
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunScreenUseCaseTest {

    @Test
    fun `hit stock sorted by ma10 distance`() = runTest {
        val bars = FixtureBars.uptrendWithVolumeSurge()
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val hitStock = Stock("600519", "贵州茅台", "1.600519", MarketBoard.SH_MAIN)
        val missStock = Stock("000001", "平安银行", "0.000001", MarketBoard.SZ_MAIN)

        val repository = FakeStockReadRepository(
            stocks = listOf(hitStock, missStock),
            snapshots = listOf(
                QuoteSnapshot(
                    code = hitStock.code,
                    price = ma10 * 0.998,
                    open = ma10 * 1.005,
                    changePercent = -1.2,
                    volume = 500_000L,
                ),
                QuoteSnapshot(
                    code = missStock.code,
                    price = 10.0,
                    open = 10.0,
                    changePercent = 0.0,
                    volume = 1_000_000L,
                ),
            ),
            barsByCode = mapOf(
                hitStock.code to bars,
                missStock.code to bars,
            ),
        )

        val outcome = RunScreenUseCase(repository).run().getOrThrow()

        assertEquals(1, outcome.results.size)
        assertEquals(hitStock.code, outcome.results.first().stock.code)
        assertTrue(outcome.results.first().distanceToMa10 < 0)
    }
}
