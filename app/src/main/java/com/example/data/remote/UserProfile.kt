package com.example.data.remote

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val totalCurhat: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
