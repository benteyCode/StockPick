@file:Suppress("FunctionNaming")

package com.ian.stockpick.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.stockpick.ui.theme.AccentBlue
import com.ian.stockpick.ui.theme.AccentGreen
import com.ian.stockpick.ui.theme.CardBg
import com.ian.stockpick.ui.theme.CardBorder
import com.ian.stockpick.ui.theme.TextPrimary
import com.ian.stockpick.ui.theme.TextSecondary

private val rules = listOf(
    "上升回踩 MA10",
    "MA10/MA20 粘合 ≤2%",
    "近20日放量 ≥2天",
    "今日缩量阴线",
)

@Composable
fun FilterSummaryCard(
    isRunning: Boolean,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    text = "筛选条件（只读摘要）",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardBorder,
                ) {
                    Text(
                        text = "RO MODE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = TextSecondary,
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            rules.forEach { rule ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "●", color = AccentGreen, fontSize = 8.sp)
                    Text(
                        text = rule,
                        modifier = Modifier.padding(start = 8.dp),
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRun,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = TextPrimary,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                } else {
                    Text(text = "▶  ", fontSize = 14.sp)
                }
                Text(text = if (isRunning) "筛选中…" else "运行筛选")
            }
        }
    }
}
