package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BlockRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun blockUser(targetUid: String): Result<Unit> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val blockId = "${currentUid}_$targetUid"
            val blockData = mapOf(
                "blockerUid" to currentUid,
                "blockedUid" to targetUid,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("blocks").document(blockId).set(blockData).await()

            // Remove connection if exists
            val connId1 = "${currentUid}_$targetUid"
            val connId2 = "${targetUid}_$currentUid"
            db.collection("connections").document(connId1).delete().await()
            db.collection("connections").document(connId2).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isBlocked(targetUid: String): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        try {
            val b1 = db.collection("blocks").document("${currentUid}_$targetUid").get().await()
            val b2 = db.collection("blocks").document("${targetUid}_$currentUid").get().await()
            return b1.exists() || b2.exists()
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun saveChatCopy(chatId: String, messages: List<MessageResponse>, partnerName: String): Result<String> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val copyId = UUID.randomUUID().toString()
            val copyData = mapOf(
                "copyId" to copyId,
                "ownerUid" to currentUid,
                "chatId" to chatId,
                "partnerName" to partnerName,
                "messages" to messages.map { mapOf("senderId" to it.senderId, "text" to it.text, "imageUrl" to (it.imageUrl ?: ""), "createdAt" to it.createdAt) },
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("shared_chat_copies").document(copyId).set(copyData).await()
            val link = "https://ais-pre-jo2awl56tqipvrtb37xrx2-270659541135.us-west2.run.app/shared_chat_offline?id=$copyId"
            Result.success(link)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
