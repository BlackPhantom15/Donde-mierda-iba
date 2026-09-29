package com.blackphantom.dondemierdaiba.util

import org.json.JSONObject

/**
 * Calificación por capítulo (solo SERIE), guardada como JSON con clave
 * "temporada-capítulo" -> estrellas (1 a 5). De acá sale el promedio por
 * temporada que se muestra en el editor. Mismo patrón que EpisodeCounts.
 */
object EpisodeRatings {

    private fun key(season: Int, episode: Int) = "$season-$episode"

    fun fromJson(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, Int>()
            obj.keys().forEach { k -> map[k] = obj.optInt(k, 0) }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun toJson(map: Map<String, Int>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    /** Devuelve el JSON actualizado con la nueva calificación de ese capítulo. */
    fun rate(json: String?, season: Int, episode: Int, stars: Int): String {
        val map = fromJson(json).toMutableMap()
        if (stars <= 0) map.remove(key(season, episode)) else map[key(season, episode)] = stars.coerceIn(1, 5)
        return toJson(map)
    }

    fun ratingFor(json: String?, season: Int, episode: Int): Int =
        fromJson(json)[key(season, episode)] ?: 0

    /** Promedio (0.0 a 5.0) y cantidad de capítulos calificados de una temporada. */
    fun seasonAverage(json: String?, season: Int): Pair<Float, Int> {
        val ratings = fromJson(json)
            .filterKeys { it.startsWith("$season-") }
            .values
        if (ratings.isEmpty()) return 0f to 0
        return (ratings.sum().toFloat() / ratings.size) to ratings.size
    }
}
