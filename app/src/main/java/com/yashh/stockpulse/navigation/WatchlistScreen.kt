package com.yashh.stockpulse.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yashh.stockpulse.core.database.dao.TickerDao
import com.yashh.stockpulse.core.database.dao.WatchlistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistRow(val symbol: String, val companyName: String, val price: Double?)

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistDao: WatchlistDao,
    tickerDao: TickerDao
) : ViewModel() {
    val rows = combine(watchlistDao.getWatchlist(), tickerDao.getAllTickers()) { watchlist, tickers ->
        val prices = tickers.associateBy { it.symbol }
        watchlist.map { item -> WatchlistRow(item.symbol, item.companyName, prices[item.symbol]?.currentPrice) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(symbol: String) {
        viewModelScope.launch { watchlistDao.removeFromWatchlist(symbol) }
    }
}

@Composable
fun WatchlistScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    if (rows.isEmpty()) {
        Text("Your watchlist is empty. Star a stock to add it.", modifier = Modifier.padding(24.dp))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rows, key = { it.symbol }) { row ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onNavigateToDetail(row.symbol) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(row.symbol, style = MaterialTheme.typography.titleMedium)
                        Text(row.companyName, style = MaterialTheme.typography.bodySmall)
                        row.price?.let { Text("$${"%.2f".format(it)}") }
                    }
                    IconButton(onClick = { viewModel.remove(row.symbol) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove ${row.symbol} from watchlist")
                    }
                }
            }
        }
    }
}
