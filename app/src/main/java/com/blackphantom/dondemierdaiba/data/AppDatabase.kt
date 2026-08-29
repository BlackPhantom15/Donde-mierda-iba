package com.blackphantom.dondemierdaiba.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(entities = [ContentEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "donde_mierda_iba.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // Red de seguridad solo para saltos de versión muy viejos que no
                    // tengan un camino de migración explícito (ej: alguien que viene
                    // de la v1 original). Para el caso normal (v2 -> v3) se usa la
                    // migración real de arriba y no se pierde nada.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
