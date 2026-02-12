package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSummary(
    val name: String,
    val id: String
)

@Serializable
data class BikeOwner(
    val user: UserSummary,
    @SerialName("submission_count") val submissionCount: Int
)

@Serializable
data class BikeListItem(
    @SerialName("bike_qr_id") val bikeQrId: String,
    val provider: String? = null,
    @SerialName("submission_count") val submissionCount: Int,
    val owner: UserSummary? = null
)

@Serializable
data class PaginatedBikes(
    val items: List<BikeListItem>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class BikeDetailResponse(
    @SerialName("bike_qr_id") val bikeQrId: String,
    val provider: String? = null,
    @SerialName("bike_brand") val bikeBrand: String? = null,
    @SerialName("first_seen_at") val firstSeenAt: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    val notes: String? = null,
    @SerialName("submission_count") val submissionCount: Int,
    val owners: List<BikeOwner> = emptyList(),
    @SerialName("first_captured_by") val firstCapturedBy: UserSummary? = null,
    @SerialName("last_captured_by") val lastCapturedBy: UserSummary? = null
)
