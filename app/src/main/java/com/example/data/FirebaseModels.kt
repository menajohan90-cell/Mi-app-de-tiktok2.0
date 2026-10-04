package com.example.data

data class CommentModel(
    val commentId: String = "",
    val videoId: String = "",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)

data class ConnectionModel(
    val connectionId: String = "",
    val requesterUid: String = "",
    val targetUid: String = "",
    val status: String = "pending", // pending, accepted
    val createdAt: Long = 0L
)

data class ProfileModel(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0
)

data class UserBriefModel(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
)
