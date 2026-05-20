package com.ian.stockpick.domain.model

data class ScreenResult(
    val stock: Stock,
    val price: Double,
    val changePercent: Double,
    val distanceToMa10: Double,
    val volumeRatio: Double,
    val statusLabel: String,
)
