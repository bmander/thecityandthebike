package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlagStatusResponse(
    val flagged: Boolean,
    @SerialName("flag_count") val flagCount: Int
)
