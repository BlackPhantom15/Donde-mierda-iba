package com.blackphantom.dondemierdaiba.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_items")
data class ContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: String,          // ContentType.name
    val name: String,

    val imageUri: String? = null,     // uri local, url web, o nombre de plataforma preestablecida
    val imageSource: String = ImageSource.NONE.name,

    val season: Int = 0,       // solo SERIE
    val episode: Int = 0,      // solo SERIE
    val minute: Int = 0,       // SERIE / PELICULA / VIDEO
    val platform: String? = null, // solo VIDEO

    // Vínculo con TVmaze (solo SERIE). Si es null, la serie no se encontró
    // en TVmaze o el usuario la cargó sin conexión, y no se chequean novedades.
    val tvMazeId: Int? = null,
    val remoteSeason: Int = 0,   // última temporada emitida que ya conocíamos
    val remoteEpisode: Int = 0,  // último capítulo emitido que ya conocíamos

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val typeEnum: ContentType get() = ContentType.valueOf(type)
    val imageSourceEnum: ImageSource get() = try {
        ImageSource.valueOf(imageSource)
    } catch (e: Exception) {
        ImageSource.NONE
    }
}
