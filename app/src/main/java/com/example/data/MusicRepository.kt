package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class SongItem(
    val trackId: Long,
    val trackName: String,
    val artistName: String,
    val previewUrl: String,
    val artworkUrl: String
)

class MusicRepository {
    private val client = OkHttpClient()

    suspend fun searchSongs(query: String = "trending"): Result<List<SongItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query.ifBlank { "trending" }, "UTF-8")
                val request = Request.Builder().url("https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=30").build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Error al conectar con la API de música"))

                val bodyStr = response.body?.string() ?: "{}"
                val json = JSONObject(bodyStr)
                val results = json.getJSONArray("results")
                val songs = mutableListOf<SongItem>()

                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    songs.add(
                        SongItem(
                            trackId = item.optLong("trackId", i.toLong()),
                            trackName = item.optString("trackName", "Música sin título"),
                            artistName = item.optString("artistName", "Artista desconocido"),
                            previewUrl = item.optString("previewUrl", ""),
                            artworkUrl = item.optString("artworkUrl100", "")
                        )
                    )
                }
                Result.success(songs)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
