package com.gptwork.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
    private var homepage = "https://www.google.com"
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
        homepage = prefs.getString("homepage", "https://www.google.com") ?: "https://www.google.com"
        dnsProfile = prefs.getString("dns", "") ?: ""
        blockPopups = prefs.getBoolean("popups", true)
        desktopMode = prefs.getBoolean("desktop", false)
    }

    private fun savePrefs() = prefs.edit().putString("provider", provider).putBoolean("dark", dark)
        .putString("homepage", homepage).putString("dns", dnsProfile).putBoolean("popups", blockPopups)
        .putBoolean("desktop", desktopMode).apply()

    private fun button(label: String, action: () -> Unit, size: Float = 18f): TextView = TextView(this).apply {
        text = label; textSize = size; gravity = Gravity.CENTER; setTextColor(if (dark) Color.WHITE else Color.DKGRAY)
        setPadding(18, 14, 18, 14); minWidth = 58; minHeight = 58
        setOnClickListener { action() }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(if (dark) Color.rgb(16,17,20) else Color.WHITE) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(6, 6, 6, 2) }
        top.addView(button("‹", { if (web.canGoBack()) web.goBack() }))
        top.addView(button("›", { if (web.canGoForward()) web.goForward() }))
        address = EditText(this).apply {
            hint = "Search or enter address"; setSingleLine(true); imeOptions = 5; textSize = 16f
            setTextColor(if (dark) Color.WHITE else Color.DKGRAY); setHintTextColor(Color.GRAY)
            setBackgroundColor(if (dark) Color.rgb(30,32,38) else Color.rgb(238,240,244)); setPadding(18, 0, 18, 0)
            setOnEditorActionListener { _, _, _ -> navigateInput(); true }
        }
        top.addView(address, LinearLayout.LayoutParams(0, 56, 1f))
        top.addView(button("↻", { web.reload() }))
        tabsButton = button("1", { showTabs() }, 16f); top.addView(tabsButton)
        root.addView(top)

        titleText = TextView(this).apply { text = "GPT-WORK Browser v2"; textSize = 12f; setPadding(12, 0, 12, 2); setTextColor(Color.GRAY) }
        root.addView(titleText)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0; visibility = View.GONE }
        root.addView(progress, LinearLayout.LayoutParams(-1, 4))

        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(2, 2, 2, 4) }
        nav.addView(button("⌂", { web.loadUrl(homepage) }))
        val newTab = button("＋", { newTab(homepage) }, 24f).apply { minWidth = 82; minHeight = 72; setPadding(24, 16, 24, 16) }
        nav.addView(newTab)
        nav.addView(button("▣", { showTabs() }))
        nav.addView(button("★", { bookmark() }))
        nav.addView(button("☰", { showMainMenu(this) }))
        root.addView(nav)
        setContentView(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.loadsImagesAutomatically = true
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.setSupportZoom(true)
        s.builtInZoomControls = false
        s.displayZoomControls = false
        s.mediaPlaybackRequiresUserGesture = true
        s.allowFileAccess = false
        s.allowContentAccess = false
        if (Build.VERSION.SDK_INT >= 26) s.safeBrowsingEnabled = true
        if (Build.VERSION.SDK_INT >= 23) s.offscreenPreRaster = true
        applyUserAgent()
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme ?: return false
                return scheme != "http" && scheme != "https" && scheme != "javascript"
            }
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                progress.visibility = View.VISIBLE; progress.progress = 5; address.setText(url); titleText.text = "Carregando… $url"
            }
            override fun onPageFinished(view: WebView, url: String) {
                address.setText(url); titleText.text = view.title ?: "GPT-WORK Browser"; progress.progress = 100
                progress.postDelayed({ progress.visibility = View.GONE }, 180)
                if (pages.isNotEmpty()) pages[pages.lastIndex] = url
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
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
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36 GPT-WORK/2.0"
        } else {
            WebSettings.getDefaultUserAgent(this).replace(" GPT-WORK/1.0", "") + " GPT-WORK/2.0"
        }
    }

    private fun navigateInput() {
        val input = address.text.toString().trim(); if (input.isBlank()) return
        val url = if (input.startsWith("http://") || input.startsWith("https://")) input
        else if (input.contains(".") && !input.contains(" ")) "https://$input"
        else lua.searchUrl(provider, input)
        web.loadUrl(url)
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

    private fun showMainMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Abas / Nova aba").setOnMenuItemClickListener { showTabs(); true }
            menu.add("Favoritos").setOnMenuItemClickListener { showBookmarks(); true }
            menu.add("Bookmarklets").setOnMenuItemClickListener { showBookmarklets(); true }
            menu.add("DevTools nativo").setOnMenuItemClickListener { showDevTools(); true }
            menu.add("Configurações").setOnMenuItemClickListener { showSettings(); true }
            menu.add("Buscar: ${if (provider == "google") "Google" else "DuckDuckGo"}").setOnMenuItemClickListener { provider = if (provider == "google") "duckduckgo" else "google"; savePrefs(); true }
            menu.add("Limpar cache e histórico").setOnMenuItemClickListener { web.clearHistory(); web.clearCache(true); CookieManager.getInstance().removeAllCookies(null); true }
            show()
        }
    }

    private fun showBookmarks() {
        val set = prefs.getStringSet("bookmarks", emptySet())!!.toList()
        val names = set.map { it.substringBefore('\t') }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Favoritos").setItems(names) { _, i -> web.loadUrl(set[i].substringAfter('\t')) }.setNegativeButton("Fechar", null).show()
    }

    private fun showBookmarklets() {
        val set = prefs.getStringSet("bookmarklets", emptySet())!!.toList()
        val names = set.map { it.substringBefore('\t') }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Gerenciador de Bookmarklets").setItems(names) { _, i ->
            val code = set[i].substringAfter('\t'); web.evaluateJavascript("(function(){ $code })();", null)
        }.setPositiveButton("+ Adicionar") { _, _ -> addBookmarklet() }.setNeutralButton("Gerenciar", null).setNegativeButton("Fechar", null).show()
    }

    private fun addBookmarklet() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 8, 28, 0) }
        val name = EditText(this).apply { hint = "Nome" }; val code = EditText(this).apply { hint = "JavaScript do bookmarklet"; minLines = 5; gravity = Gravity.TOP }
        box.addView(name); box.addView(code)
        AlertDialog.Builder(this).setTitle("Novo Bookmarklet").setView(box).setPositiveButton("Salvar") { _, _ ->
            if (name.text.isNotBlank() && code.text.isNotBlank()) {
                val set = prefs.getStringSet("bookmarklets", emptySet())!!.toMutableSet(); set.add("${name.text}\t${code.text}"); prefs.edit().putStringSet("bookmarklets", set).apply()
            }
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun showDevTools() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 0) }
        val output = TextView(this).apply { setTextColor(if (dark) Color.WHITE else Color.DKGRAY); textSize = 12f; setPadding(8,8,8,8); text = "WebView DevTools\nURL: ${web.url}\nUA: ${web.settings.userAgentString}\n\nConsole:\n${console.takeLast(40).joinToString("\n")}" }
        val scroll = ScrollView(this); scroll.addView(output); box.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply { text = "JS"; setOnClickListener { evalJsDialog(output) } })
        actions.addView(Button(this).apply { text = "HTML"; setOnClickListener { web.evaluateJavascript("document.documentElement.outerHTML", { v -> output.text = v }) } })
        actions.addView(Button(this).apply { text = "Console"; setOnClickListener { output.text = console.joinToString("\n") } })
        actions.addView(Button(this).apply { text = "Info"; setOnClickListener { output.text = "URL: ${web.url}\nTitle: ${web.title}\nUA: ${web.settings.userAgentString}\nCache: ${web.settings.cacheMode}\nJS: ${web.settings.javaScriptEnabled}\nDNS profile: ${if (dnsProfile.isBlank()) "system" else dnsProfile}" } })
        box.addView(actions)
        AlertDialog.Builder(this).setTitle("DevTools nativo").setView(box).setPositiveButton("Fechar", null).show()
    }

    private fun evalJsDialog(output: TextView) {
        val input = EditText(this).apply { hint = "document.title"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("Executar JavaScript").setView(input).setPositiveButton("Executar") { _, _ -> web.evaluateJavascript(input.text.toString()) { value -> output.text = value } }.setNegativeButton("Cancelar", null).show()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 4, 24, 0) }
        val home = EditText(this).apply { hint = "Homepage"; setSingleLine(true); setText(homepage) }
        val dns = EditText(this).apply { hint = "DNS personalizado / DoH URL (ex.: https://dns.google/dns-query)"; setSingleLine(true); setText(dnsProfile) }
        val search = TextView(this).apply { text = "\nBuscador: ${if (provider == "google") "Google" else "DuckDuckGo"}\n"; textSize = 15f }
        val desktop = Switch(this).apply { text = "Modo desktop"; isChecked = desktopMode }
        val popups = Switch(this).apply { text = "Bloquear pop-ups"; isChecked = blockPopups }
        val js = Switch(this).apply { text = "JavaScript"; isChecked = web.settings.javaScriptEnabled }
        val safe = Switch(this).apply { text = "Safe Browsing"; isChecked = Build.VERSION.SDK_INT < 26 || web.settings.safeBrowsingEnabled }
        val note = TextView(this).apply { text = "\nDNS: o perfil é salvo para a camada de rede futura. O WebView usa o resolvedor da pilha de rede do Android; para DNS realmente independente por app será necessário uma camada de rede/VPN/stack HTTP própria.\n"; setTextColor(Color.GRAY); textSize = 12f }
        box.addView(home); box.addView(dns); box.addView(search); box.addView(desktop); box.addView(popups); box.addView(js); box.addView(safe); box.addView(note)
        AlertDialog.Builder(this).setTitle("Configurações").setView(box).setPositiveButton("Salvar") { _, _ ->
            homepage = home.text.toString().ifBlank { "https://www.google.com" }; dnsProfile = dns.text.toString(); desktopMode = desktop.isChecked; blockPopups = popups.isChecked
            web.settings.javaScriptEnabled = js.isChecked; if (Build.VERSION.SDK_INT >= 26) web.settings.safeBrowsingEnabled = safe.isChecked; applyUserAgent(); savePrefs()
        }.setNeutralButton("Testar DNS") { _, _ -> testDns(dns.text.toString()) }.setNegativeButton("Cancelar", null).show()
    }

    private fun testDns(endpoint: String) {
        if (endpoint.isBlank()) { Toast.makeText(this, "Informe um endpoint DNS/DoH", Toast.LENGTH_SHORT).show(); return }
        executor.execute {
            val ok = try { val c = URL(endpoint).openConnection() as HttpURLConnection; c.connectTimeout = 2500; c.readTimeout = 2500; c.requestMethod = "GET"; c.responseCode in 200..499 } catch (_: Exception) { false }
            runOnUiThread { Toast.makeText(this, if (ok) "Endpoint DNS respondeu" else "Não foi possível alcançar o endpoint", Toast.LENGTH_LONG).show() }
        }
    }

    override fun onDestroy() { executor.shutdownNow(); web.destroy(); super.onDestroy() }
    @Deprecated("Compatibility") override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
