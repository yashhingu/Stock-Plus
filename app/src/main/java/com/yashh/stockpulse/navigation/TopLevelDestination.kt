package com.yashh.stockpulse.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TopLevelDestination(val route: String, val icon: ImageVector, val label: String) {
    object Dashboard : TopLevelDestination("dashboard", Icons.Filled.TrendingUp, "Dashboard")
    object Portfolio : TopLevelDestination("portfolio", Icons.Filled.PieChart, "Portfolio")
    object Watchlist : TopLevelDestination("watchlist", Icons.Filled.Star, "Watchlist")
}
