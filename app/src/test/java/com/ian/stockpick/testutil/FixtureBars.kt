package com.ian.stockpick.testutil

import com.ian.stockpick.domain.model.DailyBar

object FixtureBars {
    fun uptrendWithVolumeSurge(): List<DailyBar> =
        (1..30).map { i ->
            val vol = when (i) {
                15, 25 -> 2_000_000L
                else -> 1_000_000L
            }
            DailyBar(
                date = "2026-04-${i.toString().padStart(2, '0')}",
                open = 10.0 + i * 0.02,
                close = 10.0 + i * 0.02,
                high = 10.0 + i * 0.03,
                low = 10.0 + i * 0.01,
                volume = vol,
            )
        }
}
