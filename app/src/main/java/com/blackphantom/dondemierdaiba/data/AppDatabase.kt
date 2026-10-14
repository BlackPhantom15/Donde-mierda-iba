package com.blackphantom.dondemierdaiba.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [ContentEntity::class, NotificationLogEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v2 -> v3: se agregó la columna isPinnedToWidget (series "viendo ahora"
        // para el widget). Es un ALTER TABLE simple, así que escribimos la
        // migración real en vez de recrear la base y perder lo ya cargado.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE content_items ADD COLUMN isPinnedToWidget INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // v3 -> v4: se agregó episodeCountsJson (capítulos por temporada,
        // según TVmaze) para limitar el candado de capítulo al máximo real.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE content_items ADD COLUMN episodeCountsJson TEXT"
                )
            }
        }

        // v4 -> v5: calificación con estrellas (rating, episodeRatingsJson) y
        // marcado de "visto completo" (episodeWatchedJson, isCompleted).
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE content_items ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE content_items ADD COLUMN episodeRatingsJson TEXT")
                db.execSQL("ALTER TABLE content_items ADD COLUMN episodeWatchedJson TEXT")
                db.execSQL("ALTER TABLE content_items ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v5 -> v6: historial de notificaciones (campanita).
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notification_log (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "itemId INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "body TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "isRead INTEGER NOT NULL)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "donde_mierda_iba.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    // Red de seguridad solo para saltos de versión muy viejos que no
                    // tengan un camino de migración explícito. Para los casos
                    // normales (v2 -> v3 -> v4) se usan las migraciones reales de
                    // arriba y no se pierde nada.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
