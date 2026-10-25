package com.jay.launcher.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.webkit.URLUtil
import android.widget.Toast
import com.jay.launcher.R

object Urls {

    /** Adds https:// when the scheme is missing. Returns null if the result isn't a valid web URL. */
    fun normalize(input: String): String? {
        var url = input.trim()
        if (url.isEmpty()) return null
        if (!url.contains("://")) url = "https://$url"
        if (!URLUtil.isHttpUrl(url) && !URLUtil.isHttpsUrl(url)) return null
        val host = Uri.parse(url).host
        if (host.isNullOrBlank() || !host.contains('.') && host != "localhost") return null
        return url
    }

    fun host(url: String): String = Uri.parse(url).host?.removePrefix("www.") ?: url

    fun faviconFor(url: String): String =
        "https://www.google.com/s2/favicons?sz=128&domain=" + Uri.parse(url).host

    fun openExternally(context: Context, url: String): Boolean {
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
            true
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
