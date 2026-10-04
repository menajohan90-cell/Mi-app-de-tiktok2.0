package com.example.data

data class VideoModel(
    val videoId: String = "",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val videoUrl: String = "",
    val thumbnailURL: String = "",
    val description: String = "",
    val hashtags: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val views: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val status: String = "READY",
    val visibility: String = "PUBLIC",
    val isDraft: Boolean = false
)
