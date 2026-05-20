package com.ian.stockpick.domain.model

data class QuoteSnapshot(
    val code: String,
    val price: Double,
    val open: Double,
    val changePercent: Double,
    val volume: Long,
    val suspended: Boolean = false,
)
