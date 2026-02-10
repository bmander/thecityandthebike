package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BikeDetailResponse(
    @SerialName("bike_qr_id") val bikeQrId: String,
    val provider: String? = null,
    @SerialName("bike_brand") val bikeBrand: String? = null,
    @SerialName("first_seen_at") val firstSeenAt: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    val notes: String? = null,
    @SerialName("submission_count") val submissionCount: Int
)
