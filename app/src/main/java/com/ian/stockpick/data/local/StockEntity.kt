package com.ian.stockpick.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ian.stockpick.domain.model.MarketBoard
import com.ian.stockpick.domain.model.Stock

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val code: String,
    val name: String,
    val secId: String,
    val board: String,
    val cachedAt: Long,
)

fun StockEntity.toDomain() = Stock(code, name, secId, MarketBoard.valueOf(board))

fun Stock.toEntity(cachedAt: Long) = StockEntity(code, name, secId, board.name, cachedAt)
