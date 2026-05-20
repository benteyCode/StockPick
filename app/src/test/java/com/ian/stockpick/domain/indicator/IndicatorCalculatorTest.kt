package com.ian.stockpick.domain.indicator

import com.ian.stockpick.domain.model.DailyBar
import org.junit.Assert.assertEquals
import org.junit.Test

class IndicatorCalculatorTest {

    private val calc = IndicatorCalculator()

    @Test
    fun `ma10 returns average of last 10 closes`() {
        val bars = (1..10).map { i ->
            DailyBar(
                "2026-01-$i",
                open = i.toDouble(),
                close = i.toDouble(),
                high = i.toDouble(),
                low = i.toDouble(),
                volume = 1000L
            )
        }
        assertEquals(5.5, calc.ma(bars, 10)!!, 0.001)
    }

    @Test
    fun `avgVolume excludes today and uses prior N days`() {
        val bars = (1..6).map { i ->
            DailyBar("2026-01-0$i", 1.0, 1.0, 1.0, 1.0, volume = i * 1000L)
        }
        assertEquals(3000.0, calc.avgVolume(bars.dropLast(1), 5)!!, 0.001)
    }

    @Test
    fun `ma returns null when insufficient bars`() {
        val bars = listOf(DailyBar("2026-01-01", 1.0, 1.0, 1.0, 1.0, 1000L))
        assertEquals(null, calc.ma(bars, 10))
    }
}
