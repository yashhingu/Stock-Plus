package com.yashh.stockpulse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors

@Composable
fun StockCard(
    symbol: String,
    name: String,
    price: Double,
    change: Double,
    changePercent: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = change >= 0
    val indicatorColor = if (isPositive) LocalTradingColors.current.bull else LocalTradingColors.current.bear

    // subtle gradient based on direction
    val gradient = Brush.verticalGradient(
        colors = listOf(
            indicatorColor.copy(alpha = 0.1f),
            Color.Transparent
        )
    )

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${formatPrice(price)}",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val sign = if (isPositive) "+" else "-"
                val arrow = if (isPositive) "▲" else "▼"
                Text(
                    text = "$arrow $sign$${formatPrice(kotlin.math.abs(change))} (${formatPrice(kotlin.math.abs(changePercent))}%)",
                    color = indicatorColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatPrice(price: Double): String = String.format("%.2f", price)
