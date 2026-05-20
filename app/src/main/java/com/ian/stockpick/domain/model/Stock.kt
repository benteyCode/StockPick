package com.ian.stockpick.domain.model

enum class MarketBoard { SH_MAIN, SZ_MAIN, CHINEXT, STAR, OTHER }

data class Stock(
    val code: String,
    val name: String,
    val secId: String, // "1.600519" or "0.000001"
    val board: MarketBoard,
) {
    val isSt: Boolean get() = name.contains("ST", ignoreCase = true)
}
