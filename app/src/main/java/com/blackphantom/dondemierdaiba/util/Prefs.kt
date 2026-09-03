package com.blackphantom.dondemierdaiba.util

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "dmi_prefs"
    private const val KEY_GRID = "is_grid_view"
    private const val KEY_BATTERY_PROMPT_SHOWN = "battery_prompt_shown"
    private const val KEY_ONBOARDING_SHOWN = "onboarding_shown"

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
}
