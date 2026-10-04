package com.example.data

data class StoryModel(
    val storyId: String = "",
    val ownerId: String = "",
    val username: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image", // "image" or "video"
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L,
    val visibility: String = "Todo el mundo", // "Todo el mundo", "Amigos", "Solo yo", "Personalizado"
    val excludedUsers: List<String> = emptyList(),
    val allowedUsers: List<String> = emptyList(),
    val soundId: String? = null,
    val soundName: String? = null,
    val isSharedVideo: Boolean = false,
    val originalVideoId: String? = null
)
