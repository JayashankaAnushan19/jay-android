package com.jay.launcher.ui

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.chip.ChipGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jay.launcher.R
import com.jay.launcher.data.AdminAuth
import com.jay.launcher.data.Website
import com.jay.launcher.data.WebsiteRepository
import com.jay.launcher.databinding.ActivitySettingsBinding
import com.jay.launcher.databinding.DialogChangePasswordBinding
import com.jay.launcher.databinding.DialogLoginBinding
import com.jay.launcher.databinding.DialogWebsiteBinding
import com.jay.launcher.databinding.ItemAdminWebsiteBinding
import com.jay.launcher.util.IconStore
import com.jay.launcher.util.Urls

/** Password-protected admin panel: manage websites, change password, backup. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var repo: WebsiteRepository
    private lateinit var auth: AdminAuth
    private lateinit var adapter: AdminAdapter
    private lateinit var touchHelper: ItemTouchHelper

    private var unlocked = false
    private var loginDialog: AlertDialog? = null
    /** True while a system file picker is open, so leaving the screen doesn't re-lock it. */
    private var awaitingPicker = false

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        awaitingPicker = false
        uri?.let { exportTo(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        awaitingPicker = false
        uri?.let { confirmImport(it) }
    }

    /** Receives the icon value for the add/edit dialog that opened the gallery. */
    private var onIconPicked: ((String) -> Unit)? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        awaitingPicker = false
        if (uri == null) return@registerForActivityResult
        Thread {
            val value = IconStore.save(this, uri)
            runOnUiThread {
                if (value != null) onIconPicked?.invoke(value)
                else Toast.makeText(this, R.string.icon_failed, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repo = WebsiteRepository(this)
        auth = AdminAuth(this)
        unlocked = savedInstanceState?.getBoolean(STATE_UNLOCKED) ?: false

        adapter = AdminAdapter()
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter
        touchHelper = ItemTouchHelper(DragCallback())
        touchHelper.attachToRecyclerView(binding.list)

        binding.add.setOnClickListener { showWebsiteDialog(null) }
        binding.changePassword.setOnClickListener { showChangePasswordDialog() }
    }

    override fun onStart() {
        super.onStart()
        if (unlocked) showContent() else promptLogin()
    }

    override fun onStop() {
        super.onStop()
        // Lock again when the user leaves the app, but not on rotation or while picking a file.
        if (!isChangingConfigurations && !awaitingPicker) {
            unlocked = false
            binding.content.visibility = View.INVISIBLE
            binding.add.visibility = View.GONE
            invalidateOptionsMenu()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_UNLOCKED, unlocked && isChangingConfigurations)
    }

    override fun onDestroy() {
        loginDialog?.dismiss()
        if (isFinishing) repo.cleanupIcons(IconStore.dir(this))
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
    }

    // ---- Login ----

    private fun promptLogin() {
        if (loginDialog?.isShowing == true) return
        val b = DialogLoginBinding.inflate(layoutInflater)
        b.hint.visibility = if (auth.isDefaultPassword) View.VISIBLE else View.GONE

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.admin_login)
            .setView(b.root)
            .setCancelable(false)
            .setPositiveButton(R.string.unlock, null)
            .setNegativeButton(R.string.cancel) { _, _ -> finish() }
            .create()

        val attempt = {
            val locked = auth.lockoutRemainingMs()
            when {
                locked > 0 -> b.passwordLayout.error = getString(R.string.locked_out, (locked / 1000 + 1).toInt())
                auth.verify(b.password.text.toString()) -> {
                    dialog.dismiss()
                    unlocked = true
                    showContent()
                }
                else -> {
                    val nowLocked = auth.lockoutRemainingMs()
                    b.passwordLayout.error = if (nowLocked > 0) {
                        getString(R.string.locked_out, (nowLocked / 1000 + 1).toInt())
                    } else {
                        getString(R.string.wrong_password)
                    }
                    b.password.text = null
                }
            }
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { attempt() }
        }
        b.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { attempt(); true } else false
        }
        loginDialog = dialog
        dialog.show()
        b.password.requestFocus()
    }

    private fun showContent() {
        binding.content.visibility = View.VISIBLE
        binding.add.visibility = View.VISIBLE
        binding.defaultWarning.visibility = if (auth.isDefaultPassword) View.VISIBLE else View.GONE
        refreshList()
        invalidateOptionsMenu()
    }

    private fun refreshList() {
        adapter.items = repo.getAll().toMutableList()
        adapter.notifyDataSetChanged()
        updateEmpty()
    }

    private fun updateEmpty() {
        binding.empty.visibility = if (adapter.items.isEmpty()) View.VISIBLE else View.GONE
    }

    // ---- Add / edit ----

    private fun showWebsiteDialog(existing: Website?) {
        val b = DialogWebsiteBinding.inflate(layoutInflater)
        var icon = existing?.iconUrl.orEmpty()
        val refreshPreview = { SiteIcon.bind(b.preview, b.name.text.toString().ifBlank { "?" }, icon) }
        val setIcon = { value: String ->
            icon = value
            refreshPreview()
        }

        existing?.let {
            b.name.setText(it.name)
            b.description.setText(it.description)
            b.url.setText(it.url)
        }
        if (icon.startsWith("http")) {
            b.link.setText(icon)
            b.linkLayout.visibility = View.VISIBLE
        }
        refreshPreview()
        b.name.doAfterTextChanged { refreshPreview() }

        b.chipGallery.setOnClickListener {
            onIconPicked = setIcon
            awaitingPicker = true
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        b.chipAvatar.setOnClickListener { showAvatarPicker(icon, setIcon) }
        b.chipSite.setOnClickListener {
            val url = Urls.normalize(b.url.text.toString())
            if (url == null) {
                b.urlLayout.error = getString(R.string.site_icon_needs_url)
            } else {
                b.urlLayout.error = null
                setIcon(Urls.faviconFor(url))
            }
        }
        b.chipLink.setOnClickListener {
            b.linkLayout.visibility = View.VISIBLE
            b.link.requestFocus()
        }
        b.chipLetter.setOnClickListener {
            b.linkLayout.visibility = View.GONE
            setIcon("")
        }
        b.link.doAfterTextChanged {
            val link = it.toString().trim()
            if (link.isEmpty()) return@doAfterTextChanged
            Urls.normalize(link)?.let(setIcon)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.add_website else R.string.edit_website)
            .setView(b.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = b.name.text.toString().trim()
                val description = b.description.text.toString().trim()
                val url = Urls.normalize(b.url.text.toString())
                b.nameLayout.error = if (name.isEmpty()) getString(R.string.error_name_required) else null
                b.urlLayout.error = if (url == null) getString(R.string.error_invalid_url) else null
                if (name.isEmpty() || url == null) return@setOnClickListener

                if (existing == null) {
                    repo.add(Website(name = name, url = url, iconUrl = icon, description = description))
                } else {
                    repo.update(existing.copy(name = name, url = url, iconUrl = icon, description = description))
                }
                refreshList()
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { onIconPicked = null }
        dialog.show()
    }

    private fun showAvatarPicker(current: String, onPick: (String) -> Unit) {
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        var colorIndex = if (current.startsWith("avatar:")) current.split(':').getOrNull(2)?.toIntOrNull() ?: 0 else 0

        val colorRow = ChipGroup(this)
        val iconGrid = ChipGroup(this).apply {
            chipSpacingHorizontal = dp(10)
            chipSpacingVertical = dp(10)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(colorRow)
            addView(iconGrid, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        }
        lateinit var dialog: AlertDialog

        fun tile(color: Int, radius: Float, selected: Boolean = false) = GradientDrawable().apply {
            cornerRadius = radius
            setColor(color)
            if (selected) setStroke(dp(3), ContextCompat.getColor(this@SettingsActivity, R.color.text_primary))
        }

        fun render() {
            colorRow.removeAllViews()
            Avatars.colors.forEachIndexed { i, color ->
                colorRow.addView(View(this).apply {
                    layoutParams = ViewGroup.LayoutParams(dp(30), dp(30))
                    background = tile(color, dp(15).toFloat(), i == colorIndex)
                    setOnClickListener {
                        colorIndex = i
                        render()
                    }
                })
            }
            iconGrid.removeAllViews()
            Avatars.icons.forEach { (key, res) ->
                iconGrid.addView(ImageView(this).apply {
                    layoutParams = ViewGroup.LayoutParams(dp(54), dp(54))
                    background = tile(Avatars.colors[colorIndex], dp(16).toFloat())
                    setImageResource(res)
                    setPadding(dp(14), dp(14), dp(14), dp(14))
                    contentDescription = key
                    setOnClickListener {
                        onPick(Avatars.encode(key, colorIndex))
                        dialog.dismiss()
                    }
                })
            }
        }
        render()

        dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_avatar)
            .setView(root)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(site: Website) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.delete_confirm, site.name))
            .setPositiveButton(R.string.delete) { _, _ ->
                val index = adapter.items.indexOfFirst { it.id == site.id }
                repo.delete(site.id)
                refreshList()
                Snackbar.make(binding.root, R.string.deleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.add)
                    .setAction(R.string.undo) {
                        val list = repo.getAll().toMutableList()
                        list.add(index.coerceIn(0, list.size), site)
                        repo.saveAll(list)
                        refreshList()
                    }
                    .show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---- Password ----

    private fun showChangePasswordDialog() {
        val b = DialogChangePasswordBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.change_password)
            .setView(b.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val current = b.current.text.toString()
                val new = b.newPassword.text.toString()
                val confirm = b.confirm.text.toString()
                b.currentLayout.error = null
                b.newLayout.error = null
                b.confirmLayout.error = null

                when {
                    new.length < AdminAuth.MIN_LENGTH ->
                        b.newLayout.error = getString(R.string.password_too_short, AdminAuth.MIN_LENGTH)
                    new != confirm -> b.confirmLayout.error = getString(R.string.passwords_dont_match)
                    auth.lockoutRemainingMs() > 0 ->
                        b.currentLayout.error = getString(R.string.locked_out, (auth.lockoutRemainingMs() / 1000 + 1).toInt())
                    !auth.verify(current) -> b.currentLayout.error = getString(R.string.wrong_password)
                    else -> {
                        auth.setPassword(new)
                        binding.defaultWarning.visibility = View.GONE
                        Toast.makeText(this, R.string.password_changed, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    // ---- Backup ----

    private fun exportTo(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(repo.exportJson().toByteArray()) }
            Toast.makeText(this, R.string.exported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmImport(uri: Uri) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.import_links)
            .setMessage(R.string.import_confirm)
            .setPositiveButton(R.string.import_action) { _, _ ->
                try {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val count = repo.importJson(json)
                    refreshList()
                    Toast.makeText(this, getString(R.string.imported, count), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---- Menu ----

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (unlocked) menuInflater.inflate(R.menu.settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home, R.id.action_lock -> finish()
            R.id.action_export -> {
                awaitingPicker = true
                exportLauncher.launch("jay-websites.json")
            }
            R.id.action_import -> {
                awaitingPicker = true
                importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    // ---- List ----

    private inner class AdminAdapter : RecyclerView.Adapter<AdminAdapter.Holder>() {
        var items: MutableList<Website> = mutableListOf()

        inner class Holder(val b: ItemAdminWebsiteBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemAdminWebsiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val site = items[position]
            val b = holder.b
            b.name.text = site.name
            b.url.text = if (site.enabled) site.url else getString(R.string.hidden) + " · " + site.url
            b.root.alpha = if (site.enabled) 1f else 0.55f
            SiteIcon.bind(b.icon, site.name, site.iconUrl)

            b.enabled.setOnCheckedChangeListener(null)
            b.enabled.isChecked = site.enabled
            b.enabled.setOnCheckedChangeListener { _, checked ->
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
                val updated = items[pos].copy(enabled = checked)
                items[pos] = updated
                repo.update(updated)
                notifyItemChanged(pos)
            }

            b.root.setOnClickListener { showWebsiteDialog(items[holder.bindingAdapterPosition]) }
            b.delete.setOnClickListener { confirmDelete(items[holder.bindingAdapterPosition]) }
            b.drag.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) touchHelper.startDrag(holder)
                false
            }
        }
    }

    private inner class DragCallback : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun isLongPressDragEnabled() = true

        override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
            val a = from.bindingAdapterPosition
            val b = to.bindingAdapterPosition
            adapter.items.add(b, adapter.items.removeAt(a))
            adapter.notifyItemMoved(a, b)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(rv, viewHolder)
            repo.saveAll(adapter.items)
        }
    }

    companion object {
        private const val STATE_UNLOCKED = "unlocked"
    }
}
