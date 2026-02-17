package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("score") val score: Int
)

@Serializable
data class LeaderboardResponse(
    val period: String,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val entries: List<LeaderboardEntry>
)
