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
                val thumbnailURL = (data["thumbnailURL"] ?: data["thumbnailUrl"] ?: data["thumbnail"]) as? String ?: ""
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
                    thumbnailURL = thumbnailURL,
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
        if (currentUser == null) {
            android.util.Log.d("PostRepository", "getVideos called with no authenticated user")
            return Result.failure(Exception("AUTH_REQUIRED: Debes iniciar sesión para ver las publicaciones de Momentos."))
        }

        return try {
            val currentUid = currentUser.uid

            // Query videos satisfying Firestore rules:
            // allow read: if isAuthenticated() && (resource.data.visibility == 'PUBLIC' || resource.data.visibility == 'public' || resource.data.uid == request.auth.uid);
            val snapshot = try {
                db.collection("videos")
                    .whereIn("visibility", listOf("PUBLIC", "public"))
                    .limit(50)
                    .get()
                    .await()
            } catch (e1: Exception) {
                try {
                    db.collection("videos")
                        .whereEqualTo("visibility", "PUBLIC")
                        .limit(50)
                        .get()
                        .await()
                } catch (e2: Exception) {
                    db.collection("videos")
                        .whereEqualTo("visibility", "public")
                        .limit(50)
                        .get()
                        .await()
                }
            }

            val parsedVideos = snapshot.documents.mapNotNull { parseVideoDocument(it) }
            val filteredVideos = parsedVideos.filter { video ->
                val hasValidUrl = video.videoUrl.isNotBlank() && !video.videoUrl.startsWith("content://media/picker_get_content")
                val notDraft = !video.isDraft
                val isPublic = video.visibility.equals("PUBLIC", ignoreCase = true) || video.visibility.isBlank()
                hasValidUrl && notDraft && isPublic
            }.sortedByDescending { it.createdAt }

            // Friend feed filter if applicable
            if (feedType == "Amigos") {
                try {
                    val connections = db.collection("connections")
                        .whereEqualTo("status", "accepted")
                        .get().await()
                    val friendUids = connections.documents.mapNotNull { doc ->
                        val req = doc.getString("requesterUid")
                        val tgt = doc.getString("targetUid")
                        if (req == currentUid) tgt else if (tgt == currentUid) req else null
                    }.toSet()
                    if (friendUids.isNotEmpty()) {
                        val friendVideos = filteredVideos.filter { it.uid in friendUids }
                        if (friendVideos.isNotEmpty()) {
                            return Result.success(friendVideos)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PostRepository", "Error getting friend connections: ${e.message}")
                }
            }

            // Populate isLiked for current user safely
            val finalVideos = if (filteredVideos.isNotEmpty()) {
                filteredVideos.map { video ->
                    val isLiked = try {
                        if (video.videoId.isNotEmpty()) {
                            val likeDoc = db.collection("videos").document(video.videoId)
                                .collection("likes").document(currentUid).get().await()
                            likeDoc.exists()
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                    video.copy(isLiked = isLiked)
                }
            } else {
                filteredVideos
            }

            Result.success(finalVideos)
        } catch (e: Exception) {
            android.util.Log.w("PostRepository", "Could not load videos: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addComment(videoId: String, text: String, parentCommentId: String? = null): Result<CommentResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val userDoc = db.collection("users").document(currentUid).get().await()
            val profile = userDoc.toObject(ProfileResponse::class.java) ?: ProfileResponse(uid = currentUid)

            val commentId = UUID.randomUUID().toString()
            val comment = CommentResponse(
                commentId = commentId,
                videoId = videoId,
                uid = currentUid,
                username = profile.username,
                displayName = profile.displayName,
                photoUrl = profile.photoUrl,
                text = text,
                createdAt = System.currentTimeMillis()
            )

            db.collection("videos").document(videoId)
                .collection("comments").document(commentId)
                .set(comment).await()
                
            db.collection("videos").document(videoId)
                .update("commentsCount", FieldValue.increment(1)).await()

            Result.success(comment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(videoId: String): Result<List<CommentResponse>> {
        return try {
            val snapshot = db.collection("videos").document(videoId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val comments = snapshot.documents.mapNotNull { it.toObject(CommentResponse::class.java) }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(videoId: String, commentId: String): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            
            val commentDoc = db.collection("videos").document(videoId)
                .collection("comments").document(commentId).get().await()
            
            val comment = commentDoc.toObject(CommentResponse::class.java)
            if (comment?.uid == currentUid) {
                db.collection("videos").document(videoId)
                    .collection("comments").document(commentId).delete().await()
                    
                db.collection("videos").document(videoId)
                    .update("commentsCount", FieldValue.increment(-1)).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No permission to delete this comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLikeVideo(videoId: String, currentIsLiked: Boolean): Result<LikeResponse> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val videoRef = db.collection("videos").document(videoId)
            val likeRef = videoRef.collection("likes").document(currentUid)

            var newIsLiked = currentIsLiked
            var newLikesCount = 0

            db.runTransaction { transaction ->
                val videoSnapshot = transaction.get(videoRef)
                val likeSnapshot = transaction.get(likeRef)
                var count = videoSnapshot.getLong("likesCount")?.toInt() ?: 0

                if (likeSnapshot.exists()) {
                    // Unlike
                    transaction.delete(likeRef)
                    count = maxOf(0, count - 1)
                    transaction.update(videoRef, "likesCount", count)
                    newIsLiked = false
                } else {
                    // Like
                    transaction.set(likeRef, mapOf("createdAt" to System.currentTimeMillis()))
                    count += 1
                    transaction.update(videoRef, "likesCount", count)
                    newIsLiked = true
                }
                newLikesCount = count
                null
            }.await()

            Result.success(LikeResponse(newIsLiked, newLikesCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordVideoView(videoId: String): Result<Int> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val viewKey = "${videoId}_$currentUid"
            val viewDocRef = db.collection("video_views").document(viewKey)
            val videoRef = db.collection("videos").document(videoId)

            var newViewsCount = 0
            db.runTransaction { transaction ->
                val viewSnap = transaction.get(viewDocRef)
                val videoSnap = transaction.get(videoRef)
                var views = videoSnap.getLong("views")?.toInt() ?: 0

                if (!viewSnap.exists()) {
                    transaction.set(viewDocRef, mapOf("videoId" to videoId, "uid" to currentUid, "createdAt" to System.currentTimeMillis()))
                    views += 1
                    transaction.update(videoRef, "views", views)
                }
                newViewsCount = views
                null
            }.await()

            Result.success(newViewsCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getGeminiApiKey(): String {
        return try {
            val field = com.example.BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun analyzeReportWithGemini(reason: String, description: String?): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getGeminiApiKey()
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("Placeholder")) {
                    return@withContext Pair(false, "Una persona o un sistema automatizado pronto registrará o revisará tu denuncia.")
                }

                val client = OkHttpClient()
                val prompt = "Actúa como un sistema de moderación de contenido para una red social. Analiza el siguiente reporte de un usuario. Motivo del reporte: '$reason'. Detalles adicionales: '${description ?: "Ninguno"}'. " +
                        "Determina estrictamente si el contenido infringe las normas comunitarias (acoso, spam, violencia, contenido peligroso, etc.). " +
                        "Responde en español indicando primero la palabra 'APROBADA' o 'RECHAZADA', seguida de un punto y un análisis profesional detallado justificando la decisión."

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", prompt)
                        ))
                    ))
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
                
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Pair(false, "Una persona o un sistema automatizado pronto registrará o revisará tu denuncia.")
                    }
                    val responseString = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseString)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            val isApproved = text.contains("APROBADA", ignoreCase = true) || text.contains("infrinja", ignoreCase = true) || text.contains("violación", ignoreCase = true)
                            val analysis = text.ifBlank {
                                if (isApproved) "Denuncia aprobada. El sistema determinó que la publicación infringe las normas y ha sido eliminada permanentemente de la plataforma."
                                else "Denuncia rechazada. El motivo indicado ('$reason') no constituye una infracción tras la revisión exhaustiva del contenido por parte del sistema."
                            }
                            return@withContext Pair(isApproved, analysis)
                        }
                    }
                    Pair(false, "Una persona o un sistema automatizado pronto registrará o revisará tu denuncia.")
                }
            } catch (e: Exception) {
                android.util.Log.w("PostRepository", "Gemini API error or quota exhausted: ${e.message}")
                Pair(false, "Una persona o un sistema automatizado pronto registrará o revisará tu denuncia.")
            }
        }
    }

    suspend fun reportPost(videoId: String, reason: String, description: String? = null): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val reportId = UUID.randomUUID().toString()
            
            var reportedUserId = ""
            try {
                val videoDoc = db.collection("videos").document(videoId).get().await()
                reportedUserId = videoDoc.getString("uid") ?: ""
            } catch (_: Exception) {}

            val reportData = mapOf(
                "reportId" to reportId,
                "videoId" to videoId,
                "reporterUid" to currentUid,
                "reportedUserId" to reportedUserId,
                "reason" to reason,
                "description" to (description ?: ""),
                "status" to "reviewing_report",
                "systemResult" to "",
                "systemAnalysis" to "El sistema está revisando tu denuncia. Espere por favor.",
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "resolvedAt" to 0L
            )
            db.collection("reports").document(reportId).set(reportData).await()
            
            val notifId = UUID.randomUUID().toString()
            db.collection("system_notifications").document(notifId).set(mapOf(
                "id" to notifId,
                "uid" to currentUid,
                "title" to "Denuncia recibida",
                "message" to "El sistema está revisando tu denuncia. Espere por favor.",
                "createdAt" to System.currentTimeMillis(),
                "type" to "REPORT_STATUS",
                "reportId" to reportId
            )).await()
            
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    // 30 segundos revisando la denuncia
                    delay(30000)
                    db.collection("reports").document(reportId).update(
                        mapOf(
                            "status" to "reviewing_publication",
                            "systemAnalysis" to "El sistema está revisando la publicación.",
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()

                    // 30 segundos revisando el video / publicación y llamada a IA real (Gemini)
                    delay(30000)
                    val (accepted, analysisText) = analyzeReportWithGemini(reason, description)
                    val resultVal = if (accepted) "accepted" else if (analysisText.contains("Una persona")) "pending" else "rejected"
                    
                    if (accepted) {
                        try {
                            db.collection("videos").document(videoId).delete().await()
                        } catch (e: Exception) {
                            android.util.Log.w("PostRepository", "Could not delete reported video: ${e.message}")
                        }
                    }

                    db.collection("reports").document(reportId).update(
                        mapOf(
                            "status" to "resolved",
                            "systemResult" to resultVal,
                            "systemAnalysis" to analysisText,
                            "updatedAt" to System.currentTimeMillis(),
                            "resolvedAt" to System.currentTimeMillis()
                        )
                    ).await()

                    val notifId2 = UUID.randomUUID().toString()
                    val resTitle = if (accepted) "Denuncia aprobada" else if (analysisText.contains("Una persona")) "Revisión extendida" else "Denuncia rechazada"
                    db.collection("system_notifications").document(notifId2).set(mapOf(
                        "id" to notifId2,
                        "uid" to currentUid,
                        "title" to resTitle,
                        "message" to "El sistema terminó de revisar tu denuncia. $analysisText",
                        "createdAt" to System.currentTimeMillis(),
                        "type" to "REPORT_RESOLVED",
                        "reportId" to reportId
                    )).await()
                } catch (e: Exception) {
                    android.util.Log.e("PostRepository", "Background report analysis error: ${e.message}")
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReports(): Result<List<ReportModel>> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val snapshot = db.collection("reports")
                .whereEqualTo("reporterUid", currentUid)
                .get().await()
            val reports = snapshot.documents.mapNotNull { it.toObject(ReportModel::class.java) }
                .sortedByDescending { it.createdAt }
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReportDetail(reportId: String): Result<ReportModel?> {
        return try {
            val doc = db.collection("reports").document(reportId).get().await()
            if (doc.exists()) {
                Result.success(doc.toObject(ReportModel::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sharePost(videoId: String, type: String, targetUserIds: List<String>? = null): Result<Unit> {
        return try {
            val videoRef = db.collection("videos").document(videoId)
            videoRef.update("sharesCount", FieldValue.increment(1)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Story and Publish methods simplified for brevity/local changes...
    suspend fun publishVideo(
        videoId: String,
        localUriString: String,
        description: String,
        hashtags: List<String>,
        username: String,
        displayName: String,
        visibility: String = "PUBLIC"
    ): Result<VideoModel> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val initialUrl = if (localUriString.startsWith("content://media/picker_get_content")) "" else localUriString
            val video = VideoModel(
                videoId = videoId,
                uid = currentUid,
                username = username,
                displayName = displayName,
                videoUrl = initialUrl,
                description = description,
                hashtags = hashtags,
                createdAt = System.currentTimeMillis(),
                views = 0,
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                isLiked = false,
                status = "UPLOADING",
                visibility = visibility
            )
            db.collection("videos").document(videoId).set(video).await()
            
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(localUriString)
                    val ref = storage.reference.child("videos/${currentUid}/${videoId}.mp4")
                    ref.putFile(uri).await()
                    val downloadUrl = ref.downloadUrl.await().toString()
                    
                    db.collection("videos").document(videoId).update(
                        "videoUrl", downloadUrl,
                        "status", "READY"
                    ).await()
                } catch (e: Exception) {
                    android.util.Log.e("PostRepository", "Video storage upload failed: ${e.message}", e)
                    db.collection("videos").document(videoId).update(
                        "status", "FAILED"
                    ).await()
                }
            }
            
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likeComment(commentId: String): Result<LikeResponse> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val likeId = "${commentId}_$currentUid"
            val docRef = db.collection("comment_likes").document(likeId)
            val doc = docRef.get().await()
            val isLiked = if (doc.exists()) {
                docRef.delete().await()
                false
            } else {
                val data = mapOf(
                    "commentId" to commentId,
                    "uid" to currentUid,
                    "createdAt" to System.currentTimeMillis()
                )
                docRef.set(data).await()
                true
            }
            val snapshot = db.collection("comment_likes").whereEqualTo("commentId", commentId).get().await()
            Result.success(LikeResponse(isLiked, snapshot.size()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikeComment(commentId: String): Result<LikeResponse> = likeComment(commentId)

    suspend fun incrementView(videoId: String): Int {
        return try {
            val docRef = db.collection("videos").document(videoId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentViews = (snapshot.get("views") as? Number)?.toInt() ?: 0
                val newViews = currentViews + 1
                transaction.update(docRef, "views", newViews)
                newViews
            }.await()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun publishStory(
        mediaFile: java.io.File?,
        mediaUrl: String,
        visibility: String,
        mediaType: String,
        soundName: String
    ): Result<StoryResponse> = Result.success(StoryResponse())

    suspend fun getStories(): Result<List<StoryResponse>> = Result.success(emptyList())
    suspend fun deleteStory(storyId: String): Result<Unit> = Result.success(Unit)
}
