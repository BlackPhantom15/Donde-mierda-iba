package com.blackphantom.dondemierdaiba.util

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "dmi_prefs"
    private const val KEY_GRID = "is_grid_view"
    private const val KEY_BATTERY_PROMPT_SHOWN = "battery_prompt_shown"
    private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
    private const val KEY_TMDB_API_KEY = "tmdb_api_key"
    private const val KEY_UPDATE_CHECK = "update_check_enabled"
    private const val KEY_PENDING_UPDATE = "pending_update_tag"
    private const val KEY_LAST_NOTIFIED_UPDATE = "last_notified_update_tag"
    private const val KEY_LAST_UPDATE_CHECK = "last_update_check_ms"

    fun isGridView(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_GRID, false)

    fun setGridView(context: Context, grid: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GRID, grid).apply()
    }

    /** Si ya le preguntamos una vez sobre excluir la app del ahorro de batería. */
    fun wasBatteryPromptShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BATTERY_PROMPT_SHOWN, false)

    fun setBatteryPromptShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BATTERY_PROMPT_SHOWN, true).apply()
    }

    /** Si ya le mostramos el tour guiado de bienvenida. */
    fun wasOnboardingShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_SHOWN, false)

    fun setOnboardingShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONBOARDING_SHOWN, true).apply()
    }

    /** Clave de TMDb que cargó la persona (vacía si no cargó ninguna). */
    fun getTmdbKey(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TMDB_API_KEY, "").orEmpty()

    fun setTmdbKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TMDB_API_KEY, key.trim()).apply()
    }

    // ---------- Actualizaciones de la app ----------

    /** Si la persona quiere que la app le avise de versiones nuevas (por defecto sí). */
    fun isUpdateCheckEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_UPDATE_CHECK, true)

    fun setUpdateCheckEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_UPDATE_CHECK, enabled).apply()
    }

    /** Última versión nueva detectada (ej: "v2.1"); vacía si no hay ninguna. */
    fun getPendingUpdate(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PENDING_UPDATE, "").orEmpty()

    fun setPendingUpdate(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PENDING_UPDATE, tag).apply()
    }

    fun getLastNotifiedUpdate(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_NOTIFIED_UPDATE, "").orEmpty()

    fun setLastNotifiedUpdate(context: Context, tag: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_NOTIFIED_UPDATE, tag).apply()
    }

    fun getLastUpdateCheck(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_UPDATE_CHECK, 0L)

    fun setLastUpdateCheck(context: Context, millis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_UPDATE_CHECK, millis).apply()
    }
}
