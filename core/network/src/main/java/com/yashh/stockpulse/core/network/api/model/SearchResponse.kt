package com.yashh.stockpulse.core.network.api.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("result") val result: List<SearchResult>? = null
)

data class SearchResult(
    @SerializedName("description") val description: String = "",
    @SerializedName("displaySymbol") val displaySymbol: String = "",
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("type") val type: String = ""
)
