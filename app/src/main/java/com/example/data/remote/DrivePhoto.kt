package com.example.data.remote

data class DrivePhoto(
    val id: String,
    val name: String,
    val mimeType: String = "image/jpeg",
    val url: String,
    val downloadUrl: String = "",
    val createdTime: Long = System.currentTimeMillis()
)
