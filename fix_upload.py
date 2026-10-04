import re

with open("app/src/main/java/com/example/ui/screens/PublishScreen.kt", "r") as f:
    content = f.read()

# Make sure to import StorageMetadata
if "import com.google.firebase.storage.StorageMetadata" not in content:
    content = content.replace("import com.google.firebase.storage.FirebaseStorage", "import com.google.firebase.storage.FirebaseStorage\nimport com.google.firebase.storage.StorageMetadata")

old_upload = """                            val uploadTask = storageRef.putFile(selectedVideoUri!!)
                            
                            uploadTask.addOnProgressListener { snapshot ->
                                val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                                uploadProgress = (progress / 100.0).toFloat()
                            }.continueWithTask { task ->
                                if (!task.isSuccessful) {
                                    task.exception?.let { throw it }
                                }
                                storageRef.downloadUrl
                            }.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val downloadUri = task.result
                                    
                                    val hashtagsList = hashtagsText.split(" ")
                                        .filter { it.isNotBlank() }
                                        .map { if (it.startsWith("#")) it else "#$it" }
                                    
                                    val videoModel = VideoModel(
                                        videoId = videoId,
                                        uid = user.uid,
                                        username = currentUserProfile!!["username"] as? String ?: "user",
                                        displayName = currentUserProfile!!["displayName"] as? String ?: user.displayName ?: "User",
                                        videoUrl = downloadUri.toString(),
                                        thumbnailURL = "", // We can add thumbnails later
                                        description = description,
                                        hashtags = hashtagsList,
                                        createdAt = System.currentTimeMillis(),
                                        views = 0,
                                        likesCount = 0,
                                        commentsCount = 0,
                                        sharesCount = 0
                                    )
                                    
                                    db.collection("videos").document(videoId).set(videoModel)
                                        .addOnSuccessListener {
                                            isUploading = false
                                            Toast.makeText(context, "¡Video publicado con éxito!", Toast.LENGTH_LONG).show()
                                            navController.popBackStack()
                                        }
                                        .addOnFailureListener { e ->
                                            isUploading = false
                                            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    isUploading = false
                                    android.util.Log.e("PublishScreen", "Upload failed", task.exception)
                                    uploadError = "Error: ${task.exception?.message ?: "No se pudo subir. Comprueba tu conexión."}"
                                }
                            }"""

new_upload = """                            val contentResolver = context.contentResolver
                            val mimeType = contentResolver.getType(selectedVideoUri!!) ?: "video/mp4"
                            val metadata = StorageMetadata.Builder()
                                .setContentType(mimeType)
                                .build()

                            val uploadTask = storageRef.putFile(selectedVideoUri!!, metadata)
                            
                            uploadTask.addOnProgressListener { snapshot ->
                                val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                                uploadProgress = (progress / 100.0).toFloat()
                            }.addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val hashtagsList = hashtagsText.split(" ")
                                        .filter { it.isNotBlank() }
                                        .map { if (it.startsWith("#")) it else "#$it" }
                                    
                                    val videoModel = VideoModel(
                                        videoId = videoId,
                                        uid = user.uid,
                                        username = currentUserProfile!!["username"] as? String ?: "user",
                                        displayName = currentUserProfile!!["displayName"] as? String ?: user.displayName ?: "User",
                                        videoUrl = downloadUri.toString(),
                                        thumbnailURL = "", 
                                        description = description,
                                        hashtags = hashtagsList,
                                        createdAt = System.currentTimeMillis(),
                                        views = 0,
                                        likesCount = 0,
                                        commentsCount = 0,
                                        sharesCount = 0
                                    )
                                    
                                    db.collection("videos").document(videoId).set(videoModel)
                                        .addOnSuccessListener {
                                            isUploading = false
                                            Toast.makeText(context, "¡Video publicado con éxito!", Toast.LENGTH_LONG).show()
                                            navController.popBackStack()
                                        }
                                        .addOnFailureListener { e ->
                                            isUploading = false
                                            Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }.addOnFailureListener { e ->
                                    isUploading = false
                                    android.util.Log.e("PublishScreen", "Get URL failed", e)
                                    uploadError = "Error al obtener URL: ${e.message}"
                                }
                            }.addOnFailureListener { e ->
                                isUploading = false
                                android.util.Log.e("PublishScreen", "Upload failed", e)
                                uploadError = "Error de subida: ${e.message ?: "Verifica las Reglas de Storage (Permisos y formato)."}"
                            }"""

content = content.replace(old_upload, new_upload)

with open("app/src/main/java/com/example/ui/screens/PublishScreen.kt", "w") as f:
    f.write(content)
