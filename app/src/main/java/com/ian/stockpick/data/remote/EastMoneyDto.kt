package com.ian.stockpick.data.remote

import com.ian.stockpick.domain.model.DailyBar
import com.ian.stockpick.domain.model.MarketBoard
import com.ian.stockpick.domain.model.QuoteSnapshot
import com.ian.stockpick.domain.model.Stock
import com.squareup.moshi.Json
import kotlin.math.abs

internal data class EastMoneyClistResponse(
    val rc: Int? = null,
    val data: EastMoneyClistData? = null,
)

internal data class EastMoneyClistData(
    val total: Int? = null,
    val diff: List<CListRowDto>? = null,
)

internal data class CListRowDto(
    @Json(name = "f12") val f12: Long? = null,
    @Json(name = "f14") val f14: String? = null,
    @Json(name = "f13") val f13: Int? = null,
)

internal data class EastMoneyKlineResponse(
    val data: EastMoneyKlineData? = null,
)

internal data class EastMoneyKlineData(
    val klines: List<String>? = null,
)

internal data class EastMoneyUlistResponse(
    val rc: Int? = null,
    val data: EastMoneyUlistData? = null,
)

internal data class EastMoneyUlistData(
    val total: Int? = null,
    val diff: List<UlistRowDto>? = null,
)

internal data class UlistRowDto(
    @Json(name = "f12") val f12: Long? = null,
    @Json(name = "f2") val f2: Double? = null,
    @Json(name = "f3") val f3: Double? = null,
    @Json(name = "f5") val f5: Double? = null,
    @Json(name = "f17") val f17: Double? = null,
    @Json(name = "f152") val f152: Int? = null,
)

internal fun normalizeEastMoneyPrice(raw: Double): Double {
    return if (raw > 1000.0) raw / 100.0 else raw
}

private fun normalizeEastMoneyPercent(raw: Double): Double {
    return if (abs(raw) > 1000.0) raw / 100.0 else raw
}

private fun Long.toPaddedCode(): String = toString().padStart(6, '0')

internal fun CListRowDto.toStockOrNull(): Stock? {
    val codeNum = f12 ?: return null
    val name = f14?.trim().orEmpty()
    if (name.contains("ST", ignoreCase = true)) return null
    val market = f13 ?: return null
    val code = codeNum.toPaddedCode()
    return when (market) {
        1 -> {
            val secId = "1.$code"
            val board = if (code.startsWith("688") || code.startsWith("689")) {
                MarketBoard.STAR
            } else {
                MarketBoard.SH_MAIN
            }
            Stock(code, name, secId, board)
        }
        0 -> {
            val secId = "0.$code"
            val board = if (code.startsWith("300") || code.startsWith("301")) {
                MarketBoard.CHINEXT
            } else {
                MarketBoard.SZ_MAIN
            }
            Stock(code, name, secId, board)
        }
        else -> null
    }
}

internal fun UlistRowDto.toQuoteSnapshotOrNull(): QuoteSnapshot? {
    val codeNum = f12 ?: return null
    val code = codeNum.toPaddedCode()
    val rawPrice = f2 ?: return null
    val price = normalizeEastMoneyPrice(rawPrice)
    val open = f17?.let(::normalizeEastMoneyPrice) ?: 0.0
    val changePercent = f3?.let(::normalizeEastMoneyPercent) ?: 0.0
    val volume = f5?.toLong() ?: 0L
    val suspended = f152 == 2
    return QuoteSnapshot(
        code = code,
        price = price,
        open = open,
        changePercent = changePercent,
        volume = volume,
        suspended = suspended,
    )
}

internal fun parseKlineCsvLine(line: String): DailyBar? {
    val parts = line.split(',')
    if (parts.size < 6) return null
    val date = parts[0].trim()
    val open = parts[1].toDoubleOrNull()?.let(::normalizeEastMoneyPrice) ?: return null
    val close = parts[2].toDoubleOrNull()?.let(::normalizeEastMoneyPrice) ?: return null
    val high = parts[3].toDoubleOrNull()?.let(::normalizeEastMoneyPrice) ?: return null
    val low = parts[4].toDoubleOrNull()?.let(::normalizeEastMoneyPrice) ?: return null
    val volume = parts[5].toDoubleOrNull()?.toLong() ?: return null
    return DailyBar(
        date = date,
        open = open,
        close = close,
        high = high,
        low = low,
        volume = volume,
    )
}
