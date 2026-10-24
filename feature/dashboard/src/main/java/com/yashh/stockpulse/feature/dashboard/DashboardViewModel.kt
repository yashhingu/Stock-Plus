package com.yashh.stockpulse.feature.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<DashboardEffect>()
    val effect: SharedFlow<DashboardEffect> = _effect.asSharedFlow()

    private val popularSymbols = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA", "NVDA", "META", "NFLX")

    init {
        processIntent(DashboardIntent.LoadMarketData)
    }

    fun processIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.LoadMarketData -> loadMarketData()
            is DashboardIntent.RefreshData -> {
                _state.update { it.copy(isLoading = true) }
                loadMarketData()
            }
            is DashboardIntent.SearchStock -> {
                // TODO: implement search
            }
            is DashboardIntent.AddToWatchlist -> {
                Log.d("DashboardVM", "Adding ${intent.symbol} to watchlist")
                // repository call
            }
            is DashboardIntent.RemoveFromWatchlist -> {
                // repository call
            }
        }
    }

    private fun loadMarketData() {
        viewModelScope.launch {
            try {
                repository.fetchMarketOverview(popularSymbols).collect { quotes ->
                    // mock split
                    val sorted = quotes.sortedByDescending { it.changePercent }
                    val gainers = sorted.take(4).toImmutableList()
                    val losers = sorted.reversed().take(4).toImmutableList()

                    _state.update {
                        it.copy(
                            isLoading = false,
                            topGainers = gainers,
                            topLosers = losers,
                            tickerTape = quotes.toImmutableList(),
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error loading data", e)
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }
}
