package com.yashh.stockpulse.feature.stockdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.yashh.stockpulse.core.database.dao.CandleDao
import com.yashh.stockpulse.core.database.dao.WatchlistDao
import com.yashh.stockpulse.core.model.StockQuote
import com.yashh.stockpulse.core.model.TimeFrame
import com.yashh.stockpulse.core.network.websocket.WebSocketMessage
import com.yashh.stockpulse.core.network.websocket.StockTick
import com.yashh.stockpulse.core.network.websocket.StockWebSocketClient
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.coVerify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [StockDetailViewModel] verifying MVI state transitions,
 * WebSocket message handling, intent processing, and data fetching logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private lateinit var webSocketClient: StockWebSocketClient
    private lateinit var repository: StockDetailRepository
    private lateinit var candleDao: CandleDao
    private lateinit var watchlistDao: WatchlistDao
    private lateinit var savedStateHandle: SavedStateHandle

    private lateinit var viewModel: StockDetailViewModel
    private val webSocketFlow = MutableSharedFlow<WebSocketMessage>()
    private val tickFlow = MutableSharedFlow<StockTick>(extraBufferCapacity = 64)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        webSocketClient = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        candleDao = mockk(relaxed = true)
        watchlistDao = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle(mapOf("symbol" to "AAPL"))

        every { webSocketClient.messages } returns webSocketFlow
        every { webSocketClient.tickFlow } returns tickFlow
        every { repository.getHistoricalCandles(any(), any()) } returns flowOf(emptyList())
        coEvery { repository.getInitialQuote(any()) } returns StockQuote("AAPL", 150.0, 123456789L)
        coEvery { watchlistDao.isSymbolInWatchlist(any()) } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================================================
    // Initial State Tests
    // ========================================================================

    /**
     * Verifies that the ViewModel starts in the Loading state.
     */
    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)
        assertEquals(StockDetailState.Loading, viewModel.state.value)
    }

    /**
     * Verifies that successful data fetch transitions the state to Connected.
     */
    @Test
    fun `successful connection transitions to Connected state`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)

        viewModel.state.test {
            // First emission might be Loading, skip or assert
            val initialState = awaitItem()
            if (initialState is StockDetailState.Loading) {
                val connectedState = awaitItem()
                assertTrue(connectedState is StockDetailState.Connected)
                assertEquals("AAPL", (connectedState as StockDetailState.Connected).quote.symbol)
            } else {
                assertTrue(initialState is StockDetailState.Connected)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================================================
    // WebSocket Tests
    // ========================================================================

    /**
     * Verifies that rapid tick updates are conflated and properly update the state.
     */
    @Test
    fun `rapid-fire ticks are properly conflated`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)

        // Wait for Connected state
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val connectedState = awaitItem() as StockDetailState.Connected

            // Emit rapid ticks
            tickFlow.emit(StockTick("AAPL", 151.0, 0.0, 1L))
            tickFlow.emit(StockTick("AAPL", 152.0, 0.0, 2L))
            testScheduler.advanceUntilIdle()
            val updatedState = expectMostRecentItem() as StockDetailState.Connected
            assertEquals(152.0, updatedState.quote.currentPrice, 0.01)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifies that Disconnected message transitions state to Reconnecting.
     */
    @Test
    fun `reconnecting state on disconnect`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            awaitItem() // Connected state

            webSocketFlow.emit(WebSocketMessage.Disconnected())

            val state = awaitItem()
            assertTrue(state is StockDetailState.Reconnecting)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifies that Error message transitions state to Error.
     */
    @Test
    fun `error state on connection failure`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            awaitItem() // Connected state

            webSocketFlow.emit(WebSocketMessage.Error("Network failure"))

            val state = awaitItem()
            assertTrue(state is StockDetailState.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========================================================================
    // Intent Tests
    // ========================================================================

    /**
     * Verifies that selecting a timeframe reloads the data.
     */
    @Test
    fun `timeframe change triggers candle reload`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(StockDetailIntent.SelectTimeFrame(TimeFrame.ONE_WEEK))
        testScheduler.advanceUntilIdle()

        coVerify { repository.getHistoricalCandles("AAPL", TimeFrame.ONE_WEEK) }
    }

    /**
     * Verifies that toggling watchlist intent calls the DAO appropriately.
     */
    @Test
    fun `watchlist toggle works`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)
        testScheduler.advanceUntilIdle() // Ensure we are connected and not in watchlist initially

        viewModel.processIntent(StockDetailIntent.ToggleWatchlist("AAPL"))
        testScheduler.advanceUntilIdle()

        coVerify { watchlistDao.addToWatchlist("AAPL") }
    }

    /**
     * Verifies that toggling indicators updates state correctly.
     */
    @Test
    fun `indicator toggling updates state`() = runTest {
        viewModel = StockDetailViewModel(savedStateHandle, webSocketClient, repository, candleDao, watchlistDao)
        testScheduler.advanceUntilIdle()

        viewModel.processIntent(StockDetailIntent.ToggleRsi)
        var state = viewModel.state.value as StockDetailState.Connected
        assertTrue(state.showRsi)

        viewModel.processIntent(StockDetailIntent.ToggleVolume)
        state = viewModel.state.value as StockDetailState.Connected
        assertEquals(false, state.showVolume)
    }
}
