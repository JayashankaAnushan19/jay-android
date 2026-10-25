package com.jay.launcher.ui

import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jay.launcher.R
import com.jay.launcher.util.IconLoader

/** Fills a container that includes view_site_icon.xml as a rounded tile. */
object SiteIcon {

    fun bind(container: View, name: String, icon: String) {
        val letter = container.findViewById<TextView>(R.id.letter)
        val glyph = container.findViewById<ImageView>(R.id.glyph)
        val image = container.findViewById<ImageView>(R.id.image)
        val size = container.layoutParams.height.toFloat()

        fun tile(color: Int) = GradientDrawable().apply {
            cornerRadius = size * 0.3f
            setColor(color)
        }

        letter.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * 0.42f)
        image.setImageDrawable(null)
        image.visibility = View.INVISIBLE
        glyph.visibility = View.GONE

        val avatar = Avatars.decode(icon)
        if (avatar != null) {
            letter.text = ""
            letter.background = tile(avatar.second)
            letter.visibility = View.VISIBLE
            glyph.setImageResource(avatar.first)
            val pad = (size * 0.24f).toInt()
            glyph.setPadding(pad, pad, pad, pad)
            glyph.visibility = View.VISIBLE
            return
        }

        letter.text = name.trim().firstOrNull()?.uppercase() ?: "?"
        letter.background = tile(Avatars.colorFor(name))
        letter.visibility = View.VISIBLE

        val isPhoto = icon.startsWith("file:")
        image.scaleType = if (isPhoto) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
        val pad = if (isPhoto) 0 else (size * 0.14f).toInt()
        image.setPadding(pad, pad, pad, pad)
        image.background = if (isPhoto) null else tile(ContextCompat.getColor(container.context, R.color.icon_tile))

        IconLoader.load(icon, image) { shown ->
            image.visibility = if (shown) View.VISIBLE else View.INVISIBLE
            letter.visibility = if (shown) View.INVISIBLE else View.VISIBLE
        }
    }
}
