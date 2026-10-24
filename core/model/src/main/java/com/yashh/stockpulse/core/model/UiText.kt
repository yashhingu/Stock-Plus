package com.yashh.stockpulse.core.model

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(val resId: Int, val args: List<Any> = emptyList()) : UiText()

    companion object {
        fun from(message: String): UiText = DynamicString(message)
    }
}
