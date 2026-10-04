package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun parseVideoDocument(doc: com.google.firebase.firestore.DocumentSnapshot): VideoModel? {
        return try {
            val model = doc.toObject(VideoModel::class.java)
            if (model != null) {
                val id = if (model.videoId.isNotBlank()) model.videoId else doc.id
                return model.copy(videoId = id)
            }
            null
        } catch (e: Exception) {
            try {
                val data = doc.data ?: return null
                val videoId = data["videoId"] as? String ?: doc.id
                val uid = data["uid"] as? String ?: ""
                val username = data["username"] as? String ?: ""
                val displayName = data["displayName"] as? String ?: username
                val videoUrl = (data["videoUrl"] ?: data["url"]) as? String ?: ""
                val thumbnailUrl = (data["thumbnailUrl"] ?: data["thumbnailURL"] ?: data["thumbnail"]) as? String ?: ""
                val description = (data["description"] ?: data["caption"]) as? String ?: ""
                val hashtags = (data["hashtags"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val createdAt = when (val c = data["createdAt"]) {
                    is Number -> c.toLong()
                    is com.google.firebase.Timestamp -> c.toDate().time
                    else -> 0L
                }
                val views = (data["views"] as? Number)?.toInt() ?: 0
                val likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0
                val commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0
                val sharesCount = (data["sharesCount"] as? Number)?.toInt() ?: 0
                val status = data["status"] as? String ?: "READY"
                val visibility = data["visibility"] as? String ?: "PUBLIC"
                val isDraft = data["isDraft"] as? Boolean ?: false

                VideoModel(
                    videoId = videoId,
                    uid = uid,
                    username = username,
                    displayName = displayName,
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl,
                    description = description,
                    hashtags = hashtags,
                    createdAt = createdAt,
                    views = views,
                    likesCount = likesCount,
                    commentsCount = commentsCount,
                    sharesCount = sharesCount,
                    isLiked = false,
                    status = status,
                    visibility = visibility,
                    isDraft = isDraft
                )
            } catch (e2: Exception) {
                android.util.Log.w("PostRepository", "Failed to parse video document ${doc.id}: ${e2.message}")
                null
            }
        }
    }

    suspend fun getVideos(feedType: String): Result<List<VideoModel>> {
        val currentUser = auth.currentUser
        if (currentUser == null) return Result.failure(Exception("AUTH_REQUIRED"))

        return try {
            val currentUid = currentUser.uid
            val snapshot = db.collection("videos")
                .whereIn("visibility", listOf("PUBLIC", "public"))
                .limit(50)
                .get()
                .await()

            val parsedVideos = snapshot.documents.mapNotNull { parseVideoDocument(it) }
            val filteredVideos = parsedVideos.filter { video ->
                video.videoUrl.isNotBlank() && !video.isDraft
            }.sortedByDescending { it.createdAt }

            val finalVideos = filteredVideos.map { video ->
                val isLiked = try {
                    db.collection("videos").document(video.videoId)
                        .collection("likes").document(currentUid).get().await().exists()
                } catch (e: Exception) { false }
                video.copy(isLiked = isLiked)
            }

            Result.success(finalVideos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(videoId: String, text: String, parentCommentId: String? = null): Result<CommentResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val userDoc = db.collection("users").document(currentUid).get().await()
            val commentId = UUID.randomUUID().toString()
            val comment = CommentResponse(
                commentId = commentId,
                videoId = videoId,
                uid = currentUid,
                username = userDoc.getString("username") ?: "usuario",
                displayName = userDoc.getString("displayName") ?: "Usuario",
                photoUrl = userDoc.getString("photoUrl") ?: "",
                text = text,
                createdAt = System.currentTimeMillis()
            )
            db.collection("videos").document(videoId).collection("comments").document(commentId).set(comment).await()
            db.collection("videos").document(videoId).update("commentsCount", FieldValue.increment(1)).await()
            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(videoId: String): Result<List<CommentResponse>> {
        return try {
            val currentUid = auth.currentUser?.uid ?: ""
            val snapshot = db.collection("videos").document(videoId).collection("comments").orderBy("createdAt", Query.Direction.DESCENDING).get().await()
            val comments = snapshot.documents.mapNotNull { doc ->
                val comment = doc.toObject(CommentResponse::class.java) ?: return@mapNotNull null
                val isLiked = if (currentUid.isNotEmpty()) {
                    db.collection("comment_likes").document("${comment.commentId}_$currentUid").get().await().exists()
                } else false
                comment.copy(isLiked = isLiked)
            }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(videoId: String, commentId: String): Result<Unit> {
        return try {
            db.collection("videos").document(videoId).collection("comments").document(commentId).delete().await()
            db.collection("videos").document(videoId).update("commentsCount", FieldValue.increment(-1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLikeVideo(videoId: String, currentIsLiked: Boolean): Result<LikeResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val videoRef = db.collection("videos").document(videoId)
            val likeRef = videoRef.collection("likes").document(currentUid)
            var newIsLiked = !currentIsLiked
            var newLikesCount = 0
            db.runTransaction { transaction ->
                val snap = transaction.get(videoRef)
                var count = snap.getLong("likesCount")?.toInt() ?: 0
                if (currentIsLiked) {
                    transaction.delete(likeRef)
                    count = maxOf(0, count - 1)
                } else {
                    transaction.set(likeRef, mapOf("createdAt" to System.currentTimeMillis()))
                    count += 1
                }
                transaction.update(videoRef, "likesCount", count)
                newLikesCount = count
                null
            }.await()
            Result.success(LikeResponse(newIsLiked, newLikesCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeComment(videoId: String, commentId: String): Result<LikeResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val commentRef = db.collection("videos").document(videoId).collection("comments").document(commentId)
            val likeRef = db.collection("comment_likes").document("${commentId}_$currentUid")
            var newLikesCount = 0
            db.runTransaction { transaction ->
                val commentSnap = transaction.get(commentRef)
                if (!commentSnap.exists()) return@runTransaction null
                val likeSnap = transaction.get(likeRef)
                var count = commentSnap.getLong("likesCount")?.toInt() ?: 0
                if (!likeSnap.exists()) {
                    transaction.set(likeRef, mapOf("uid" to currentUid, "commentId" to commentId, "createdAt" to System.currentTimeMillis()))
                    count += 1
                    transaction.update(commentRef, "likesCount", count)
                }
                newLikesCount = count
                null
            }.await()
            Result.success(LikeResponse(true, newLikesCount))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun unlikeComment(videoId: String, commentId: String): Result<LikeResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val commentRef = db.collection("videos").document(videoId).collection("comments").document(commentId)
            val likeRef = db.collection("comment_likes").document("${commentId}_$currentUid")
            var newLikesCount = 0
            db.runTransaction { transaction ->
                val commentSnap = transaction.get(commentRef)
                if (!commentSnap.exists()) return@runTransaction null
                val likeSnap = transaction.get(likeRef)
                var count = commentSnap.getLong("likesCount")?.toInt() ?: 0
                if (likeSnap.exists()) {
                    transaction.delete(likeRef)
                    count = maxOf(0, count - 1)
                    transaction.update(commentRef, "likesCount", count)
                }
                newLikesCount = count
                null
            }.await()
            Result.success(LikeResponse(false, newLikesCount))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun recordVideoView(videoId: String): Result<Int> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val viewKey = "${videoId}_$currentUid"
            val videoRef = db.collection("videos").document(videoId)
            var newViewsCount = 0
            db.runTransaction { transaction ->
                val videoSnap = transaction.get(videoRef)
                if (!videoSnap.exists()) return@runTransaction null
                val viewSnap = transaction.get(db.collection("video_views").document(viewKey))
                var views = videoSnap.getLong("views")?.toInt() ?: 0
                if (!viewSnap.exists()) {
                    transaction.set(db.collection("video_views").document(viewKey), mapOf("videoId" to videoId, "uid" to currentUid))
                    views += 1
                    transaction.update(videoRef, "views", views)
                }
                newViewsCount = views
                null
            }.await()
            Result.success(newViewsCount)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun sharePost(videoId: String, type: String, targetUserIds: List<String>? = null): Result<Unit> {
        return try {
            db.collection("videos").document(videoId).update("sharesCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun getGeminiApiKey(): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) { "" }
    }

    private suspend fun analyzeReportWithGemini(reason: String, description: String?): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getGeminiApiKey()
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
                    return@withContext Pair(false, "Revisión automática no disponible. Una persona revisará tu denuncia pronto.")
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val prompt = "Actúa como un moderador de IA para una red social llamada Momentos. Analiza este reporte de usuario:\nMotivo: $reason\nDetalles: ${description ?: "Ninguno"}\n\nDetermina si es una infracción grave (violencia, acoso, pornografía). Responde únicamente con: 'APROBADA' o 'RECHAZADA' seguido de un motivo muy breve."

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext Pair(false, "Error de análisis. (HTTP ${response.code})")
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                        val isApproved = text.trim().startsWith("APROBADA", ignoreCase = true) || text.contains("APROBADA", ignoreCase = true)
                        return@withContext Pair(isApproved, text)
                    }
                    Pair(false, "No se recibió respuesta válida de la IA.")
                }
            } catch (e: Exception) { Pair(false, "Error de red: ${e.message}") }
        }
    }

    suspend fun reportPost(videoId: String, reason: String, description: String? = null): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val reportId = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()
            
            val reportData = hashMapOf(
                "reportId" to reportId,
                "videoId" to videoId,
                "reporterUid" to currentUid,
                "reason" to reason,
                "description" to (description ?: ""),
                "status" to "reviewing_report",
                "systemResult" to "",
                "systemAnalysis" to "El sistema está revisando tu denuncia. Espere por favor.",
                "createdAt" to createdAt,
                "updatedAt" to createdAt
            )
            db.collection("reports").document(reportId).set(reportData).await()

            // BACKGROUND AUTOMATION
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    delay(5000)
                    db.collection("reports").document(reportId).update("status", "reviewing_publication", "updatedAt", System.currentTimeMillis()).await()
                    
                    delay(5000)
                    val (accepted, analysis) = analyzeReportWithGemini(reason, description)
                    
                    if (accepted) {
                        db.collection("videos").document(videoId).delete().await()
                    }

                    db.collection("reports").document(reportId).update(
                        "status", "resolved",
                        "systemResult", if (accepted) "accepted" else "rejected",
                        "systemAnalysis", analysis,
                        "updatedAt", System.currentTimeMillis(),
                        "resolvedAt", System.currentTimeMillis()
                    ).await()
                } catch (e: Exception) {
                    android.util.Log.e("PostRepository", "Automated report flow failed: ${e.message}")
                    db.collection("reports").document(reportId).update(
                        "status", "resolved",
                        "systemResult", "pending",
                        "systemAnalysis", "Error en el sistema de IA. Un moderador revisará el caso.",
                        "updatedAt", System.currentTimeMillis()
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getReports(): Result<List<ReportModel>> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val snap = db.collection("reports").whereEqualTo("reporterUid", uid).get().await()
            Result.success(snap.documents.mapNotNull { it.toObject(ReportModel::class.java) }.sortedByDescending { it.createdAt })
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun publishVideo(videoId: String, localUriString: String, localThumbnailUriString: String?, description: String, hashtags: List<String>, username: String, displayName: String, visibility: String = "PUBLIC"): Result<VideoModel> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not auth"))
            val video = VideoModel(videoId = videoId, uid = uid, username = username, displayName = displayName, createdAt = System.currentTimeMillis(), status = "UPLOADING", visibility = visibility, description = description, hashtags = hashtags)
            db.collection("videos").document(videoId).set(video).await()
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val vRef = storage.reference.child("videos/$uid/$videoId.mp4")
                    vRef.putFile(Uri.parse(localUriString)).await()
                    val vUrl = vRef.downloadUrl.await().toString()
                    var tUrl = ""
                    if (localThumbnailUriString != null) {
                        val tRef = storage.reference.child("thumbnails/$uid/$videoId.jpg")
                        tRef.putFile(Uri.parse(localThumbnailUriString)).await()
                        tUrl = tRef.downloadUrl.await().toString()
                    }
                    db.collection("videos").document(videoId).update("videoUrl", vUrl, "thumbnailUrl", tUrl, "status", "READY").await()
                } catch (e: Exception) { db.collection("videos").document(videoId).update("status", "FAILED").await() }
            }
            Result.success(video)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun publishStory(mediaFile: java.io.File?, thumbnailFile: java.io.File?, visibility: String, mediaType: String, soundName: String): Result<StoryResponse> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not auth"))
            val id = UUID.randomUUID().toString()
            val mRef = storage.reference.child("stories/$uid/$id.${if(mediaType=="video") "mp4" else "jpg"}")
            mRef.putFile(Uri.fromFile(mediaFile!!)).await()
            val mUrl = mRef.downloadUrl.await().toString()
            var tUrl = mUrl
            if (mediaType == "video" && thumbnailFile != null) {
                val tRef = storage.reference.child("stories_thumbs/$uid/$id.jpg")
                tRef.putFile(Uri.fromFile(thumbnailFile)).await()
                tUrl = tRef.downloadUrl.await().toString()
            }
            val story = StoryModel(storyId = id, ownerId = uid, mediaUrl = mUrl, thumbnailUrl = tUrl, mediaType = mediaType, createdAt = System.currentTimeMillis(), expiresAt = System.currentTimeMillis() + 86400000, visibility = visibility, soundName = soundName)
            db.collection("stories").document(id).set(story).await()
            Result.success(StoryResponse(storyId = id, uid = uid, mediaUrl = mUrl, thumbnailUrl = tUrl, mediaType = mediaType, createdAt = story.createdAt, expiresAt = story.expiresAt))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getStories(): Result<List<StoryResponse>> {
        return try {
            val uid = auth.currentUser?.uid ?: ""
            val snap = db.collection("stories").whereGreaterThan("expiresAt", System.currentTimeMillis()).get().await()
            val viewedIds = if (uid.isNotEmpty()) db.collection("story_views").whereEqualTo("uid", uid).get().await().documents.mapNotNull { it.getString("storyId") }.toSet() else emptySet()
            val stories = snap.documents.mapNotNull { doc ->
                val m = doc.toObject(StoryModel::class.java) ?: return@mapNotNull null
                StoryResponse(storyId = m.storyId, uid = m.ownerId, username = m.username, mediaUrl = m.mediaUrl, thumbnailUrl = m.thumbnailUrl, mediaType = m.mediaType, createdAt = m.createdAt, expiresAt = m.expiresAt, isViewed = viewedIds.contains(m.storyId))
            }
            Result.success(stories)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteStory(storyId: String): Result<Unit> {
        return try { db.collection("stories").document(storyId).delete().await(); Result.success(Unit) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun markStoryAsViewed(storyId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not auth"))
            db.collection("story_views").document("${storyId}_$uid").set(mapOf("storyId" to storyId, "uid" to uid, "viewedAt" to System.currentTimeMillis())).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteVideo(videoId: String): Result<Unit> {
        return try {
            db.collection("videos").document(videoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
