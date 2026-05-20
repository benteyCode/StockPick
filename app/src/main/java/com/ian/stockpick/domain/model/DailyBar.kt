package com.ian.stockpick.domain.model

data class DailyBar(
    val date: String, // yyyy-MM-dd
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
)
