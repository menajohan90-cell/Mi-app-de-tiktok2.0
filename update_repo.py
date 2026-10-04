import re

with open("app/src/main/java/com/example/data/PostRepository.kt", "r") as f:
    content = f.read()

# Add imports
imports = """import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay"""
content = re.sub(r'import com.google.firebase.auth.FirebaseAuth\nimport com.google.firebase.firestore.FieldValue\nimport com.google.firebase.firestore.FirebaseFirestore', imports, content)

# Add storage instance
content = content.replace('private val auth = FirebaseAuth.getInstance()', 'private val auth = FirebaseAuth.getInstance()\n    private val storage = FirebaseStorage.getInstance()')

# Replace publishVideo
old_publish = """    suspend fun publishVideo(
        videoId: String,
        videoUrl: String,
        description: String,
        hashtags: List<String>,
        username: String,
        displayName: String
    ): Result<VideoModel> {
        return try {
            val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
            val video = VideoModel(
                videoId = videoId,
                uid = currentUid,
                username = username,
                displayName = displayName,
                videoUrl = videoUrl,
                description = description,
                hashtags = hashtags,
                createdAt = System.currentTimeMillis(),
                views = 0,
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                isLiked = false
            )
            db.collection("videos").document(videoId).set(video).await()
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }"""

new_publish = """    suspend fun publishVideo(
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
            val video = VideoModel(
                videoId = videoId,
                uid = currentUid,
                username = username,
                displayName = displayName,
                videoUrl = localUriString,
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
                        "status", "SECURITY_REVIEW"
                    ).await()
                    
                    delay(3000)
                    
                    db.collection("videos").document(videoId).update(
                        "status", "READY"
                    ).await()
                } catch (e: Exception) {
                    db.collection("videos").document(videoId).update(
                        "status", "FAILED"
                    ).await()
                }
            }
            
            Result.success(video)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }"""

content = content.replace(old_publish, new_publish)

with open("app/src/main/java/com/example/data/PostRepository.kt", "w") as f:
    f.write(content)
