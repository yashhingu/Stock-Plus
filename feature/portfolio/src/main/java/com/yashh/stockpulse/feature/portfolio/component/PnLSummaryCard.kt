package com.yashh.stockpulse.feature.portfolio.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors
import java.text.DecimalFormat

private val moneyFormat = DecimalFormat("#,##0.00")

@Composable
fun PnLSummaryCard(
    totalValue: Double,
    stockEquity: Double = 0.0,
    availableCash: Double = 0.0,
    totalPnL: Double,
    pnlPercent: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Account Value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$${moneyFormat.format(totalValue)}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val isPositive = totalPnL >= 0
            val color = if (isPositive) LocalTradingColors.current.bull else LocalTradingColors.current.bear
            val sign = if (isPositive) "+" else "-"
            val arrow = if (isPositive) "▲" else "▼"

            Text(
                text = "$arrow $sign$${moneyFormat.format(Math.abs(totalPnL))} (${String.format("%.2f", Math.abs(pnlPercent))}%)",
                color = color,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
            )

            if (availableCash > 0 || stockEquity > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Stock Equity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${moneyFormat.format(stockEquity)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Virtual Cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${moneyFormat.format(availableCash)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}
