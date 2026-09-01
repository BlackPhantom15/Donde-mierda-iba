package com.blackphantom.dondemierdaiba.util

import org.json.JSONObject

/**
 * Codifica/decodifica el mapa "temporada -> cantidad de capítulos" que
 * guardamos en ContentEntity.episodeCountsJson, para limitar el candado de
 * capítulo al máximo real de cada temporada.
 */
object EpisodeCounts {

    fun toJson(counts: Map<Int, Int>): String {
        val obj = JSONObject()
        counts.forEach { (season, count) -> obj.put(season.toString(), count) }
        return obj.toString()
    }

    fun fromJson(json: String?): Map<Int, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<Int, Int>()
            obj.keys().forEach { key ->
                key.toIntOrNull()?.let { season -> result[season] = obj.optInt(key, 0) }
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
