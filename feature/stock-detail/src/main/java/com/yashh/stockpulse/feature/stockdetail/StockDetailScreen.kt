package com.yashh.stockpulse.feature.stockdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yashh.stockpulse.core.designsystem.component.ThemeCustomizerSheet
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors
import com.yashh.stockpulse.core.model.TimeFrame
import com.yashh.stockpulse.feature.stockdetail.chart.CandlestickChart
import com.yashh.stockpulse.feature.stockdetail.chart.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    viewModel: StockDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTradeSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    // Handle order notifications
    LaunchedEffect(state) {
        val currentState = state
        if (currentState is StockDetailState.Connected && currentState.orderNotification != null) {
            snackbarHostState.showSnackbar(
                message = currentState.orderNotification,
                duration = SnackbarDuration.Short
            )
            viewModel.processIntent(StockDetailIntent.DismissOrderNotification)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state is StockDetailState.Connected) (state as StockDetailState.Connected).quote.symbol else "Loading...",
                            fontWeight = FontWeight.Bold
                        )
                        if (state is StockDetailState.Connected) {
                            Text(
                                text = (state as StockDetailState.Connected).quote.companyName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showThemeSheet = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Customize Palette")
                    }
                    if (state is StockDetailState.Connected) {
                        val connectedState = state as StockDetailState.Connected
                        IconButton(onClick = { viewModel.processIntent(StockDetailIntent.ToggleWatchlist(connectedState.quote.symbol)) }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Watchlist",
                                tint = if (connectedState.isInWatchlist) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state is StockDetailState.Connected) {
                val conn = state as StockDetailState.Connected
                val bullColor = LocalTradingColors.current.bull
                val bearColor = LocalTradingColors.current.bear

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Virtual Cash: $${formatPrice(conn.availableCash)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Held: ${conn.heldQuantity.toInt()} shares",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        Button(
                            onClick = { showTradeSheet = true },
                            modifier = Modifier.height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = bullColor, contentColor = Color(0xFF062421)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("BUY", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showTradeSheet = true },
                            modifier = Modifier.height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = bearColor, contentColor = Color(0xFF062421)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SELL", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val currentState = state) {
                is StockDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StockDetailState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (val error = currentState.error) {
                                is com.yashh.stockpulse.core.model.UiText.DynamicString -> error.value
                                is com.yashh.stockpulse.core.model.UiText.StringResource -> "Unable to load stock"
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.processIntent(StockDetailIntent.RetryConnection) }) { Text("Retry") }
                    }
                }
                is StockDetailState.Reconnecting -> {
                    Text("Reconnecting...", modifier = Modifier.align(Alignment.Center))
                }
                is StockDetailState.Connected -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Real-time Price Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "$${formatPrice(currentState.quote.currentPrice)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                val isPos = currentState.quote.isPositive
                                val changeColor = if (isPos) LocalTradingColors.current.bull else LocalTradingColors.current.bear
                                Text(
                                    text = "${if (isPos) "+" else ""}${formatPrice(currentState.quote.change)} (${String.format("%.2f", currentState.quote.changePercent)}%)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = changeColor
                                )
                            }

                            // Local demo tick status
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(LocalTradingColors.current.bull, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEMO TICKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // TimeFrame selection row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TimeFrame.values().forEach { tf ->
                                FilterChip(
                                    selected = currentState.selectedTimeFrame == tf,
                                    onClick = { viewModel.processIntent(StockDetailIntent.SelectTimeFrame(tf)) },
                                    label = { Text(tf.label, fontSize = 12.sp) },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }

                        // Technical Indicators Toolbar (SMA, EMA, Volume, RSI)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = currentState.showSma,
                                onClick = { viewModel.processIntent(StockDetailIntent.ToggleIndicator(ema = false)) },
                                label = { Text("SMA 20", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = currentState.showEma,
                                onClick = { viewModel.processIntent(StockDetailIntent.ToggleIndicator(ema = true)) },
                                label = { Text("EMA 20", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = currentState.showVolume,
                                onClick = { viewModel.processIntent(StockDetailIntent.ToggleVolume) },
                                label = { Text("VOL", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = currentState.showRsi,
                                onClick = { viewModel.processIntent(StockDetailIntent.ToggleRsi) },
                                label = { Text("RSI 14", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PRICE  ·  ${currentState.selectedTimeFrame.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Drag to pan  ·  Pinch to zoom  ·  Tap to inspect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }

                        // Candlestick & Indicator Canvas Chart
                        CandlestickChart(
                            candles = currentState.candles,
                            showSma = currentState.showSma,
                            showEma = currentState.showEma,
                            showRsi = currentState.showRsi,
                            showVolume = currentState.showVolume,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    // Paper Trading Bottom Sheet Modal
                    if (showTradeSheet) {
                        TradeBottomSheet(
                            symbol = currentState.quote.symbol,
                            companyName = currentState.quote.companyName,
                            currentPrice = currentState.quote.currentPrice,
                            availableCash = currentState.availableCash,
                            heldQuantity = currentState.heldQuantity,
                            onDismissRequest = { showTradeSheet = false },
                            onExecuteOrder = { isBuy, qty, price ->
                                viewModel.processIntent(StockDetailIntent.ExecuteOrder(isBuy, qty, price))
                            }
                        )
                    }
                }
            }
        }

        // Theme and Trading Palette customizer
        if (showThemeSheet) {
            ThemeCustomizerSheet(onDismissRequest = { showThemeSheet = false })
        }
    }
}
