package com.example.data

data class CommentResponse(
    val commentId: String = "",
    val videoId: String = "",
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val isLiked: Boolean = false,
    val likesCount: Int = 0,
    val userProfilePic: String = "", val replies: List<CommentResponse> = emptyList()
)

data class ConnectionResponse(
    val connectionId: String = "",
    val requesterUid: String = "",
    val targetUid: String = "",
    val status: String = "pending", // pending, accepted
    val createdAt: Long = 0L,
    val requester: UserBrief? = null,
    val target: UserBrief? = null
)

data class ProfileResponse(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val suspended: Boolean = false,
    val suspensionReason: String? = null,
    val moderationStatus: String? = null,
    val accountType: String = "USER",
    val email: String = "",
    val isVerified: Boolean = false,
    val isSinger: Boolean = false,
    val isCrown: Boolean = false,
    val isCreator: Boolean = false,
    val suspendedUntil: Long = 0L
)

data class UserBrief(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
)

data class ChatBrief(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val otherUser: UserBrief? = null,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0
)

data class MessageResponse(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = 0L,
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val readAt: Long = 0L
)

data class StoryResponse(
    val storyId: String = "",
    val uid: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val mediaType: String = "",
    val createdAt: Long = 0L,
    val expiresAt: Long = 0L, val soundName: String = "",
    val isViewed: Boolean = false
)

data class LikeResponse(
    val isLiked: Boolean = false,
    val likesCount: Int = 0
)

data class ReportModel(
    val reportId: String = "",
    val videoId: String = "",
    val reporterUid: String = "",
    val reportedUserId: String = "",
    val reason: String = "",
    val description: String? = null,
    val status: String = "reviewing_report", // reviewing_report, reviewing_publication, resolved
    val systemResult: String = "", // accepted, rejected
    val systemAnalysis: String = "El sistema está revisando tu denuncia. Espere por favor.",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val resolvedAt: Long = 0L
)
