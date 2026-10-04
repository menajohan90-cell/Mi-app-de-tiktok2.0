package com.example.data

data class AppUpdateModel(
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val releaseDate: String = "",
    val mandatory: Boolean = false,
    val minimumVersion: Int = 1,
    val fileSize: String = "",
    val updateType: String = "Actualización",
    val securityPatch: Boolean = false
)
