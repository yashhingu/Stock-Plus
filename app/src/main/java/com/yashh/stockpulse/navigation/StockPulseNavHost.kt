package com.yashh.stockpulse.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yashh.stockpulse.feature.dashboard.DashboardScreen
import com.yashh.stockpulse.feature.portfolio.PortfolioScreen
import com.yashh.stockpulse.feature.stockdetail.StockDetailScreen

@Composable
fun StockPulseNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        TopLevelDestination.Dashboard,
        TopLevelDestination.Portfolio,
        TopLevelDestination.Watchlist
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Dashboard.route,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(TopLevelDestination.Dashboard.route) {
                DashboardScreen(onNavigateToDetail = { symbol ->
                    navController.navigate("stock/$symbol")
                })
            }
            composable(TopLevelDestination.Portfolio.route) {
                PortfolioScreen()
            }
            composable(TopLevelDestination.Watchlist.route) {
                WatchlistScreen(onNavigateToDetail = { symbol -> navController.navigate("stock/$symbol") })
            }
            composable("stock/{symbol}") {
                StockDetailScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}
