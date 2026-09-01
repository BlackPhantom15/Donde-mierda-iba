package com.blackphantom.dondemierdaiba.util

object DurationFormatter {
    /** 45 -> "45 min" · 90 -> "1h 30min" · 120 -> "2h" */
    fun format(totalMinutes: Int): String {
        if (totalMinutes < 60) return "$totalMinutes min"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (minutes == 0) "${hours}h" else "${hours}h ${minutes}min"
    }
}
