package com.ian.stockpick.domain.rule

import com.ian.stockpick.domain.indicator.IndicatorCalculator
import com.ian.stockpick.domain.model.DailyBar

class ScreenRuleEngine(
    private val calc: IndicatorCalculator = IndicatorCalculator(),
) {

    fun matches(input: ScreenInput): Boolean =
        rule1(input) && rule2(input) && rule3(input) && rule4(input)

    private fun rule1(input: ScreenInput): Boolean {
        val bars = input.historicalBars
        val ma10 = calc.ma(bars, ScreenRules.MA_SHORT) ?: return false
        val ma20 = calc.ma(bars, ScreenRules.MA_LONG) ?: return false
        if (ma10 <= ma20) return false
        if (input.currentPrice <= ma20) return false
        val dist = calc.distanceRatio(input.currentPrice, ma10)
        if (dist < ScreenRules.MA10_PROXIMITY_MIN || dist > ScreenRules.MA10_PROXIMITY_MAX) return false
        return hadPullbackSetup(bars)
    }

    private fun hadPullbackSetup(bars: List<DailyBar>): Boolean {
        val lookback = bars.takeLast(ScreenRules.PULLBACK_LOOKBACK_MAX)
        if (lookback.size < ScreenRules.PULLBACK_LOOKBACK_MIN) return false
        return lookback.any { bar ->
            val idx = bars.indexOf(bar)
            val prior = bars.take(idx + 1)
            val ma10AtBar = calc.ma(prior, ScreenRules.MA_SHORT) ?: return@any false
            bar.close > ma10AtBar
        }
    }

    private fun rule2(input: ScreenInput): Boolean {
        val ma10 = calc.ma(input.historicalBars, ScreenRules.MA_SHORT) ?: return false
        val ma20 = calc.ma(input.historicalBars, ScreenRules.MA_LONG) ?: return false
        return calc.maConvergenceRatio(ma10, ma20) <= ScreenRules.MA_CONVERGENCE_MAX
    }

    private fun rule3(input: ScreenInput): Boolean {
        val recent = input.historicalBars.takeLast(ScreenRules.VOLUME_SURGE_LOOKBACK)
        if (recent.size < ScreenRules.VOLUME_SURGE_LOOKBACK) return false
        val avg20 = calc.avgVolume(recent, ScreenRules.VOLUME_SURGE_LOOKBACK) ?: return false
        val surgeDays = recent.count { it.volume > avg20 * ScreenRules.VOLUME_SURGE_MULTIPLIER }
        return surgeDays >= ScreenRules.VOLUME_SURGE_MIN_DAYS
    }

    private fun rule4(input: ScreenInput): Boolean {
        if (input.currentPrice >= input.todayOpen) return false
        val avg5 = calc.avgVolume(input.historicalBars, ScreenRules.AVG_VOLUME_SHORT) ?: return false
        return input.currentVolume < avg5
    }

    fun buildStatusLabel(input: ScreenInput): String {
        val ma10 = calc.ma(input.historicalBars, ScreenRules.MA_SHORT) ?: return "缩量回调"
        val ma20 = calc.ma(input.historicalBars, ScreenRules.MA_LONG) ?: return "缩量回调"
        return if (calc.maConvergenceRatio(ma10, ma20) <= ScreenRules.MA_CONVERGENCE_MAX) {
            "MA10/20粘合"
        } else {
            "缩量回调"
        }
    }
}
