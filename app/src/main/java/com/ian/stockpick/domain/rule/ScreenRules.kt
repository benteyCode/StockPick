package com.ian.stockpick.domain.rule

object ScreenRules {
    const val MA10_PROXIMITY_MIN = -0.01 // -1%
    const val MA10_PROXIMITY_MAX = 0.01 // +1%
    const val MA_CONVERGENCE_MAX = 0.02 // 2%
    const val VOLUME_SURGE_MULTIPLIER = 1.5
    const val VOLUME_SURGE_MIN_DAYS = 2
    const val VOLUME_SURGE_LOOKBACK = 20
    const val PULLBACK_LOOKBACK_MIN = 5
    const val PULLBACK_LOOKBACK_MAX = 10
    const val MA_SHORT = 10
    const val MA_LONG = 20
    const val AVG_VOLUME_SHORT = 5
    const val AVG_VOLUME_LONG = 20
    const val KLINE_MIN_BARS = 30
}
