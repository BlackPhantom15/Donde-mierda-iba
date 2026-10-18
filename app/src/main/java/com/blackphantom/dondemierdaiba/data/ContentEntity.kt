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

    // Series marcadas por el usuario como "viendo ahora": son las que
    // aparecen en el widget de pantalla de inicio. Solo aplica a SERIE.
    val isPinnedToWidget: Boolean = false,

    // Cantidad de capítulos por temporada según TVmaze, como JSON
    // ("{"1":8,"2":10}"). Se usa para limitar el candado de capítulo al
    // máximo real de cada temporada. Null si todavía no se pudo consultar.
    val episodeCountsJson: String? = null,

    // Calificación con estrellas (0 a 5). Para PELICULA y VIDEO es una sola
    // nota. Para SERIE no se usa este campo: se califica capítulo por
    // capítulo en episodeRatingsJson.
    val rating: Int = 0,

    // Solo SERIE: calificación por capítulo, como JSON ("{"1-1":5,"1-2":4}",
    // clave "temporada-capítulo"). De acá sale el promedio por temporada.
    val episodeRatingsJson: String? = null,

    // Solo SERIE: qué capítulos quedaron marcados como "visto completo",
    // como JSON ("{"1-1":true,"1-2":true}"). Sirve para calcular sola cuándo
    // una temporada (o la serie entera) quedó completa.
    val episodeWatchedJson: String? = null,

    // PELICULA y VIDEO: si ya se marcó como visto completo.
    val isCompleted: Boolean = false,

    // Favorito: sube al tope de la lista de la app y del widget.
    val isFavorite: Boolean = false,

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
