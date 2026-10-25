package com.jay.launcher.data

import org.json.JSONObject
import java.util.UUID

/**
 * [iconUrl] holds one of: "" (letter), "avatar:<key>:<colorIndex>", "file:<path>" (gallery photo),
 * or an http(s) image link.
 */
data class Website(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val iconUrl: String = "",
    val enabled: Boolean = true,
    val description: String = "",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("url", url)
        .put("iconUrl", iconUrl)
        .put("enabled", enabled)
        .put("description", description)

    companion object {
        fun fromJson(o: JSONObject) = Website(
            id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
            name = o.getString("name"),
            url = o.getString("url"),
            iconUrl = o.optString("iconUrl"),
            enabled = o.optBoolean("enabled", true),
            description = o.optString("description"),
        )
    }
}
