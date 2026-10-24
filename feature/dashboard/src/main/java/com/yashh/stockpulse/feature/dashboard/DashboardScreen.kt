package com.yashh.stockpulse.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yashh.stockpulse.core.designsystem.component.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showThemeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("StockPulse", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = { showThemeSheet = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme Customizer")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                LoadingIndicator()
            }
        } else if (state.error != null) {
            ErrorState(
                message = state.error!!,
                onRetry = { viewModel.processIntent(DashboardIntent.RefreshData) }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    val marqueeItems = state.tickerTape.map {
                        MarqueeItem(it.symbol, it.price, it.changePercent)
                    }
                    TickerMarquee(items = marqueeItems)
                }

                item {
                    Text(
                        text = "DEMO MARKET · LIVE SIMULATOR",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item { SectionHeader("Top Gainers") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.topGainers) { stock ->
                            StockCard(
                                symbol = stock.symbol,
                                name = stock.name,
                                price = stock.price,
                                change = stock.change,
                                changePercent = stock.changePercent,
                                onClick = { onNavigateToDetail(stock.symbol) },
                                modifier = Modifier.width(220.dp)
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item { SectionHeader("Top Losers") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.topLosers) { stock ->
                            StockCard(
                                symbol = stock.symbol,
                                name = stock.name,
                                price = stock.price,
                                change = stock.change,
                                changePercent = stock.changePercent,
                                onClick = { onNavigateToDetail(stock.symbol) },
                                modifier = Modifier.width(220.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showThemeSheet) {
            ThemeCustomizerSheet(onDismissRequest = { showThemeSheet = false })
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
