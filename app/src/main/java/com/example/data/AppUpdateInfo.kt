package com.example.data

data class AppUpdateInfo(
    val latestVersionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean
)
