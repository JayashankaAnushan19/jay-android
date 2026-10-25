package com.jay.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jay.launcher.JayApp
import com.jay.launcher.R
import com.jay.launcher.data.Website
import com.jay.launcher.data.WebsiteRepository
import com.jay.launcher.databinding.ActivityMainBinding
import com.jay.launcher.databinding.ItemWebsiteBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: WebsiteRepository
    private val adapter = SitesAdapter { openSite(it) }
    private var shown: List<Website>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = WebsiteRepository(this)
        val columns = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        binding.list.layoutManager = GridLayoutManager(this, columns)
        binding.list.adapter = adapter
        binding.list.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_cards)
        binding.settings.setOnClickListener { openSettings() }
        binding.emptySettings.setOnClickListener { openSettings() }

        JayApp.takeCrashReport(this)?.let { report ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.crash_title)
                .setMessage(report)
                .setPositiveButton(R.string.copy) { _, _ ->
                    getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("Jay crash", report))
                }
                .setNegativeButton(R.string.close, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.greeting.setText(
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> R.string.greeting_morning
                in 12..16 -> R.string.greeting_afternoon
                else -> R.string.greeting_evening
            }
        )
        // Reload every time so changes made in Settings show up immediately.
        val sites = repo.getEnabled()
        if (sites != shown) {
            shown = sites
            adapter.submit(sites)
            binding.list.scheduleLayoutAnimation()
        }
        binding.empty.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }

    private fun openSite(site: Website) {
        startActivity(
            Intent(this, WebViewActivity::class.java)
                .putExtra(WebViewActivity.EXTRA_URL, site.url)
                .putExtra(WebViewActivity.EXTRA_TITLE, site.name)
                .putExtra(WebViewActivity.EXTRA_ICON, site.iconUrl)
        )
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out)
    }

    private class SitesAdapter(val onClick: (Website) -> Unit) : RecyclerView.Adapter<SitesAdapter.Holder>() {
        private var items: List<Website> = emptyList()

        fun submit(sites: List<Website>) {
            items = sites
            notifyDataSetChanged()
        }

        class Holder(val b: ItemWebsiteBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemWebsiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val site = items[position]
            holder.b.name.text = site.name
            holder.b.description.text = site.description
            holder.b.description.visibility = if (site.description.isBlank()) View.GONE else View.VISIBLE
            SiteIcon.bind(holder.b.icon, site.name, site.iconUrl)
            holder.b.root.setOnClickListener { onClick(site) }
        }
    }
}
