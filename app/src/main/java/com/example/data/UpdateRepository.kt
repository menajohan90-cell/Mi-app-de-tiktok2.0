package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class UpdateRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun formatDirectDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""

        // Convert Google Drive view URL to direct download URL
        if (trimmed.contains("drive.google.com")) {
            val fileIdRegex = Regex("/file/d/([a-zA-Z0-9_-]+)")
            val match = fileIdRegex.find(trimmed)
            if (match != null) {
                val fileId = match.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }
            val idParamRegex = Regex("[?&]id=([a-zA-Z0-9_-]+)")
            val idMatch = idParamRegex.find(trimmed)
            if (idMatch != null) {
                val fileId = idMatch.groupValues[1]
                return "https://drive.google.com/uc?export=download&id=$fileId"
            }
        }
        // Convert Dropbox preview link to direct link
        if (trimmed.contains("dropbox.com") && !trimmed.contains("dl.dropboxusercontent.com")) {
            return trimmed.replace("www.dropbox.com", "dl.dropboxusercontent.com")
                .replace("dropbox.com", "dl.dropboxusercontent.com")
                .replace("?dl=0", "")
                .replace("&dl=0", "")
        }
        // Convert GitHub release/blob to raw download
        if (trimmed.contains("github.com") && !trimmed.contains("objects/githubusercontent.com")) {
            if (trimmed.contains("/releases/download/")) {
                return trimmed // Ya es descarga directa
            }
            if (trimmed.contains("/blob/")) {
                return trimmed.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
            }
        }
        return trimmed
    }

    suspend fun setApkUrl(url: String): Result<Unit> {
        return try {
            val directUrl = formatDirectDownloadUrl(url)
            val data = mapOf(
                "apkUrl" to directUrl,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("app_updates").document("latest").set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestUpdate(): Result<AppUpdateModel?> {
        return try {
            val doc = try {
                db.collection("app_updates").document("latest").get(Source.SERVER).await()
            } catch (e: Exception) {
                db.collection("app_updates").document("latest").get(Source.DEFAULT).await()
            }

            if (doc.exists()) {
                val update = doc.toObject(AppUpdateModel::class.java)
                Result.success(update)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGitHubLatestRelease(repoPath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // repoPath ej: "usuario/mi-repositorio"
                val url = "https://api.github.com/repos/$repoPath/releases/latest"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    // Búsqueda simple del primer browser_download_url que termine en .apk
                    val regex = Regex("\"browser_download_url\":\\s*\"(.*?\\.apk)\"")
                    val match = regex.find(body)
                    val apkUrl = match?.groupValues?.get(1)
                    if (apkUrl != null) {
                        Result.success(apkUrl)
                    } else {
                        Result.failure(Exception("No se encontró archivo APK en el Release de GitHub"))
                    }
                } else {
                    Result.failure(Exception("Error al consultar GitHub: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun publishUpdate(
        versionName: String,
        versionCode: Int,
        releaseNotes: String,
        releaseDate: String,
        mandatory: Boolean,
        minimumVersion: Int,
        fileSize: String,
        updateType: String,
        securityPatch: Boolean,
        apkUri: Uri?,
        directApkUrl: String? = null,
        context: Context
    ): Result<String> {
        return try {
            var apkUrl = ""
            if (!directApkUrl.isNullOrBlank()) {
                apkUrl = formatDirectDownloadUrl(directApkUrl)
            } else if (apkUri != null) {
                val storageRef = storage.reference.child("app_updates/app_v${versionName}_code${versionCode}.apk")
                storageRef.putFile(apkUri).await()
                apkUrl = storageRef.downloadUrl.await().toString()
            }

            val updateData = mapOf(
                "versionName" to versionName,
                "versionCode" to versionCode,
                "apkUrl" to apkUrl,
                "releaseNotes" to releaseNotes,
                "releaseDate" to releaseDate,
                "mandatory" to mandatory,
                "minimumVersion" to minimumVersion,
                "fileSize" to fileSize,
                "updateType" to updateType,
                "securityPatch" to securityPatch
            )

            db.collection("app_updates").document("latest").set(updateData).await()
            Result.success(apkUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float, Long, Long) -> Unit
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val directUrl = formatDirectDownloadUrl(apkUrl)
                if (directUrl.isBlank()) {
                    return@withContext Result.failure(Exception("URL del APK inválida o vacía"))
                }

                val client = OkHttpClient.Builder().build()
                val request = Request.Builder()
                    .url(directUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Error al descargar APK: ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Cuerpo de respuesta vacío"))
                val contentLength = body.contentLength()

                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val apkFile = File(downloadsDir, "update_latest.apk")
                if (apkFile.exists()) apkFile.delete()

                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(8192)
                var bytesDownloaded: Long = 0
                var read: Int

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesDownloaded += read
                            val progress = if (contentLength > 0) bytesDownloaded.toFloat() / contentLength.toFloat() else 0f
                            withContext(Dispatchers.Main) {
                                onProgress(progress, bytesDownloaded, contentLength)
                            }
                        }
                        output.flush()
                    }
                }

                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun installApk(context: Context, file: File) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Habilita 'Instalar aplicaciones desconocidas' para continuar la actualización", Toast.LENGTH_LONG).show()
                    return
                }
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("UpdateRepository", "Error installing APK: ${e.message}", e)
            Toast.makeText(context, "No se pudo abrir el instalador: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val directUrl = formatDirectDownloadUrl(url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error abriendo navegador: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDownloadLink(context: Context, url: String, versionName: String) {
        try {
            val directUrl = formatDirectDownloadUrl(url)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Descarga la actualización v$versionName de Momentos")
                putExtra(Intent.EXTRA_TEXT, "Actualiza Momentos a la versión v$versionName directamente desde tu teléfono:\n$directUrl")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir enlace de actualización").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Error compartiendo enlace: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getInstalledVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getInstalledVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
