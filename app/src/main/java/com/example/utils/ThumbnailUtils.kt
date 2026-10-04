package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ThumbnailUtils {
    fun generateVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            // Get frame at 1 second mark, or first available
            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
        } catch (e: Exception) {
            android.util.Log.e("ThumbnailUtils", "Error generating thumbnail: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): File? {
        val file = File(context.cacheDir, "$fileName.jpg")
        return try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.close()
            file
        } catch (e: Exception) {
            android.util.Log.e("ThumbnailUtils", "Error saving bitmap: ${e.message}")
            null
        }
    }
}
