package com.yashh.stockpulse.feature.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yashh.stockpulse.core.designsystem.component.ErrorState
import com.yashh.stockpulse.core.designsystem.component.LoadingIndicator
import com.yashh.stockpulse.core.designsystem.component.ThemeCustomizerSheet
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors
import com.yashh.stockpulse.feature.portfolio.component.AddHoldingDialog
import com.yashh.stockpulse.feature.portfolio.component.AllocationChart
import com.yashh.stockpulse.feature.portfolio.component.PnLSummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portfolio & Paper Trading") },
                actions = {
                    IconButton(onClick = { showThemeSheet = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme Customizer")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Holding")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (state.error != null) {
            ErrorState(
                message = state.error!!,
                onRetry = { viewModel.processIntent(PortfolioIntent.LoadPortfolio) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    PnLSummaryCard(
                        totalValue = state.totalValue,
                        stockEquity = state.stockEquity,
                        availableCash = state.availableCash,
                        totalPnL = state.totalPnL,
                        pnlPercent = state.totalPnLPercent
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (state.holdings.isNotEmpty()) {
                    item {
                        Text("Asset Allocation", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        AllocationChart(holdings = state.holdings)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Holdings (${state.holdings.size})", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(
                        items = state.holdings,
                        key = { it.symbol }
                    ) { holding ->
                        SwipeToDeleteHolding(
                            holding = holding,
                            onDelete = { viewModel.processIntent(PortfolioIntent.RemoveHolding(holding.symbol)) }
                        )
                    }
                } else {
                    item {
                        // Empty state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Your portfolio is empty.\nExecute a paper trade or add a holding to get started!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddHoldingDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { sym, qty, price ->
                    viewModel.processIntent(PortfolioIntent.AddHolding(sym, qty, price))
                }
            )
        }

        if (showThemeSheet) {
            ThemeCustomizerSheet(onDismissRequest = { showThemeSheet = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteHolding(
    holding: PortfolioHolding,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = Color.Red.copy(alpha = 0.8f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(holding.symbol, style = MaterialTheme.typography.titleMedium)
                    Text("${holding.quantity.toInt()} shares @ $${String.format("%.2f", holding.averagePrice)}", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${String.format("%.2f", holding.totalValue)}", style = MaterialTheme.typography.titleMedium)
                    val pnl = holding.pnl
                    val isPos = pnl >= 0
                    val color = if (isPos) LocalTradingColors.current.bull else LocalTradingColors.current.bear
                    Text(
                        "${if (isPos) "+" else "-"}$${String.format("%.2f", Math.abs(pnl))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = color
                    )
                }
            }
        }
    }
}
