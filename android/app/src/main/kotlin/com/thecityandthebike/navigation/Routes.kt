package com.thecityandthebike.navigation

import kotlinx.serialization.Serializable

@Serializable object Splash
@Serializable object Onboarding
@Serializable object Login
@Serializable object Register
@Serializable object Main
@Serializable object PrivacyCopyright
@Serializable object About
@Serializable object ScoreRules
@Serializable data class ImageDetail(val submissionId: String)
@Serializable data class Bike(val bikeQrId: String)
@Serializable data class BikeImageDetail(val submissionId: String)
@Serializable data class User(val userId: String)
@Serializable data class UserImageDetail(val submissionId: String)
@Serializable object Me
@Serializable data class MeImageDetail(val submissionId: String)
@Serializable object Scanner
@Serializable data class TagDetail(val tagId: String)
@Serializable data class TagImageDetail(val submissionId: String)
@Serializable data class PhotoCapture(val qrId: String, val side: String = "right")
@Serializable data class PhotoPreview(val qrId: String, val photoUri: String, val side: String = "right")
