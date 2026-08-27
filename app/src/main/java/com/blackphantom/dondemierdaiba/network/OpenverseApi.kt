package com.blackphantom.dondemierdaiba.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo para la API pública y gratuita de Openverse
 * (https://api.openverse.org), el buscador de imágenes de Creative Commons /
 * dominio público. No requiere API key. Es la forma de darle al usuario un
 * "buscador tipo WhatsApp" para elegir una foto sin tener que pegar un link
 * a mano, sin usar contenido con copyright.
 */
object OpenverseApi {

    private const val BASE_URL = "https://api.openverse.org/v1/images/"
    private const val TIMEOUT_MS = 8000

    data class ImageResult(
        val id: String,
        val title: String,
        val thumbnailUrl: String,
        val fullUrl: String
    )

    suspend fun search(query: String): List<ImageResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$BASE_URL?q=$encoded&page_size=24&mature=false"
        val body = get(url) ?: return@withContext emptyList()

        try {
            val results = JSONObject(body).optJSONArray("results") ?: JSONArray()
            val list = mutableListOf<ImageResult>()
            for (i in 0 until results.length()) {
                val obj = results.getJSONObject(i)
                val fullUrl = obj.optString("url", "")
                if (fullUrl.isBlank()) continue
                val thumb = obj.optString("thumbnail", "").ifBlank { fullUrl }
                list.add(
                    ImageResult(
                        id = obj.optString("id", i.toString()),
                        title = obj.optString("title", ""),
                        thumbnailUrl = thumb,
                        fullUrl = fullUrl
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun get(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "DondeMierdaIbaApp/1.0")
            }
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
