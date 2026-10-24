package com.yashh.stockpulse.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yashh.stockpulse.core.designsystem.theme.LocalTradingColors
import kotlinx.coroutines.isActive

data class MarqueeItem(
    val symbol: String,
    val price: Double,
    val changePercent: Double
)

@Composable
fun TickerMarquee(
    items: List<MarqueeItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    // TODO: Optimize this, maybe use layout phase or standard marquee modifier
    val scrollState = rememberScrollState()
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        while (isActive && isRunning) {
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(
                    durationMillis = 20000,
                    easing = LinearEasing
                )
            )
            scrollState.scrollTo(0)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(scrollState, enabled = false)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            TickerItem(item)
            Spacer(modifier = Modifier.width(24.dp))
        }
        // Duplicate items to make it loop smoothly
        items.forEach { item ->
            TickerItem(item)
            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
private fun TickerItem(item: MarqueeItem) {
    val color = if (item.changePercent >= 0) {
        LocalTradingColors.current.bull
    } else {
        LocalTradingColors.current.bear
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = item.symbol,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format("%.2f", item.price),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${if(item.changePercent >= 0) "+" else ""}${String.format("%.2f", item.changePercent)}%",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
