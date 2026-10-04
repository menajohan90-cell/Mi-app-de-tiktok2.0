package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun connectUser(targetUid: String): Result<ConnectionResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            if (currentUid == targetUid) return Result.failure(Exception("No puedes conectar contigo mismo"))

            val connectionId = if (currentUid < targetUid) "${currentUid}_$targetUid" else "${targetUid}_$currentUid"
            
            // Check sender profile to create real notification
            val senderProfile = getProfile(currentUid).getOrNull()
            val senderUsername = senderProfile?.username?.ifBlank { null }
                ?: auth.currentUser?.email?.substringBefore("@")
                ?: "usuario"
            val senderDisplayName = senderProfile?.displayName?.ifBlank { null }
                ?: auth.currentUser?.displayName
                ?: senderUsername
            val senderPhoto = senderProfile?.photoUrl ?: auth.currentUser?.photoUrl?.toString() ?: ""

            val connection = ConnectionResponse(
                connectionId = connectionId,
                requesterUid = currentUid,
                targetUid = targetUid,
                status = "accepted",
                createdAt = System.currentTimeMillis()
            )
            
            // 1. Save connection in Firestore
            db.collection("connections").document(connectionId).set(connection).await()

            // 2. Create REAL notification for recipient in system_notifications
            try {
                val notifId = "conn_${currentUid}_${targetUid}"
                val notifData = hashMapOf(
                    "id" to notifId,
                    "uid" to targetUid, // recipient
                    "type" to "connection",
                    "title" to "Nueva conexión",
                    "message" to "$senderDisplayName (@$senderUsername) conectó contigo",
                    "senderUid" to currentUid,
                    "senderUsername" to senderUsername,
                    "senderDisplayName" to senderDisplayName,
                    "senderPhotoUrl" to senderPhoto,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("system_notifications").document(notifId).set(notifData).await()
            } catch (e: Exception) {
                android.util.Log.w("UserRepository", "Could not send notification: ${e.message}")
            }

            Result.success(connection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnectUser(targetUid: String): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val connectionId = if (currentUid < targetUid) "${currentUid}_$targetUid" else "${targetUid}_$currentUid"

            db.collection("connections").document(connectionId).delete().await()

            try {
                val notifId1 = "conn_${currentUid}_${targetUid}"
                val notifId2 = "conn_${targetUid}_${currentUid}"
                db.collection("system_notifications").document(notifId1).delete().await()
                db.collection("system_notifications").document(notifId2).delete().await()
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnectionWith(targetUid: String): Result<ConnectionResponse?> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val connectionId = if (currentUid < targetUid) "${currentUid}_$targetUid" else "${targetUid}_$currentUid"
            val doc = db.collection("connections").document(connectionId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(ConnectionResponse::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConnections(): Result<List<ConnectionResponse>> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            
            val connections1 = db.collection("connections").whereEqualTo("requesterUid", currentUid).get().await()
            val connections2 = db.collection("connections").whereEqualTo("targetUid", currentUid).get().await()
            
            val allDocs = (connections1.documents + connections2.documents).distinctBy { it.id }
            val allConnections = mutableListOf<ConnectionResponse>()

            for (doc in allDocs) {
                val conn = doc.toObject(ConnectionResponse::class.java)
                if (conn != null && (conn.status == "accepted" || conn.status == "connected" || conn.status == "CONNECTED")) {
                    val otherUid = if (conn.requesterUid == currentUid) conn.targetUid else conn.requesterUid
                    val otherProfile = getProfile(otherUid).getOrNull()
                    val otherBrief = otherProfile?.let {
                        UserBrief(uid = it.uid, username = it.username, displayName = it.displayName, photoUrl = it.photoUrl)
                    }
                    val populatedConn = if (conn.requesterUid == currentUid) {
                        conn.copy(target = otherBrief)
                    } else {
                        conn.copy(requester = otherBrief)
                    }
                    allConnections.add(populatedConn)
                }
            }
                
            Result.success(allConnections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConnectionStatus(connectionId: String, status: String): Result<ConnectionResponse> {
        return try {
            db.collection("connections").document(connectionId).update("status", status).await()
            val doc = db.collection("connections").document(connectionId).get().await()
            val conn = doc.toObject(ConnectionResponse::class.java)!!
            Result.success(conn)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(uid: String): Result<ProfileResponse> {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val profile = doc.toObject(ProfileResponse::class.java) ?: ProfileResponse(uid = uid)
                Result.success(profile)
            } else {
                val current = auth.currentUser
                if (current != null && current.uid == uid) {
                    val email = current.email ?: ""
                    val fallbackName = current.displayName ?: if (email.isNotEmpty()) email.substringBefore("@") else "Usuario"
                    val fallbackUser = if (email.isNotEmpty()) email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "") else "user_${uid.take(5)}"
                    val autoProfile = ProfileResponse(
                        uid = uid,
                        username = fallbackUser,
                        displayName = fallbackName,
                        photoUrl = current.photoUrl?.toString() ?: "",
                        email = email
                    )
                    Result.success(autoProfile)
                } else {
                    Result.success(ProfileResponse(uid = uid))
                }
            }
        } catch (e: Exception) {
            val current = auth.currentUser
            if (current != null && current.uid == uid) {
                val email = current.email ?: ""
                val fallbackName = current.displayName ?: if (email.isNotEmpty()) email.substringBefore("@") else "Usuario"
                val fallbackUser = if (email.isNotEmpty()) email.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "") else "user_${uid.take(5)}"
                Result.success(ProfileResponse(
                    uid = uid,
                    username = fallbackUser,
                    displayName = fallbackName,
                    photoUrl = current.photoUrl?.toString() ?: "",
                    email = email
                ))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateProfile(username: String, displayName: String): Result<ProfileResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val user = auth.currentUser
            val updates = mutableMapOf<String, Any>(
                "uid" to currentUid,
                "username" to username.trim().lowercase().replace("@", ""),
                "displayName" to displayName.trim()
            )
            if (user?.photoUrl != null) {
                updates["photoUrl"] = user.photoUrl.toString()
            }
            if (!user?.email.isNullOrBlank()) {
                updates["email"] = user?.email!!
            }
            db.collection("users").document(currentUid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            getProfile(currentUid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<UserBrief>> {
        val currentUid = auth.currentUser?.uid
        val cleanQ = query.trim().lowercase().removePrefix("@")
        if (cleanQ.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            val snapshot = try {
                db.collection("users")
                    .limit(60)
                    .get()
                    .await()
            } catch (e: Exception) {
                db.collection("users")
                    .orderBy("username")
                    .startAt(cleanQ)
                    .endAt(cleanQ + "\uf8ff")
                    .limit(30)
                    .get()
                    .await()
            }
                
            val users = snapshot.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: doc.id
                if (uid == currentUid) return@mapNotNull null // Don't return current user in search

                val username = doc.getString("username") ?: ""
                val displayName = doc.getString("displayName") ?: username
                val photoUrl = doc.getString("photoUrl") ?: ""

                val uLower = username.lowercase()
                val dLower = displayName.lowercase()

                if (uLower.contains(cleanQ) || dLower.contains(cleanQ)) {
                    UserBrief(
                        uid = uid,
                        username = username.ifEmpty { "usuario" },
                        displayName = displayName.ifEmpty { username.ifEmpty { "Usuario" } },
                        photoUrl = photoUrl
                    )
                } else null
            }.sortedWith(
                compareBy(
                    { !it.username.equals(cleanQ, ignoreCase = true) },
                    { !it.username.startsWith(cleanQ, ignoreCase = true) },
                    { !it.displayName.startsWith(cleanQ, ignoreCase = true) }
                )
            )

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setUserBadges(targetUid: String, isVerified: Boolean, isSinger: Boolean, isCrown: Boolean): Result<Unit> {
        return try {
            val data = mapOf(
                "isVerified" to isVerified,
                "isSinger" to isSinger,
                "isCrown" to isCrown
            )
            db.collection("users").document(targetUid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
