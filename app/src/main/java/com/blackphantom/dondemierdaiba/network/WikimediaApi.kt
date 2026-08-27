package com.blackphantom.dondemierdaiba.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo para la API pública de Wikimedia Commons
 * (https://commons.wikimedia.org/w/api.php), el mismo banco de imágenes
 * libres (dominio público / Creative Commons) que usa Wikipedia. No requiere
 * API key y su límite de uso anónimo es mucho más generoso que el de otras
 * APIs "sin registro" (como Openverse, que solo deja 5 búsquedas por hora).
 *
 * Es la base del buscador de fotos "tipo WhatsApp": el usuario escribe,
 * aparece una grilla de miniaturas, toca una y queda elegida.
 */
object WikimediaApi {

    private const val BASE_URL = "https://commons.wikimedia.org/w/api.php"
    private const val TIMEOUT_MS = 8000
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")

    data class ImageResult(
        val id: String,
        val title: String,
        val thumbnailUrl: String,
        val fullUrl: String
    )

    suspend fun search(query: String): List<ImageResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$BASE_URL?action=query&format=json&generator=search" +
            "&gsrnamespace=6&gsrlimit=30&gsrsearch=$encoded" +
            "&prop=imageinfo&iiprop=url&iiurlwidth=400"
        val body = get(url) ?: return@withContext emptyList()

        try {
            val pages = JSONObject(body)
                .optJSONObject("query")
                ?.optJSONObject("pages") ?: return@withContext emptyList()

            val list = mutableListOf<ImageResult>()
            val keys = pages.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val page = pages.getJSONObject(key)
                val infoArray = page.optJSONArray("imageinfo")
                if (infoArray == null || infoArray.length() == 0) continue
                val info = infoArray.getJSONObject(0)

                val fullUrl = info.optString("url", "")
                if (fullUrl.isBlank()) continue

                val extension = fullUrl.substringAfterLast('.', "").lowercase()
                if (extension !in IMAGE_EXTENSIONS) continue // saltea .svg, .ogg, .pdf, etc.

                val thumbUrl = info.optString("thumburl", fullUrl)
                val title = page.optString("title", "").removePrefix("File:")

                list.add(ImageResult(id = key, title = title, thumbnailUrl = thumbUrl, fullUrl = fullUrl))
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
                // Wikimedia pide un User-Agent descriptivo como buena práctica.
                setRequestProperty("User-Agent", "DondeMierdaIbaApp/1.0 (uso personal, Android)")
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
