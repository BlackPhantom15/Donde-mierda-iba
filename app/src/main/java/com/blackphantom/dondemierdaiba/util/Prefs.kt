package com.blackphantom.dondemierdaiba.util

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "dmi_prefs"
    private const val KEY_GRID = "is_grid_view"

    fun isGridView(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_GRID, false)

    fun setGridView(context: Context, grid: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GRID, grid).apply()
    }
}
