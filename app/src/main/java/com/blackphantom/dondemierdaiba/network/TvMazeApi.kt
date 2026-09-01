package com.blackphantom.dondemierdaiba.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo para la API pública y gratuita de TVmaze (https://www.tvmaze.com/api).
 * No requiere API key ni autenticación, y sus datos (título, temporada, capítulo,
 * fecha de emisión) son información objetiva, no contenido con copyright — por eso
 * es una forma legal de avisar sobre nuevos capítulos/temporadas.
 *
 * Se usa HttpURLConnection + org.json (ambos incluidos en el SDK de Android) para
 * no sumar dependencias pesadas como Retrofit/OkHttp/Gson.
 */
object TvMazeApi {

    private const val BASE_URL = "https://api.tvmaze.com"
    private const val TIMEOUT_MS = 8000

    data class ShowMatch(val id: Int, val name: String, val imageUrl: String?)
    data class LatestEpisode(val season: Int, val number: Int, val name: String)

    /** Busca una serie por nombre y devuelve el resultado más relevante de TVmaze. */
    suspend fun searchShow(query: String): ShowMatch? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val body = get("$BASE_URL/search/shows?q=$encoded") ?: return@withContext null
        try {
            val results = JSONArray(body)
            if (results.length() == 0) return@withContext null
            val show = results.getJSONObject(0).getJSONObject("show")
            val image = show.optJSONObject("image")
            // "original" es más grande/nítida que "medium"; si no viene ninguna
            // (pasa con series muy poco conocidas), queda null y no se usa.
            val imageUrl = image?.optString("original")?.takeIf { it.isNotBlank() }
                ?: image?.optString("medium")?.takeIf { it.isNotBlank() }
            ShowMatch(id = show.getInt("id"), name = show.getString("name"), imageUrl = imageUrl)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Devuelve el último capítulo YA EMITIDO de una serie (no el próximo a estrenar).
     * Esto es lo que nos interesa para avisar "salió temporada/capítulo nuevo".
     */
    suspend fun latestAiredEpisode(showId: Int): LatestEpisode? = withContext(Dispatchers.IO) {
        val body = get("$BASE_URL/shows/$showId?embed=previousepisode") ?: return@withContext null
        try {
            val obj = JSONObject(body)
            val embedded = obj.optJSONObject("_embedded") ?: return@withContext null
            val ep = embedded.optJSONObject("previousepisode") ?: return@withContext null
            LatestEpisode(
                season = ep.optInt("season", 0),
                number = ep.optInt("number", 0),
                name = ep.optString("name", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Devuelve, para cada temporada, cuántos capítulos tiene según TVmaze
     * (mapa temporada -> cantidad). Se usa para no dejar poner un capítulo
     * que esa temporada todavía no tiene. Ojo: en una temporada que está
     * emitiéndose, esto es "lo que salió hasta ahora", no el total final,
     * porque TVmaze tampoco lo sabe todavía.
     */
    suspend fun episodesBySeason(showId: Int): Map<Int, Int>? = withContext(Dispatchers.IO) {
        val body = get("$BASE_URL/shows/$showId/episodes") ?: return@withContext null
        try {
            val episodes = JSONArray(body)
            val counts = mutableMapOf<Int, Int>()
            for (i in 0 until episodes.length()) {
                val ep = episodes.getJSONObject(i)
                val season = ep.optInt("season", 0)
                val number = ep.optInt("number", 0)
                if (season <= 0 || number <= 0) continue // saltea especiales sin numerar
                val current = counts[season] ?: 0
                if (number > current) counts[season] = number
            }
            if (counts.isEmpty()) null else counts
        } catch (e: Exception) {
            null
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
