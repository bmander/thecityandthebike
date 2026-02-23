package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_banned") val isBanned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LeaderboardRank(
    val period: String,
    val rank: Int? = null
)

@Serializable
data class UserDetailResponse(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_banned") val isBanned: Boolean = false,
    @SerialName("submission_count") val submissionCount: Int,
    @SerialName("first_seen_at") val firstSeenAt: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("owned_bike_count") val ownedBikeCount: Int,
    @SerialName("leaderboard_ranks") val leaderboardRanks: List<LeaderboardRank>
)

@Serializable
data class BanResponse(
    val msg: String,
    @SerialName("is_banned") val isBanned: Boolean,
    @SerialName("device_banned") val deviceBanned: Boolean = false
)
