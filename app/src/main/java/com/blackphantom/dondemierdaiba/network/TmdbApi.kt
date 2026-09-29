package com.blackphantom.dondemierdaiba.network

import com.blackphantom.dondemierdaiba.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Cliente mínimo para la API de TMDb (https://www.themoviedb.org/documentation/api),
 * usada acá solo para el endpoint "watch/providers": dónde se puede ver
 * legalmente una serie o película. Es información objetiva (qué plataformas
 * la tienen disponible), y TMDb la sirve de forma gratuita y documentada
 * (por atrás son los mismos datos que agrega JustWatch, pero vía una API
 * pensada para desarrolladores, con atribución requerida).
 *
 * Requiere una API key GRATUITA propia (te la generás en
 * themoviedb.org -> Configuración -> API), que se lee desde
 * local.properties como TMDB_API_KEY=tu_clave. Sin clave configurada, esta
 * función simplemente no encuentra nada — no rompe el resto de la app.
 */
object TmdbApi {

    private const val BASE_URL = "https://api.themoviedb.org/3"
    private const val IMAGE_BASE = "https://image.tmdb.org/t/p/w92"
    private const val REGION = "AR"
    private const val TIMEOUT_MS = 8000

    data class Provider(val name: String, val logoUrl: String, val type: String)

    fun hasKey(): Boolean = BuildConfig.TMDB_API_KEY.isNotBlank()

    /** [isMovie] = true busca en /movie, false busca en /tv (series). */
    suspend fun findWatchProviders(query: String, isMovie: Boolean): List<Provider>? =
        withContext(Dispatchers.IO) {
            if (!hasKey() || query.isBlank()) return@withContext null
            val mediaType = if (isMovie) "movie" else "tv"
            val id = searchId(query, mediaType) ?: return@withContext null
            fetchProviders(id, mediaType)
        }

    private fun searchId(query: String, mediaType: String): Int? {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$BASE_URL/search/$mediaType?api_key=${BuildConfig.TMDB_API_KEY}" +
            "&query=$encoded&language=es-AR"
        val body = get(url) ?: return null
        return try {
            val results = JSONObject(body).optJSONArray("results") ?: return null
            if (results.length() == 0) null else results.getJSONObject(0).optInt("id")
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchProviders(id: Int, mediaType: String): List<Provider>? {
        val url = "$BASE_URL/$mediaType/$id/watch/providers?api_key=${BuildConfig.TMDB_API_KEY}"
        val body = get(url) ?: return null
        return try {
            val regionObj = JSONObject(body).optJSONObject("results")?.optJSONObject(REGION)
                ?: return emptyList()
            val list = mutableListOf<Provider>()
            addProviders(regionObj, "flatrate", "Suscripción", list)
            addProviders(regionObj, "rent", "Alquiler", list)
            addProviders(regionObj, "buy", "Compra", list)
            list.distinctBy { it.name }
        } catch (e: Exception) {
            null
        }
    }

    private fun addProviders(regionObj: JSONObject, key: String, typeLabel: String, out: MutableList<Provider>) {
        val arr = regionObj.optJSONArray(key) ?: return
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val logo = p.optString("logo_path", "")
            if (logo.isBlank()) continue
            out.add(
                Provider(
                    name = p.optString("provider_name", ""),
                    logoUrl = "$IMAGE_BASE$logo",
                    type = typeLabel
                )
            )
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
