package com.blackphantom.dondemierdaiba.util

import android.content.Context
import android.net.Uri
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.data.ContentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta todo lo guardado (series, películas y videos) a un único archivo
 * JSON que se puede compartir por WhatsApp, mail, etc., y lo vuelve a leer
 * para importarlo en otro dispositivo (o en el mismo). No requiere ningún
 * inicio de sesión ni servidor: el archivo ES el backup.
 *
 * Nota: al importar, todo se agrega como contenido nuevo (no se pisa ni se
 * intenta "fusionar" con lo que ya había) para no arriesgar perder datos por
 * una mala coincidencia de nombres. Si algo queda duplicado, se borra a mano
 * desde el modal de editar progreso.
 */
object BackupManager {

    private const val FORMAT_VERSION = 1

    data class ImportSummary(val total: Int, val series: Int, val peliculas: Int, val videos: Int)

    suspend fun exportToFile(context: Context): File = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).contentDao()
        val items = dao.getAllOnce()

        val root = JSONObject().apply {
            put("app", "DondeMierdaIba")
            put("formatVersion", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("items", JSONArray().apply {
                items.forEach { put(itemToJson(it)) }
            })
        }

        val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val file = File(backupsDir, "donde_mierda_iba_backup_$stamp.json")
        file.writeText(root.toString(2))
        file
    }

    /** Lee un archivo (elegido a mano o recibido por otra app) y devuelve la lista para importar. */
    suspend fun readItemsFromUri(context: Context, uri: Uri): List<ContentEntity>? = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() } ?: return@withContext null

            val root = JSONObject(text)
            val itemsArray = root.optJSONArray("items") ?: return@withContext null

            val result = mutableListOf<ContentEntity>()
            for (i in 0 until itemsArray.length()) {
                jsonToItem(itemsArray.getJSONObject(i))?.let { result.add(it) }
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    suspend fun importItems(context: Context, items: List<ContentEntity>): ImportSummary = withContext(Dispatchers.IO) {
        val dao = AppDatabase.getInstance(context).contentDao()
        dao.insertAll(items)
        ImportSummary(
            total = items.size,
            series = items.count { it.type == "SERIE" },
            peliculas = items.count { it.type == "PELICULA" },
            videos = items.count { it.type == "VIDEO" }
        )
    }

    private fun itemToJson(item: ContentEntity): JSONObject = JSONObject().apply {
        put("type", item.type)
        put("name", item.name)
        put("imageUri", item.imageUri ?: JSONObject.NULL)
        put("imageSource", item.imageSource)
        put("season", item.season)
        put("episode", item.episode)
        put("minute", item.minute)
        put("platform", item.platform ?: JSONObject.NULL)
        put("tvMazeId", item.tvMazeId ?: JSONObject.NULL)
        put("remoteSeason", item.remoteSeason)
        put("remoteEpisode", item.remoteEpisode)
        put("isPinnedToWidget", item.isPinnedToWidget)
    }

    private fun jsonToItem(obj: JSONObject): ContentEntity? {
        val type = obj.optString("type", "")
        if (type !in setOf("SERIE", "PELICULA", "VIDEO")) return null
        val name = obj.optString("name", "").trim()
        if (name.isEmpty()) return null

        return ContentEntity(
            // id = 0 para que Room le asigne uno nuevo: nunca pisamos filas existentes.
            id = 0,
            type = type,
            name = name,
            imageUri = obj.optStringOrNull("imageUri"),
            imageSource = obj.optString("imageSource", "NONE"),
            season = obj.optInt("season", 0),
            episode = obj.optInt("episode", 0),
            minute = obj.optInt("minute", 0),
            platform = obj.optStringOrNull("platform"),
            tvMazeId = if (obj.isNull("tvMazeId")) null else obj.optInt("tvMazeId"),
            remoteSeason = obj.optInt("remoteSeason", 0),
            remoteEpisode = obj.optInt("remoteEpisode", 0),
            isPinnedToWidget = obj.optBoolean("isPinnedToWidget", false),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
