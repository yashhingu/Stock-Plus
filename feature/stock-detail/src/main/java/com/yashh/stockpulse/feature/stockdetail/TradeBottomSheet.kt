package com.yashh.stockpulse.feature.stockdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors
import com.yashh.stockpulse.feature.stockdetail.chart.formatPrice
import kotlin.math.floor
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeBottomSheet(
    symbol: String,
    companyName: String,
    currentPrice: Double,
    availableCash: Double,
    heldQuantity: Double,
    onDismissRequest: () -> Unit,
    onExecuteOrder: (isBuy: Boolean, quantity: Double, price: Double) -> Unit
) {
    var isBuy by remember { mutableStateOf(true) }
    var quantityText by remember { mutableStateOf("1") }

    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val totalCost = quantity * currentPrice
    val canAfford = if (isBuy) (totalCost <= availableCash && quantity > 0) else (quantity <= heldQuantity && quantity > 0)

    val bullColor = LocalTradingColors.current.bull
    val bearColor = LocalTradingColors.current.bear

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paper Trading Execution",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "$symbol · $companyName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buy / Sell Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isBuy = true },
                    color = if (isBuy) bullColor.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Text(
                        text = "BUY",
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (isBuy) bullColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isBuy = false },
                    color = if (!isBuy) bearColor.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Text(
                        text = "SELL",
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (!isBuy) bearColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Account Balance & Position Information Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Available Cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${formatPrice(availableCash)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Currently Held",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${heldQuantity.toInt()} shares",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Price & Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Market Price",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "$${formatPrice(currentPrice)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quantity Input Field
            OutlinedTextField(
                value = quantityText,
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() } && newVal.length <= 6) {
                        quantityText = newVal
                    }
                },
                label = { Text("Number of Shares") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Stepper Chips (+1, +5, +10, MAX)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 5, 10).forEach { inc ->
                    SuggestionChip(
                        onClick = {
                            val current = quantityText.toIntOrNull() ?: 0
                            quantityText = (current + inc).toString()
                        },
                        label = { Text("+$inc") }
                    )
                }

                SuggestionChip(
                    onClick = {
                        val maxShares = if (isBuy) {
                            if (currentPrice > 0) floor(availableCash / currentPrice).toInt() else 0
                        } else {
                            heldQuantity.toInt()
                        }
                        quantityText = max(0, maxShares).toString()
                    },
                    label = { Text("MAX") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order Total Breakdown
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isBuy) "Estimated Order Cost" else "Estimated Proceeds",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$${formatPrice(totalCost)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isBuy) bullColor else bearColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val remainingCash = if (isBuy) availableCash - totalCost else availableCash + totalCost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Remaining Cash Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${formatPrice(remainingCash.coerceAtLeast(0.0))}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Order Execution Button
            Button(
                onClick = {
                    onExecuteOrder(isBuy, quantity, currentPrice)
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBuy) bullColor else bearColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                val actionLabel = if (isBuy) "BUY $quantity $symbol" else "SELL $quantity $symbol"
                val errorReason = when {
                    quantity <= 0 -> "ENTER VALID QUANTITY"
                    isBuy && totalCost > availableCash -> "INSUFFICIENT CASH FUNDS"
                    !isBuy && quantity > heldQuantity -> "INSUFFICIENT SHARES TO SELL"
                    else -> actionLabel
                }
                Text(
                    text = errorReason,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
