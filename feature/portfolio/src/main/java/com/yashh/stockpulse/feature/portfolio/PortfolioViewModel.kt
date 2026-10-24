package com.yashh.stockpulse.feature.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yashh.stockpulse.core.database.dao.AccountDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val accountDao: AccountDao
) : ViewModel() {

    private val _state = MutableStateFlow(PortfolioState())
    val state: StateFlow<PortfolioState> = _state.asStateFlow()

    init {
        observeCash()
        processIntent(PortfolioIntent.LoadPortfolio)
    }

    private fun observeCash() {
        viewModelScope.launch {
            accountDao.getAccount().collect { account ->
                val cash = account?.cashBalance ?: 50000.0
                _state.update { curr ->
                    val equity = curr.stockEquity
                    curr.copy(
                        availableCash = cash,
                        totalValue = equity + cash
                    )
                }
            }
        }
    }

    fun processIntent(intent: PortfolioIntent) {
        when (intent) {
            is PortfolioIntent.LoadPortfolio -> loadPortfolio()
            is PortfolioIntent.RefreshPrices -> {
                viewModelScope.launch { repository.refreshPrices() }
            }
            is PortfolioIntent.AddHolding -> {
                viewModelScope.launch {
                    val cost = intent.qty * intent.price
                    accountDao.updateCash(-cost)
                    repository.addHolding(intent.symbol, intent.qty, intent.price)
                }
            }
            is PortfolioIntent.RemoveHolding -> {
                viewModelScope.launch {
                    val holding = _state.value.holdings.firstOrNull { it.symbol == intent.symbol }
                    if (holding != null) {
                        accountDao.updateCash(holding.totalValue)
                    }
                    repository.removeHolding(intent.symbol)
                }
            }
        }
    }

    private fun loadPortfolio() {
        viewModelScope.launch {
            repository.getAllHoldings().collect { holdings ->
                val stockEquity = holdings.sumOf { it.totalValue }
                val totalInv = holdings.sumOf { it.totalCost }
                val pnl = stockEquity - totalInv
                val pnlPct = if (totalInv > 0) (pnl / totalInv) * 100 else 0.0

                _state.update { curr ->
                    val cash = curr.availableCash
                    curr.copy(
                        isLoading = false,
                        holdings = holdings.toImmutableList(),
                        stockEquity = stockEquity,
                        totalValue = stockEquity + cash,
                        totalInvested = totalInv,
                        totalPnL = pnl,
                        totalPnLPercent = pnlPct,
                        dailyChange = 0.0
                    )
                }
            }
        }
    }
}
