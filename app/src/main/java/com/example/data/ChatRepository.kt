package com.example.data

import com.example.utils.ContentModerator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun checkMutualConnection(partnerUid: String): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        if (currentUid == partnerUid) return true
        val connId = if (currentUid < partnerUid) "${currentUid}_$partnerUid" else "${partnerUid}_$currentUid"
        
        try {
            val doc = db.collection("connections").document(connId).get().await()
            if (doc.exists()) {
                val status = doc.getString("status") ?: ""
                return status.equals("accepted", ignoreCase = true) || status.equals("connected", ignoreCase = true)
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatRepository", "Error checking connection: ${e.message}")
        }
        return false
    }

    fun listenMessages(chatId: String, onUpdate: (List<MessageResponse>) -> Unit): ListenerRegistration {
        return db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { it.toObject(MessageResponse::class.java) }
                onUpdate(messages)
            }
    }

    suspend fun getMessages(chatId: String): Result<List<MessageResponse>> {
        return try {
            val snapshot = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
            val messages = snapshot.documents.mapNotNull { it.toObject(MessageResponse::class.java) }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String, partnerUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            val unreadSnapshot = db.collection("chats").document(chatId)
                .collection("messages")
                .whereEqualTo("senderId", partnerUid)
                .whereEqualTo("isRead", false)
                .get().await()

            if (!unreadSnapshot.isEmpty) {
                val batch = db.batch()
                for (doc in unreadSnapshot.documents) {
                    batch.update(doc.reference, mapOf("isRead" to true, "readAt" to System.currentTimeMillis()))
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            android.util.Log.w("ChatRepository", "Error marking messages as read: ${e.message}")
        }
    }

    suspend fun sendMessage(chatId: String, text: String, imageUrl: String? = null): Result<MessageResponse> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        
        val (allowed, reason) = ContentModerator.isContentAllowed(text)
        if (!allowed) {
            return Result.failure(Exception(reason))
        }
        if (!imageUrl.isNullOrBlank()) {
            val (mediaAllowed, mediaReason) = ContentModerator.isMediaAllowed(imageUrl)
            if (!mediaAllowed) {
                return Result.failure(Exception(mediaReason))
            }
        }

        return try {
            // Clear typing state on send
            setTyping(chatId, false)

            val messageId = UUID.randomUUID().toString()
            val message = MessageResponse(
                messageId = messageId,
                chatId = chatId,
                senderId = currentUid,
                text = text,
                createdAt = System.currentTimeMillis(),
                imageUrl = imageUrl ?: "",
                isRead = false,
                readAt = 0L
            )

            // 1. Guardar mensaje
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .set(message).await()

            // 2. Resolver participantes
            val parts = if (chatId.contains("_")) chatId.split("_") else listOf(currentUid)
            val partnerUid = parts.firstOrNull { it != currentUid } ?: ""
            val participants = if (partnerUid.isNotBlank()) listOf(currentUid, partnerUid) else listOf(currentUid)

            val displayLastMessage = when {
                text.isNotBlank() -> text
                !imageUrl.isNullOrBlank() -> "[Foto]"
                else -> "[Mensaje]"
            }

            val chatUpdate = mapOf(
                "chatId" to chatId,
                "participants" to participants,
                "lastMessage" to displayLastMessage,
                "lastMessageAt" to System.currentTimeMillis(),
                "lastSenderId" to currentUid
            )
            db.collection("chats").document(chatId).set(chatUpdate, SetOptions.merge()).await()

            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setTyping(chatId: String, isTyping: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            val typingData = mapOf(
                "typing_$currentUid" to isTyping,
                "typingUpdatedAt" to System.currentTimeMillis()
            )
            db.collection("chats").document(chatId).set(typingData, SetOptions.merge()).await()
        } catch (e: Exception) {}
    }

    suspend fun getPartnerTyping(chatId: String, partnerUid: String): Boolean {
        try {
            val doc = db.collection("chats").document(chatId).get().await()
            if (doc.exists()) {
                val isTyping = doc.getBoolean("typing_$partnerUid") ?: false
                val updatedAt = doc.getLong("typingUpdatedAt") ?: 0L
                if (System.currentTimeMillis() - updatedAt > 6000) return false
                return isTyping
            }
        } catch (e: Exception) {}
        return false
    }

    suspend fun getChats(): Result<List<ChatBrief>> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val chatsMap = mutableMapOf<String, ChatBrief>()

            // 1. Obtener chats directos de la colección 'chats' donde participa el usuario
            try {
                val directChats = db.collection("chats")
                    .whereArrayContains("participants", currentUid)
                    .get().await()

                for (doc in directChats.documents) {
                    val participants = doc.get("participants") as? List<*> ?: emptyList<Any>()
                    val partnerUid = participants.mapNotNull { it?.toString() }.firstOrNull { it != currentUid } ?: ""
                    if (partnerUid.isNotBlank()) {
                        val userDoc = db.collection("users").document(partnerUid).get().await()
                        val username = userDoc.getString("username") ?: "usuario"
                        val displayName = userDoc.getString("displayName") ?: username
                        val photoUrl = userDoc.getString("photoUrl") ?: ""
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val lastMessageAt = doc.getLong("lastMessageAt") ?: System.currentTimeMillis()

                        val canonicalId = if (currentUid < partnerUid) "${currentUid}_$partnerUid" else "${partnerUid}_$currentUid"
                        chatsMap[canonicalId] = ChatBrief(
                            chatId = canonicalId,
                            participants = listOf(currentUid, partnerUid),
                            otherUser = UserBrief(
                                uid = partnerUid,
                                username = username,
                                displayName = displayName,
                                photoUrl = photoUrl
                            ),
                            lastMessage = if (lastMessage.isNotBlank()) lastMessage else "¡Conectados! Di hola a tu amigo",
                            lastMessageAt = lastMessageAt,
                            unreadCount = 0
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ChatRepository", "Direct chats lookup: ${e.message}")
            }

            // 2. Obtener chats a través de conexiones aceptadas
            val conn1 = db.collection("connections").whereEqualTo("requesterUid", currentUid).get().await()
            val conn2 = db.collection("connections").whereEqualTo("targetUid", currentUid).get().await()
            val allConns = (conn1.documents + conn2.documents).distinctBy { it.id }

            for (doc in allConns) {
                val status = doc.getString("status") ?: ""
                if (status.equals("accepted", ignoreCase = true) || status.equals("connected", ignoreCase = true)) {
                    val req = doc.getString("requesterUid") ?: ""
                    val tgt = doc.getString("targetUid") ?: ""
                    val partnerUid = if (req == currentUid) tgt else req
                    
                    if (partnerUid.isNotBlank()) {
                        val canonicalId = if (currentUid < partnerUid) "${currentUid}_$partnerUid" else "${partnerUid}_$currentUid"
                        
                        // Si ya se cargó desde 'chats', verificar si tiene datos más actualizados
                        if (!chatsMap.containsKey(canonicalId)) {
                            val userDoc = db.collection("users").document(partnerUid).get().await()
                            val username = userDoc.getString("username") ?: "usuario"
                            val displayName = userDoc.getString("displayName") ?: username
                            val photoUrl = userDoc.getString("photoUrl") ?: ""

                            // Revisar si existe documento de chat para obtener último mensaje real
                            var lastMsg = "¡Conectados! Di hola a tu amigo"
                            var lastMsgAt = System.currentTimeMillis()
                            try {
                                val chatDoc = db.collection("chats").document(canonicalId).get().await()
                                if (chatDoc.exists()) {
                                    val dbMsg = chatDoc.getString("lastMessage")
                                    val dbTime = chatDoc.getLong("lastMessageAt")
                                    if (!dbMsg.isNullOrBlank()) lastMsg = dbMsg
                                    if (dbTime != null && dbTime > 0) lastMsgAt = dbTime
                                }
                            } catch (e: Exception) {}

                            chatsMap[canonicalId] = ChatBrief(
                                chatId = canonicalId,
                                participants = listOf(currentUid, partnerUid),
                                otherUser = UserBrief(
                                    uid = partnerUid,
                                    username = username,
                                    displayName = displayName,
                                    photoUrl = photoUrl
                                ),
                                lastMessage = lastMsg,
                                lastMessageAt = lastMsgAt,
                                unreadCount = 0
                            )
                        }
                    }
                }
            }

            Result.success(chatsMap.values.sortedByDescending { it.lastMessageAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
