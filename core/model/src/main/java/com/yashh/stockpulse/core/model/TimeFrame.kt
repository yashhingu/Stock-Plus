package com.yashh.stockpulse.core.model

enum class TimeFrame(val label: String, val intervalMinutes: Int) {
    ONE_DAY("1D", 1),
    ONE_WEEK("1W", 5),
    ONE_MONTH("1M", 60),
    THREE_MONTHS("3M", 1440),
    ONE_YEAR("1Y", 1440 * 7),
    ALL("ALL", 1440 * 30)
}
