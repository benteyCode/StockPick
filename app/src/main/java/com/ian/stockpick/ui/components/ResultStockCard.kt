@file:Suppress("FunctionNaming")

package com.ian.stockpick.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.stockpick.domain.model.MarketBoard
import com.ian.stockpick.domain.model.ScreenResult
import com.ian.stockpick.domain.model.Stock
import com.ian.stockpick.ui.theme.AccentGreen
import com.ian.stockpick.ui.theme.CardBg
import com.ian.stockpick.ui.theme.CardBorder
import com.ian.stockpick.ui.theme.GreenUp
import com.ian.stockpick.ui.theme.RedDown
import com.ian.stockpick.ui.theme.TextPrimary
import com.ian.stockpick.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ResultStockCard(
    result: ScreenResult,
    modifier: Modifier = Modifier,
) {
    val changeColor = if (result.changePercent >= 0) GreenUp else RedDown
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${result.stock.code} ${result.stock.name}",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = formatPercent(result.changePercent),
                    color = changeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
            Text(
                text = result.stock.exchangeLabel(),
                color = TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricColumn("距MA10", formatPercent(result.distanceToMa10 * 100, signed = true))
                MetricColumn("量比", String.format(Locale.CHINA, "%.1f", result.volumeRatio))
                MetricColumn(
                    label = "状态",
                    value = result.statusLabel,
                    valueColor = AccentGreen,
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun formatPercent(value: Double, signed: Boolean = false): String {
    val prefix = if (signed && value > 0) "+" else ""
    return String.format(Locale.CHINA, "%s%.1f%%", prefix, value)
}

fun Stock.exchangeLabel(): String = when (board) {
    MarketBoard.SH_MAIN -> "SHANGHAI STOCK EXCHANGE"
    MarketBoard.SZ_MAIN -> "SHENZHEN STOCK EXCHANGE"
    MarketBoard.CHINEXT -> "CHINEXT"
    MarketBoard.STAR -> "STAR MARKET"
    MarketBoard.OTHER -> ""
}
