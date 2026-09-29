package com.blackphantom.dondemierdaiba.util

import org.json.JSONObject

/**
 * Qué capítulos quedaron marcados como "visto completo" (solo SERIE), guardado
 * como JSON con clave "temporada-capítulo" -> true. De acá se calcula sola
 * cuándo una temporada (o la serie entera) quedó completa, comparando contra
 * lo que ya sabemos por EpisodeCounts (capítulos reales de cada temporada).
 */
object EpisodeWatched {

    private fun key(season: Int, episode: Int) = "$season-$episode"

    fun fromJson(json: String?): Set<String> {
        if (json.isNullOrBlank()) return emptySet()
        return try {
            val obj = JSONObject(json)
            obj.keys().asSequence().filter { obj.optBoolean(it, false) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun toJson(set: Set<String>): String {
        val obj = JSONObject()
        set.forEach { obj.put(it, true) }
        return obj.toString()
    }

    fun isWatched(json: String?, season: Int, episode: Int): Boolean =
        key(season, episode) in fromJson(json)

    /** Devuelve el JSON con ese capítulo marcado (o desmarcado) como visto. */
    fun setWatched(json: String?, season: Int, episode: Int, watched: Boolean): String {
        val set = fromJson(json).toMutableSet()
        if (watched) set.add(key(season, episode)) else set.remove(key(season, episode))
        return toJson(set)
    }

    /** Temporada completa = todos sus capítulos conocidos (según TVmaze) están vistos. */
    fun isSeasonComplete(watchedJson: String?, countsJson: String?, season: Int): Boolean {
        val total = EpisodeCounts.fromJson(countsJson)[season] ?: return false
        if (total <= 0) return false
        val watched = fromJson(watchedJson)
        return (1..total).all { ep -> key(season, ep) in watched }
    }

    /** Serie completa = todas las temporadas que conocemos (según TVmaze) están completas. */
    fun isSeriesComplete(watchedJson: String?, countsJson: String?): Boolean {
        val counts = EpisodeCounts.fromJson(countsJson)
        if (counts.isEmpty()) return false
        return counts.keys.all { season -> isSeasonComplete(watchedJson, countsJson, season) }
    }
}
