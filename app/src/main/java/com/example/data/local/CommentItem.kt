package com.example.data.local

data class CommentItem(
    val commentId: String = "",
    val noteDocId: String = "",
    val parentCommentId: String? = null,
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
