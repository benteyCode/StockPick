package com.ian.stockpick.domain.rule

import com.ian.stockpick.testutil.FixtureBars
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRuleEngineTest {

    private val engine = ScreenRuleEngine()

    @Suppress("FunctionMaxLength")
    @Test
    fun `passes all rules when pullback shrink negative near ma10`() {
        val bars = FixtureBars.uptrendWithVolumeSurge()
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10 * 1.005,
            currentPrice = ma10 * 0.998,
            currentVolume = 500_000L,
        )
        assertTrue(engine.matches(input))
    }

    @Test
    fun `fails when price too far below ma10`() {
        val bars = FixtureBars.uptrendWithVolumeSurge()
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10,
            currentPrice = ma10 * 0.95,
            currentVolume = 500_000L,
        )
        assertFalse(engine.matches(input))
    }

    @Test
    fun `fails when today is positive candle`() {
        val bars = FixtureBars.uptrendWithVolumeSurge()
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10 * 0.99,
            currentPrice = ma10 * 1.001,
            currentVolume = 500_000L,
        )
        assertFalse(engine.matches(input))
    }

    @Test
    fun `fails when insufficient volume surge days`() {
        val bars = FixtureBars.uptrendWithVolumeSurge().mapIndexed { i, b ->
            if (i == 7) b.copy(volume = 2_000_000L) else b.copy(volume = 1_000_000L)
        }
        val ma10 = bars.takeLast(10).map { it.close }.average()
        val input = ScreenInput(
            historicalBars = bars,
            todayOpen = ma10 * 1.005,
            currentPrice = ma10 * 0.998,
            currentVolume = 500_000L,
        )
        assertFalse(engine.matches(input))
    }
}
