package com.ian.stockpick.domain.indicator

import com.ian.stockpick.domain.model.DailyBar

class IndicatorCalculator {

    fun ma(bars: List<DailyBar>, period: Int): Double? {
        if (bars.size < period) return null
        return bars.takeLast(period).map { it.close }.average()
    }

    fun avgVolume(bars: List<DailyBar>, period: Int): Double? {
        if (bars.size < period) return null
        return bars.takeLast(period).map { it.volume.toDouble() }.average()
    }

    fun distanceRatio(price: Double, ma: Double): Double = (price - ma) / ma

    fun maConvergenceRatio(maShort: Double, maLong: Double): Double =
        kotlin.math.abs(maShort - maLong) / maLong
}
