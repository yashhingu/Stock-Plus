package com.yashh.stockpulse.core.network.api.model

import com.google.gson.annotations.SerializedName

data class CandleResponse(
    @SerializedName("c") val c: List<Double>? = null,
    @SerializedName("h") val h: List<Double>? = null,
    @SerializedName("l") val l: List<Double>? = null,
    @SerializedName("o") val o: List<Double>? = null,
    @SerializedName("s") val s: String = "",
    @SerializedName("t") val t: List<Long>? = null,
    @SerializedName("v") val v: List<Long>? = null
)
