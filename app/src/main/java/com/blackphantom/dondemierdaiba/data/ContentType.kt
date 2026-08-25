package com.blackphantom.dondemierdaiba.data

enum class ContentType {
    SERIE, PELICULA, VIDEO
}

enum class ImageSource {
    LOCAL, WEB, PRESET, NONE
}

enum class VideoPlatform(val label: String, val colorHex: String) {
    YOUTUBE("YouTube", "#FF3B3B"),
    TWITCH("Twitch", "#9146FF"),
    KICK("Kick", "#53FC18"),
    TIKTOK("TikTok", "#25F4EE"),
    FACEBOOK("Facebook", "#3B82F6"),
    X("X", "#E5E5E5");

    companion object {
        fun fromNameOrNull(value: String?): VideoPlatform? =
            values().firstOrNull { it.name == value }
    }
}
