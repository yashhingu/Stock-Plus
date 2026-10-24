package com.yashh.stockpulse.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors
import com.yashh.stockpulse.core.designsystem.theme.Typography
import kotlin.math.abs

// helper to format price
private fun formatPrice(price: Double): String = String.format("%.2f", price)

@Composable
fun PriceText(
    price: Double,
    change: Double,
    changePercent: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = change >= 0
    val color = if (isPositive) LocalTradingColors.current.bull else LocalTradingColors.current.bear
    val sign = if (isPositive) "+" else "-"
    val arrow = if (isPositive) "▲" else "▼"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = "$${formatPrice(price)}",
            style = Typography.labelMedium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$arrow $sign$${formatPrice(abs(change))} (${formatPrice(abs(changePercent))}%)",
            color = color,
            style = Typography.labelMedium
        )
    }
}
