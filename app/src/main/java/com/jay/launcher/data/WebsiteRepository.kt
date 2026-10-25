package com.jay.launcher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.io.File

/**
 * Stores the website list as a JSON array in SharedPreferences.
 * List order is the display order on the home screen.
 */
class WebsiteRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("jay_websites", Context.MODE_PRIVATE)

    fun getAll(): List<Website> {
        val raw = prefs.getString(KEY_SITES, null) ?: return seedDefaults()
        return try {
            parse(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getEnabled(): List<Website> = getAll().filter { it.enabled }

    fun get(id: String): Website? = getAll().firstOrNull { it.id == id }

    fun saveAll(sites: List<Website>) {
        prefs.edit().putString(KEY_SITES, toJson(sites)).apply()
    }

    fun add(site: Website) = saveAll(getAll() + site)

    fun update(site: Website) = saveAll(getAll().map { if (it.id == site.id) site else it })

    fun delete(id: String) = saveAll(getAll().filterNot { it.id == id })

    /** Deletes saved gallery photos that no website uses any more. */
    fun cleanupIcons(iconDir: File) {
        val used = getAll().map { it.iconUrl.removePrefix("file:") }.toSet()
        iconDir.listFiles()?.filter { it.absolutePath !in used }?.forEach { it.delete() }
    }

    fun exportJson(): String = JSONArray(getAll().map { it.toJson() }).toString(2)

    /** Replaces the current list with the sites in [json]. Returns the number imported. */
    fun importJson(json: String): Int {
        val sites = parse(json)
        saveAll(sites)
        return sites.size
    }

    private fun seedDefaults(): List<Website> {
        val defaults = listOf(
            Website(
                name = "My GitHub Portfolio",
                url = "https://github.com/JayashankaAnushan19",
                iconUrl = "https://github.com/JayashankaAnushan19.png",
                description = "Projects & code",
            )
        )
        saveAll(defaults)
        return defaults
    }

    private fun parse(raw: String): List<Website> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { Website.fromJson(arr.getJSONObject(it)) }
    }

    private fun toJson(sites: List<Website>) = JSONArray(sites.map { it.toJson() }).toString()

    companion object {
        private const val KEY_SITES = "sites"
    }
}
