package com.jay.launcher

import android.app.Application
import android.content.Context
import android.os.Build

/** Saves the details of a crash so the next launch can show them (there is no logcat on the phone). */
class JayApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            val report = buildString {
                append("Jay ").append(BuildConfig.VERSION_NAME)
                append(" · Android ").append(Build.VERSION.RELEASE)
                append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\n\n")
                append(error.stackTraceToString().take(6000))
            }
            prefs(this).edit().putString(KEY_CRASH, report).commit()
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        private const val KEY_CRASH = "last_crash"

        private fun prefs(context: Context) = context.getSharedPreferences("jay_crash", Context.MODE_PRIVATE)

        /** Returns and clears the last saved crash report, if any. */
        fun takeCrashReport(context: Context): String? {
            val p = prefs(context)
            val report = p.getString(KEY_CRASH, null) ?: return null
            p.edit().remove(KEY_CRASH).apply()
            return report
        }
    }
}
