package com.yashh.stockpulse.feature.stockdetail.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CrosshairOverlay(
    open: Double,
    high: Double,
    low: Double,
    close: Double,
    volume: Long,
    date: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.DarkGray.copy(alpha = 0.8f))
            .padding(8.dp) // hardcoded for now
    ) {
        Text("Date: $date", color = Color.White)
        Text("O: ${formatPrice(open)}", color = Color.White)
        Text("H: ${formatPrice(high)}", color = Color.White)
        Text("L: ${formatPrice(low)}", color = Color.White)
        Text("C: ${formatPrice(close)}", color = Color.White)
        Text("Vol: ${formatVolume(volume)}", color = Color.White)
    }
}
