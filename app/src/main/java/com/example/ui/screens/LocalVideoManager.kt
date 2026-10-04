package com.example.ui.screens

import android.content.Context
import android.net.Uri
import com.example.data.VideoModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object LocalVideoManager {
    private const val PREFS_NAME = "local_videos_prefs"
    private const val KEY_VIDEOS = "videos_list"

    fun copyVideoToLocal(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val dir = File(context.filesDir, "local_videos")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val fileName = "video_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp4"
            val destFile = File(dir, fileName)
            val outputStream = FileOutputStream(destFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveVideoLocally(context: Context, video: VideoModel) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val videos = getLocalVideos(context).toMutableList()
        videos.add(0, video)
        
        try {
            val array = JSONArray()
            for (v in videos) {
                val obj = JSONObject()
                obj.put("videoId", v.videoId)
                obj.put("uid", v.uid)
                obj.put("username", v.username)
                obj.put("displayName", v.displayName)
                obj.put("videoUrl", v.videoUrl)
                obj.put("thumbnailUrl", v.thumbnailUrl)
                obj.put("description", v.description)
                
                val hashArray = JSONArray()
                for (tag in v.hashtags) {
                    hashArray.put(tag)
                }
                obj.put("hashtags", hashArray)
                obj.put("createdAt", v.createdAt)
                obj.put("views", v.views)
                obj.put("likesCount", v.likesCount)
                obj.put("commentsCount", v.commentsCount)
                obj.put("sharesCount", v.sharesCount)
                
                array.put(obj)
            }
            sharedPrefs.edit().putString(KEY_VIDEOS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLocalVideos(context: Context): List<VideoModel> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(KEY_VIDEOS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<VideoModel>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val hashtagsList = mutableListOf<String>()
                val hashArray = obj.optJSONArray("hashtags")
                if (hashArray != null) {
                    for (j in 0 until hashArray.length()) {
                        hashtagsList.add(hashArray.getString(j))
                    }
                }
                list.add(
                    VideoModel(
                        videoId = obj.optString("videoId"),
                        uid = obj.optString("uid"),
                        username = obj.optString("username"),
                        displayName = obj.optString("displayName"),
                        videoUrl = obj.optString("videoUrl"),
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        description = obj.optString("description"),
                        hashtags = hashtagsList,
                        createdAt = obj.optLong("createdAt"),
                        views = obj.optInt("views"),
                        likesCount = obj.optInt("likesCount"),
                        commentsCount = obj.optInt("commentsCount"),
                        sharesCount = obj.optInt("sharesCount")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
