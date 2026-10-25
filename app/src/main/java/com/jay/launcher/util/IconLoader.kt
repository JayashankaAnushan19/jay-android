package com.jay.launcher.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Minimal image loader with an in-memory cache, so the app needs no image library.
 * Handles http(s) links and "file:<path>" gallery photos.
 */
object IconLoader {

    private val cache = LruCache<String, Bitmap>(64)
    private val failed = mutableSetOf<String>()
    private val executor = Executors.newFixedThreadPool(3)
    private val main = Handler(Looper.getMainLooper())

    /**
     * Loads [url] into [view]. Calls [onResult] on the main thread with true when an image was
     * shown, false when it could not be loaded (so the caller can show a fallback).
     */
    fun load(url: String, view: ImageView, onResult: (Boolean) -> Unit) {
        view.tag = url
        if (url.isBlank() || url in failed) return onResult(false)
        cache.get(url)?.let {
            view.setImageBitmap(it)
            return onResult(true)
        }
        onResult(false)
        executor.execute {
            val bmp = if (url.startsWith("file:")) BitmapFactory.decodeFile(url.removePrefix("file:")) else fetch(url)
            main.post {
                if (bmp == null) failed += url else cache.put(url, bmp)
                if (view.tag == url && bmp != null) {
                    view.setImageBitmap(bmp)
                    onResult(true)
                }
            }
        }
    }

    private fun fetch(url: String): Bitmap? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.instanceFollowRedirects = true
        conn.inputStream.use { stream ->
            BitmapFactory.decodeStream(stream)?.let { scale(it) }
        }.also { conn.disconnect() }
    } catch (e: Exception) {
        null
    }

    private fun scale(bmp: Bitmap): Bitmap {
        val max = 192
        if (bmp.width <= max && bmp.height <= max) return bmp
        val ratio = max.toFloat() / maxOf(bmp.width, bmp.height)
        return Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
    }
}
