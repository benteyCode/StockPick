package com.ian.stockpick.data.local

import androidx.room.Entity
import com.ian.stockpick.domain.model.DailyBar

@Entity(
    tableName = "daily_bars",
    primaryKeys = ["code", "date"],
)
data class DailyBarEntity(
    val code: String,
    val date: String,
    val open: Double,
    val close: Double,
    val high: Double,
    val low: Double,
    val volume: Long,
    val cachedAt: Long,
)

fun DailyBarEntity.toDomain() = DailyBar(date, open, close, high, low, volume)

fun DailyBar.toEntity(code: String, cachedAt: Long) = DailyBarEntity(
    code = code,
    date = date,
    open = open,
    close = close,
    high = high,
    low = low,
    volume = volume,
    cachedAt = cachedAt,
)
