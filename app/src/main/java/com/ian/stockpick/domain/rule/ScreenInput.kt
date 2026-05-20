package com.ian.stockpick.domain.rule

import com.ian.stockpick.domain.model.DailyBar

data class ScreenInput(
    val historicalBars: List<DailyBar>,
    val todayOpen: Double,
    val currentPrice: Double,
    val currentVolume: Long,
)
