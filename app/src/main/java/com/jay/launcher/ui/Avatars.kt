package com.jay.launcher.ui

import com.jay.launcher.R

/** Built-in avatar icons and the colours used for avatars and letter icons. */
object Avatars {

    val colors = intArrayOf(
        0xFF4F46E5.toInt(), 0xFF0EA5E9.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(),
        0xFFEF4444.toInt(), 0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(), 0xFF14B8A6.toInt(),
    )

    val icons = linkedMapOf(
        "person" to R.drawable.av_person,
        "work" to R.drawable.av_work,
        "code" to R.drawable.av_code,
        "article" to R.drawable.av_article,
        "school" to R.drawable.av_school,
        "web" to R.drawable.ic_language,
        "cart" to R.drawable.av_cart,
        "camera" to R.drawable.av_camera,
        "music" to R.drawable.av_music,
        "play" to R.drawable.av_play,
        "star" to R.drawable.av_star,
        "heart" to R.drawable.av_heart,
        "mail" to R.drawable.av_mail,
        "home" to R.drawable.av_home,
    )

    fun encode(key: String, colorIndex: Int) = "avatar:$key:$colorIndex"

    /** Returns (drawable, color) for an "avatar:" value, or null if it isn't one. */
    fun decode(value: String): Pair<Int, Int>? {
        if (!value.startsWith("avatar:")) return null
        val parts = value.split(':')
        val icon = icons[parts.getOrNull(1)] ?: return null
        val color = colors[(parts.getOrNull(2)?.toIntOrNull() ?: 0).mod(colors.size)]
        return icon to color
    }

    fun colorFor(name: String) = colors[Math.floorMod(name.hashCode(), colors.size)]
}
