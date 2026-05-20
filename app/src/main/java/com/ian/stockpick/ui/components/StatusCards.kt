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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.stockpick.ui.theme.AccentBlue
import com.ian.stockpick.ui.theme.AccentGreen
import com.ian.stockpick.ui.theme.CardBg
import com.ian.stockpick.ui.theme.CardBorder
import com.ian.stockpick.ui.theme.TextSecondary

@Composable
fun StatusCards(
    lastRunTime: String?,
    hitCount: Int,
    dataAsOf: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(
                label = "上次运行",
                value = lastRunTime ?: "--:--",
                valueColor = AccentBlue,
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                label = "命中结果",
                value = "$hitCount 只",
                valueColor = AccentGreen,
                modifier = Modifier.weight(1f),
            )
        }
        if (dataAsOf != null) {
            Text(
                text = "数据截至：$dataAsOf",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = label, color = TextSecondary, fontSize = 12.sp)
            Text(
                text = value,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
