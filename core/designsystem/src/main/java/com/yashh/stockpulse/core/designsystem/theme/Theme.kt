package com.yashh.stockpulse.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TradingPalette(val label: String, val description: String) {
    WESTERN("Western Standard", "Green Bull / Red Bear"),
    EASTERN("Asian Traditional", "Red Bull / Green Bear"),
    COLORBLIND("Accessible High-Contrast", "Blue Bull / Orange Bear")
}

enum class AppThemeMode(val label: String) {
    DARK("Pro Dark"),
    OLED_TERMINAL("OLED Terminal"),
    LIGHT("Clean Light")
}

object ThemeManager {
    private val _themeMode = MutableStateFlow(AppThemeMode.DARK)
    val themeMode = _themeMode.asStateFlow()

    private val _tradingPalette = MutableStateFlow(TradingPalette.WESTERN)
    val tradingPalette = _tradingPalette.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun setTradingPalette(palette: TradingPalette) {
        _tradingPalette.value = palette
    }
}

class TradingColors(
    val bull: Color,
    val bear: Color,
    val chartBg: Color,
    val chartGrid: Color,
    val rsiLine: Color = IndicatorPurple,
    val volumeBull: Color = bull.copy(alpha = 0.55f),
    val volumeBear: Color = bear.copy(alpha = 0.55f)
)

val LocalTradingColors = staticCompositionLocalOf {
    TradingColors(
        bull = BullGreen,
        bear = BearRed,
        chartBg = ChartBackground,
        chartGrid = ChartGrid
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = BullGreenDark,
    tertiary = BearRedDark,
    background = ChartBackground,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    primaryContainer = Color(0xFF194B50),
    secondaryContainer = Color(0xFF194B50),
    onPrimary = Color(0xFF062421),
    onSecondary = Color(0xFF062421),
    onSecondaryContainer = TextPrimary,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF486173)
)

private val OledColorScheme = darkColorScheme(
    primary = OledCyanAccent,
    secondary = BullGreenDark,
    tertiary = BearRedDark,
    background = OledBackground,
    surface = OledSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFE4E4E4)
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = BullGreen,
    tertiary = BearRed,
    background = Color(0xFFF8F9FA),
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

@Composable
fun StockPulseTheme(
    themeModeOverride: AppThemeMode? = null,
    tradingPaletteOverride: TradingPalette? = null,
    content: @Composable () -> Unit
) {
    val activeThemeMode by ThemeManager.themeMode.collectAsState()
    val activePalette by ThemeManager.tradingPalette.collectAsState()

    val currentTheme = themeModeOverride ?: activeThemeMode
    val currentPalette = tradingPaletteOverride ?: activePalette

    val colorScheme = when (currentTheme) {
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.OLED_TERMINAL -> OledColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
    }

    val (bullColor, bearColor) = when (currentPalette) {
        TradingPalette.WESTERN -> BullGreen to BearRed
        TradingPalette.EASTERN -> EasternBullRed to EasternBearGreen
        TradingPalette.COLORBLIND -> AccessibleBullBlue to AccessibleBearOrange
    }

    val chartBg = when (currentTheme) {
        AppThemeMode.DARK -> ChartBackground
        AppThemeMode.OLED_TERMINAL -> OledBackground
        AppThemeMode.LIGHT -> Color(0xFFFFFFFF)
    }

    val chartGrid = when (currentTheme) {
        AppThemeMode.DARK -> ChartGrid
        AppThemeMode.OLED_TERMINAL -> OledGrid
        AppThemeMode.LIGHT -> Color(0xFFE0E0E0)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                (currentTheme == AppThemeMode.LIGHT)
        }
    }

    val tradingColors = TradingColors(
        bull = bullColor,
        bear = bearColor,
        chartBg = chartBg,
        chartGrid = chartGrid,
        rsiLine = IndicatorPurple,
        volumeBull = bullColor.copy(alpha = 0.55f),
        volumeBear = bearColor.copy(alpha = 0.55f)
    )

    CompositionLocalProvider(LocalTradingColors provides tradingColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
