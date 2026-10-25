package com.jay.launcher.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Admin password stored as a salted PBKDF2-SHA256 hash. The plain password is never saved.
 * Repeated wrong attempts trigger a temporary lockout.
 */
class AdminAuth(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("jay_admin", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_HASH)) setPassword(DEFAULT_PASSWORD, isDefault = true)
    }

    val isDefaultPassword: Boolean get() = prefs.getBoolean(KEY_IS_DEFAULT, true)

    /** Milliseconds left in the current lockout, or 0. */
    fun lockoutRemainingMs(): Long {
        val until = prefs.getLong(KEY_LOCK_UNTIL, 0)
        val now = System.currentTimeMillis()
        return if (until > now) until - now else 0
    }

    fun verify(password: String): Boolean {
        if (lockoutRemainingMs() > 0) return false
        val salt = Base64.decode(prefs.getString(KEY_SALT, ""), Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString(KEY_HASH, ""), Base64.NO_WRAP)
        val ok = MessageDigest.isEqual(hash(password, salt), expected)
        if (ok) {
            prefs.edit().putInt(KEY_FAILS, 0).apply()
        } else {
            val fails = prefs.getInt(KEY_FAILS, 0) + 1
            val edit = prefs.edit().putInt(KEY_FAILS, fails)
            if (fails >= MAX_ATTEMPTS) {
                edit.putLong(KEY_LOCK_UNTIL, System.currentTimeMillis() + LOCKOUT_MS).putInt(KEY_FAILS, 0)
            }
            edit.apply()
        }
        return ok
    }

    fun setPassword(password: String, isDefault: Boolean = false) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash(password, salt), Base64.NO_WRAP))
            .putBoolean(KEY_IS_DEFAULT, isDefault)
            .apply()
    }

    private fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val DEFAULT_PASSWORD = "admin"
        const val MIN_LENGTH = 4
        private const val ITERATIONS = 60_000
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS = 30_000L
        private const val KEY_HASH = "hash"
        private const val KEY_SALT = "salt"
        private const val KEY_IS_DEFAULT = "is_default"
        private const val KEY_FAILS = "fails"
        private const val KEY_LOCK_UNTIL = "lock_until"
    }
}
