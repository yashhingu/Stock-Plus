package com.yashh.stockpulse.core.network.api.model

import com.google.gson.annotations.SerializedName

data class QuoteResponse(
    @SerializedName("c") val c: Double = 0.0,
    @SerializedName("d") val d: Double = 0.0,
    @SerializedName("dp") val dp: Double = 0.0,
    @SerializedName("h") val h: Double = 0.0,
    @SerializedName("l") val l: Double = 0.0,
    @SerializedName("o") val o: Double = 0.0,
    @SerializedName("pc") val pc: Double = 0.0,
    @SerializedName("t") val t: Long = 0
)
