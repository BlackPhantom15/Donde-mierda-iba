package com.blackphantom.dondemierdaiba.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ContentEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "donde_mierda_iba.db"
                )
                    // Se agregaron columnas nuevas para el vínculo con TVmaze (v2).
                    // Como todavía no hay una base real en producción, se opta por
                    // recrear la tabla en vez de escribir una migración manual.
                    // Si ya tenés contenido cargado en el dispositivo, se pierde al
                    // actualizar a esta versión.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
