package com.jay.launcher.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jay.launcher.R
import com.jay.launcher.databinding.ActivityWebviewBinding
import com.jay.launcher.util.Urls

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding
    private lateinit var startUrl: String
    private var mainFrameError = false
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startUrl = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val name = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
        supportActionBar?.apply {
            title = name
            setDisplayHomeAsUpEnabled(true)
        }
        binding.loaderName.text = name
        SiteIcon.bind(binding.loaderIcon, name, intent.getStringExtra(EXTRA_ICON).orEmpty())

        val web = binding.web
        web.setBackgroundColor(ContextCompat.getColor(this, R.color.surface))
        web.isHapticFeedbackEnabled = false
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Mobile-friendly rendering: respect the site's viewport, fit wide pages to the screen.
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            mediaPlaybackRequiresUserGesture = true
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }
        web.webViewClient = Client()
        web.webChromeClient = Chrome()
        web.setDownloadListener { url, _, _, _, _ ->
            Toast.makeText(this, R.string.download_in_browser, Toast.LENGTH_SHORT).show()
            Urls.openExternally(this, url)
        }

        binding.swipe.setColorSchemeResources(R.color.brand)
        binding.swipe.setOnChildScrollUpCallback { _, _ -> web.canScrollVertically(-1) }
        binding.swipe.setOnRefreshListener { reload() }
        binding.retry.setOnClickListener {
            showLoader()
            reload()
        }
        binding.openExternal.setOnClickListener { Urls.openExternally(this, currentUrl()) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    customView != null -> hideCustomView()
                    binding.error.visibility == View.VISIBLE && web.canGoBack() -> {
                        hideError()
                        web.goBack()
                    }
                    web.canGoBack() -> web.goBack()
                    else -> finish()
                }
            }
        })

        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) load(startUrl)
    }

    private fun currentUrl(): String = binding.web.url ?: startUrl

    private fun load(url: String) {
        if (!Urls.isOnline(this)) return showError(offline = true)
        hideError()
        binding.web.loadUrl(url)
    }

    private fun reload() {
        if (!Urls.isOnline(this)) {
            binding.swipe.isRefreshing = false
            return showError(offline = true)
        }
        hideError()
        // After a failed first load the WebView has no real page to reload.
        if (binding.web.url.isNullOrEmpty() || binding.web.url == "about:blank") binding.web.loadUrl(startUrl)
        else binding.web.reload()
    }

    private fun showLoader() {
        binding.loader.animate().cancel()
        binding.loader.alpha = 1f
        binding.loader.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        if (binding.loader.visibility != View.VISIBLE) return
        binding.loader.animate().alpha(0f).setDuration(250).withEndAction {
            binding.loader.visibility = View.GONE
        }
    }

    private fun showError(offline: Boolean) {
        binding.swipe.isRefreshing = false
        binding.progress.visibility = View.GONE
        binding.loader.animate().cancel()
        binding.loader.visibility = View.GONE
        binding.openExternal.visibility = if (offline) View.GONE else View.VISIBLE
        binding.errorTitle.setText(if (offline) R.string.no_internet_title else R.string.load_error_title)
        binding.errorMessage.setText(if (offline) R.string.no_internet_msg else R.string.load_error_msg)
        binding.error.visibility = View.VISIBLE
    }

    private fun hideError() {
        mainFrameError = false
        binding.error.visibility = View.GONE
    }

    /** Hands non-web links (tel:, mailto:, intent:, app links…) to other apps. */
    private fun handleExternal(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob") return false
        if (scheme == "intent") {
            try {
                val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    .addCategory(Intent.CATEGORY_BROWSABLE).setComponent(null)
                intent.selector = null
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val fallback = intent.getStringExtra("browser_fallback_url")
                    if (fallback != null) binding.web.loadUrl(fallback)
                }
            } catch (e: Exception) {
                Toast.makeText(this, R.string.no_app_to_open, Toast.LENGTH_SHORT).show()
            }
            return true
        }
        Urls.openExternally(this, uri.toString())
        return true
    }

    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = handleExternal(request.url)

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            // The opening screen covers the first load; later page changes get a thin bar only.
            if (binding.loader.visibility != View.VISIBLE) binding.progress.visibility = View.VISIBLE
        }

        override fun onPageCommitVisible(view: WebView, url: String?) {
            if (!mainFrameError) hideLoader()
        }

        override fun onPageFinished(view: WebView, url: String?) {
            binding.swipe.isRefreshing = false
            binding.progress.visibility = View.GONE
            if (mainFrameError) showError(offline = !Urls.isOnline(this@WebViewActivity)) else hideLoader()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                mainFrameError = true
                showError(offline = !Urls.isOnline(this@WebViewActivity))
            }
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            binding.progress.setProgressCompat(newProgress, true)
            if (newProgress >= 100) binding.progress.visibility = View.GONE
            if (newProgress >= 85 && !mainFrameError) hideLoader()
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            fileCallback?.onReceiveValue(null)
            fileCallback = filePathCallback
            return try {
                fileChooser.launch(fileChooserParams.createIntent())
                true
            } catch (e: ActivityNotFoundException) {
                fileCallback = null
                false
            }
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (customView != null) return callback.onCustomViewHidden()
            customView = view
            customViewCallback = callback
            binding.fullscreen.addView(view)
            binding.fullscreen.visibility = View.VISIBLE
            supportActionBar?.hide()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        override fun onHideCustomView() = hideCustomView()
    }

    private fun hideCustomView() {
        val view = customView ?: return
        binding.fullscreen.removeView(view)
        binding.fullscreen.visibility = View.GONE
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        supportActionBar?.show()
        WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.webview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_refresh -> reload()
            R.id.action_open_browser -> Urls.openExternally(this, currentUrl())
            R.id.action_share -> startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, currentUrl()),
                    getString(R.string.share),
                )
            )
            R.id.action_copy -> {
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("URL", currentUrl()))
                Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.web.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        binding.web.onResume()
    }

    override fun onPause() {
        binding.web.onPause()
        CookieManager.getInstance().flush()
        super.onPause()
    }

    override fun onDestroy() {
        binding.web.apply {
            stopLoading()
            (parent as? android.view.ViewGroup)?.removeView(this)
            destroy()
        }
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ICON = "icon"
    }
}
