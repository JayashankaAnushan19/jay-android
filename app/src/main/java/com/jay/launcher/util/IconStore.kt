package com.jay.launcher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.UUID
import kotlin.math.min

/** Copies photos picked from the gallery into private app storage as small square PNGs. */
object IconStore {

    private const val SIZE = 256

    fun dir(context: Context) = File(context.filesDir, "icons").apply { mkdirs() }

    /** Returns a "file:<path>" icon value, or null if the image couldn't be read. Call off the main thread. */
    fun save(context: Context, uri: Uri): String? = try {
        val bmp = decode(context, uri) ?: throw IllegalArgumentException()
        val side = min(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)
        val file = File(dir(context), UUID.randomUUID().toString() + ".png")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        "file:" + file.absolutePath
    } catch (e: Exception) {
        null
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > SIZE * 4) decoder.setTargetSampleSize(longest / (SIZE * 2))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }
}
