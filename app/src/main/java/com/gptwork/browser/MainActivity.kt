package com.gptwork.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.view.Gravity
import android.view.View
import android.net.http.SslError
import android.webkit.*
import android.webkit.SslErrorHandler
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var tabsButton: TextView
    private lateinit var progress: ProgressBar
    private lateinit var titleText: TextView
    private val lua = LuaEngine()
    private val prefs by lazy { getSharedPreferences("gptwork", MODE_PRIVATE) }
    private val pages = mutableListOf<String>()
    private val console = mutableListOf<String>()
    private val executor = Executors.newSingleThreadExecutor()
    private var provider = "google"
    private var dark = true
    private var homepage = "file:///android_asset/home.html"
    private var dnsProfile = ""
    private var blockPopups = true
    private var desktopMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPrefs()
        buildUi()
        configureWebView()
        newTab(homepage)
    }

    private fun loadPrefs() {
        provider = prefs.getString("provider", "google") ?: "google"
        dark = prefs.getBoolean("dark", true)
        homepage = prefs.getString("homepage", "file:///android_asset/home.html") ?: "file:///android_asset/home.html"
        if (homepage == "https://www.google.com") homepage = "file:///android_asset/home.html"
        dnsProfile = prefs.getString("dns", "") ?: ""
        blockPopups = prefs.getBoolean("popups", true)
        desktopMode = prefs.getBoolean("desktop", false)
    }

    private fun savePrefs() = prefs.edit().putString("provider", provider).putBoolean("dark", dark)
        .putString("homepage", homepage).putString("dns", dnsProfile).putBoolean("popups", blockPopups)
        .putBoolean("desktop", desktopMode).apply()

    private fun iconButton(iconRes: Int, content: String, action: () -> Unit): ImageView = ImageView(this).apply {
        setImageResource(iconRes); contentDescription = content
        setColorFilter(if (dark) Color.WHITE else Color.DKGRAY)
        setPadding(18,18,18,18); minimumWidth = 56; minimumHeight = 56
        isClickable = true; isFocusable = true
        background = android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.rgb(124,156,255)), null, null)
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(56,56)
    }
    private fun textButton(label: String, action: () -> Unit, size: Float = 16f): TextView = TextView(this).apply {
        text = label; textSize = size; gravity = Gravity.CENTER; setTextColor(if (dark) Color.WHITE else Color.DKGRAY)
        setPadding(14,12,14,12); minWidth = 52; minHeight = 52; setOnClickListener { action() }
    }

    private fun buildUi() {
        val bg = if (dark) Color.rgb(15,17,20) else Color.WHITE
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        // Top bar
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(6,6,6,2); setBackgroundColor(bg) }
        top.addView(iconButton(R.drawable.ic_back, "Voltar") { if (web.canGoBack()) web.goBack() })
        top.addView(iconButton(R.drawable.ic_forward, "Avançar") { if (web.canGoForward()) web.goForward() })
        address = EditText(this).apply {
            hint = "Pesquisar ou digitar endereço"; setSingleLine(true); imeOptions = 5; textSize = 15f
            setTextColor(if (dark) Color.WHITE else Color.DKGRAY); setHintTextColor(Color.GRAY)
            setBackgroundColor(if (dark) Color.rgb(30,32,38) else Color.rgb(238,240,244))
            setPadding(18,0,18,0); background = android.graphics.drawable.GradientDrawable().apply { setColor(if(dark) Color.rgb(30,32,38) else Color.rgb(238,240,244)); cornerRadius = 24f }
            setOnEditorActionListener { _,_,_-> navigateInput(); true }
        }
        top.addView(address, LinearLayout.LayoutParams(0,56,1f).apply { setMargins(6,0,6,0) })
        top.addView(iconButton(R.drawable.ic_refresh, "Recarregar") { web.reload() })
        tabsButton = textButton("1", { showTabs() }, 14f).apply { setBackgroundColor(if(dark) Color.rgb(30,32,38) else Color.rgb(238,240,244)); background = android.graphics.drawable.GradientDrawable().apply { setColor(if(dark) Color.rgb(30,32,38) else Color.rgb(238,240,244)); cornerRadius = 14f } }
        top.addView(tabsButton, LinearLayout.LayoutParams(52,52).apply { setMargins(4,0,0,0) })
        root.addView(top)

        titleText = TextView(this).apply { text = "GPT-WORK • Turbo"; textSize = 11f; setPadding(12,2,12,2); setTextColor(Color.rgb(122,133,145)) }
        root.addView(titleText)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0; visibility = View.GONE; progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(124,156,255)) }
        root.addView(progress, LinearLayout.LayoutParams(-1,3))

        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1,0,1f))

        // Bottom nav with SVG icons
        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(8,6,8,8); setBackgroundColor(bg) }
        nav.addView(iconButton(R.drawable.ic_home, "Início") { web.loadUrl(homepage) })
        nav.addView(Space(this).apply { minimumWidth = 8 })
        val addTab = iconButton(R.drawable.ic_add, "Nova aba") { newTab(homepage) }.apply { setBackgroundColor(Color.rgb(124,156,255)); setColorFilter(Color.rgb(15,17,20)); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(124,156,255)); cornerRadius = 28f; setStroke(0,0) }; layoutParams = LinearLayout.LayoutParams(72,72).apply{ setMargins(8,0,8,0)} }
        nav.addView(addTab)
        nav.addView(Space(this).apply { minimumWidth = 8 })
        nav.addView(iconButton(R.drawable.ic_tabs, "Abas") { showTabs() })
        nav.addView(iconButton(R.drawable.ic_bookmark, "Favoritos") { bookmark() })
        nav.addView(iconButton(R.drawable.ic_settings, "Configurações") { openSettings() })
        root.addView(nav)
        setContentView(root)
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        val cm = CookieManager.getInstance()
        val cookiesEnabled = prefs.getBoolean("cookies", true)
        val domEnabled = prefs.getBoolean("domStorage", true)
        val indexDbEnabled = prefs.getBoolean("indexdb", true)
        val safeBrowsing = prefs.getBoolean("safeBrowsing", false)
        val httpsOnly = prefs.getBoolean("httpsOnly", false)
        val blockMixed = prefs.getBoolean("blockMixed", true)
        val dnt = prefs.getBoolean("dnt", true)
        val redirect = prefs.getBoolean("redirect", true)
        val oauth = prefs.getBoolean("oauth", true)

        cm.setAcceptCookie(cookiesEnabled)
        try { cm.setAcceptThirdPartyCookies(web, cookiesEnabled) } catch (_: Exception) {}
        val s = web.settings
        s.javaScriptEnabled = prefs.getBoolean("js", true)
        s.domStorageEnabled = domEnabled
        s.databaseEnabled = indexDbEnabled
        // IndexDB requires databaseEnabled + domStorageEnabled + allowFileAccess for blob
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.allowFileAccessFromFileURLs = false
        s.allowUniversalAccessFromFileURLs = false
        s.loadsImagesAutomatically = true
        s.blockNetworkImage = false
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.mixedContentMode = if (blockMixed) WebSettings.MIXED_CONTENT_NEVER_ALLOW else WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.setSupportZoom(true)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = false
        s.setGeolocationEnabled(false)
        s.saveFormData = false
        s.javaScriptCanOpenWindowsAutomatically = oauth && !blockPopups
        s.setSupportMultipleWindows(oauth)
        if (Build.VERSION.SDK_INT >= 26) s.safeBrowsingEnabled = safeBrowsing
        if (Build.VERSION.SDK_INT >= 23) s.offscreenPreRaster = true
        try { if (Build.VERSION.SDK_INT >= 26) web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true) } catch (_: Exception) {}
        applyUserAgent()
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // DNT header via custom header injection
        if (dnt) {
            // will add in shouldIntercept via header
        }

        executor.execute {
            try {
                val c = URL(if (homepage.startsWith("file")) "https://www.google.com" else homepage).openConnection() as HttpURLConnection
                c.requestMethod = "HEAD"; c.connectTimeout = 1500; c.readTimeout = 1500; c.connect(); c.disconnect()
            } catch (_: Exception) {}
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest?): WebResourceResponse? {
                // Privacidade: add DNT
                return super.shouldInterceptRequest(view, request)
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                val scheme = request.url.scheme ?: return false
                // OAuth domains - always allow
                val oauthDomains = listOf("accounts.google.com","github.com","login.microsoftonline.com","appleid.apple.com","auth0.com","okta.com")
                val isOAuth = oauth && oauthDomains.any { url.contains(it) }
                if (isOAuth) return false
                // Redirect handling
                if (!redirect && request.isRedirect) return true
                // HTTPS only
                if (httpsOnly && scheme == "http") {
                    view.loadUrl(url.replace("http://","https://"))
                    return true
                }
                // Allow http/https/javascript only, block weird schemes unless oauth
                if (scheme == "http" || scheme == "https" || scheme == "javascript" || scheme == "file" || scheme == "data") return false
                // External intent for others (tel, mailto, intent)
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                } catch (_: Exception) { true }
            }
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                progress.visibility = View.VISIBLE; progress.progress = 5; address.setText(url); titleText.text = "Carregando…"
            }
            override fun onPageFinished(view: WebView, url: String) {
                address.setText(url); titleText.text = view.title?.take(40) ?: "GPT-WORK"; progress.progress = 100
                progress.postDelayed({ progress.visibility = View.GONE }, 180)
                if (pages.isNotEmpty()) pages[pages.lastIndex] = url
                // Inject DNT JS
                if (prefs.getBoolean("dnt", true)) view.evaluateJavascript("try{navigator.doNotTrack='1';window.doNotTrack='1'}catch(e){}",null)
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                AlertDialog.Builder(this@MainActivity).setTitle("⚠️ SSL").setMessage("Certificado inválido. Continuar?").setPositiveButton("Continuar"){_,_-> handler.proceed() }.setNegativeButton("Cancelar"){_,_-> handler.cancel() }.show()
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) titleText.text = "Erro: ${error.description}"
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE; progress.progress = newProgress }
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                console.add("${message.messageLevel()}: ${message.message()} @ ${message.lineNumber()}")
                if (console.size > 200) console.removeAt(0)
                return true
            }
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                if (!oauth) return false
                val newWeb = WebView(this@MainActivity).apply { settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.databaseEnabled = true }
                val d = AlertDialog.Builder(this@MainActivity).setView(newWeb).setPositiveButton("Fechar",null).create()
                d.show()
                newWeb.webChromeClient = this
                newWeb.webViewClient = WebViewClient()
                (resultMsg.obj as WebView.WebViewTransport).webView = newWeb
                resultMsg.sendToTarget()
                return true
            }
            override fun onPermissionRequest(request: PermissionRequest) {
                // Privacidade: nega por padrão, exceto se necessário
                request.deny()
            }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, _, mimeType, _ ->
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType); addRequestHeader("User-Agent", userAgent)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).lastPathSegment ?: "download")
            }
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            Toast.makeText(this, "Download iniciado", Toast.LENGTH_SHORT).show()
        })
    }

    private fun applyUserAgent() {
        val s = web.settings
        s.userAgentString = if (desktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36 GPT-WORK/2.1"
        } else {
            try { WebSettings.getDefaultUserAgent(this).replace(" GPT-WORK/1.0","") + " GPT-WORK/2.1" } catch (_:Exception) { "Mozilla/5.0 GPT-WORK/2.1" }
        }
    }

    private fun navigateInput() {
        val input = address.text.toString().trim(); if (input.isBlank()) return
        val url = if (input.startsWith("http://") || input.startsWith("https://")) input
        else if (input.startsWith("file://")) input
        else if (input.contains(".") && !input.contains(" ")) "https://$input"
        else lua.searchUrl(provider, input)
        // HTTPS only redirect
        val finalUrl = if (prefs.getBoolean("httpsOnly", false) && url.startsWith("http://")) url.replace("http://","https://") else url
        web.loadUrl(finalUrl)
    }

    private fun newTab(url: String) { pages.add(url); tabsButton.text = pages.size.toString(); web.loadUrl(url) }

    private fun showTabs() {
        val items = pages.mapIndexed { i, u -> "${i + 1}. ${u.take(70)}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Abas (${pages.size})").setItems(items) { _, which ->
            if (which in pages.indices) { web.loadUrl(pages[which]); tabsButton.text = (which + 1).toString() }
        }.setPositiveButton("+ Nova aba") { _, _ -> newTab(homepage) }.setNegativeButton("Fechar") { _, _ -> }.show()
    }

    private fun bookmark() {
        val url = web.url ?: return
        val name = web.title ?: url
        val set = prefs.getStringSet("bookmarks", emptySet())!!.toMutableSet(); set.add("$name\t$url")
        prefs.edit().putStringSet("bookmarks", set).apply(); Toast.makeText(this, "Favorito salvo", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() { super.onResume(); loadPrefs(); applyUserAgent(); web.settings.javaScriptEnabled = prefs.getBoolean("js", true) }
    override fun onDestroy() { executor.shutdownNow(); web.destroy(); super.onDestroy() }
    @Deprecated("Compatibility") override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
