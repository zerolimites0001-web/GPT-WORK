package com.gptwork.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.io.File
import java.net.URL
import java.util.concurrent.Executors

data class GTab(val webView: WebView, var url: String, var title: String)
object WebHolder {
    val tabs = mutableListOf<GTab>()
    var currentIndex = -1
}
class MainActivity : AppCompatActivity() {
    private lateinit var webContainer: FrameLayout
    private lateinit var address: EditText
    private lateinit var tabsButton: TextView
    private lateinit var progress: ProgressBar
    private lateinit var titleText: TextView
    private val lua = LuaEngine()
    private val prefs by lazy { getSharedPreferences("gptwork", MODE_PRIVATE) }
    private val tabs get() = WebHolder.tabs
    private var currentIndex
        get() = WebHolder.currentIndex
        set(v) { WebHolder.currentIndex = v }
    private val console = mutableListOf<String>()
    private val executor = Executors.newSingleThreadExecutor()
    private var provider = "google"
    private var dark = true
    private var homepage = "file:///android_asset/home.html"
    private var dnsProfile = ""
    private var blockPopups = true
    private var desktopMode = false

    private val currentWeb: WebView?
        get() = if (currentIndex in tabs.indices) tabs[currentIndex].webView else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPrefs()
        buildUi()
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

    private fun startBgService() {
        try {
            val i = Intent(this, BackgroundAudioService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        } catch (_: Exception) {}
    }
    private fun stopBgService() {
        try { stopService(Intent(this, BackgroundAudioService::class.java)) } catch (_: Exception) {}
    }

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
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(6,6,6,2); setBackgroundColor(bg) }
        top.addView(iconButton(R.drawable.ic_back, "Voltar") { currentWeb?.let { if (it.canGoBack()) it.goBack() } })
        top.addView(iconButton(R.drawable.ic_forward, "Avançar") { currentWeb?.let { if (it.canGoForward()) it.goForward() } })
        address = EditText(this).apply {
            hint = "Pesquisar ou digitar endereço"; setSingleLine(true); imeOptions = 5; textSize = 15f
            setTextColor(if (dark) Color.WHITE else Color.DKGRAY); setHintTextColor(Color.GRAY)
            setBackgroundColor(if (dark) Color.rgb(30,32,38) else Color.rgb(238,240,244))
            setPadding(18,0,18,0); background = android.graphics.drawable.GradientDrawable().apply { setColor(if(dark) Color.rgb(30,32,38) else Color.rgb(238,240,244)); cornerRadius = 24f }
            setOnEditorActionListener { _,_,_-> navigateInput(); true }
        }
        top.addView(address, LinearLayout.LayoutParams(0,56,1f).apply { setMargins(6,0,6,0) })
        top.addView(iconButton(R.drawable.ic_refresh, "Recarregar") { currentWeb?.reload() })
        tabsButton = textButton("0", { showTabs() }, 14f).apply { setBackgroundColor(if(dark) Color.rgb(30,32,38) else Color.rgb(238,240,244)); background = android.graphics.drawable.GradientDrawable().apply { setColor(if(dark) Color.rgb(30,32,38) else Color.rgb(238,240,244)); cornerRadius = 14f } }
        top.addView(tabsButton, LinearLayout.LayoutParams(52,52).apply { setMargins(4,0,0,0) })
        root.addView(top)

        titleText = TextView(this).apply { text = "GPT-WORK • Turbo"; textSize = 11f; setPadding(12,2,12,2); setTextColor(Color.rgb(122,133,145)) }
        root.addView(titleText)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0; visibility = View.GONE; progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(124,156,255)) }
        root.addView(progress, LinearLayout.LayoutParams(-1,3))

        webContainer = FrameLayout(this)
        root.addView(webContainer, LinearLayout.LayoutParams(-1,0,1f))

        val nav = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(8,6,8,8); setBackgroundColor(bg) }
        nav.addView(iconButton(R.drawable.ic_home, "Início") { currentWeb?.loadUrl(homepage) })
        nav.addView(Space(this).apply { minimumWidth = 8 })
        val addTab = iconButton(R.drawable.ic_add, "Nova aba") { newTab(homepage) }.apply { setBackgroundColor(Color.rgb(124,156,255)); setColorFilter(Color.rgb(15,17,20)); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(124,156,255)); cornerRadius = 28f; setStroke(0,0) }; layoutParams = LinearLayout.LayoutParams(72,72).apply{ setMargins(8,0,8,0)} }
        nav.addView(addTab)
        nav.addView(Space(this).apply { minimumWidth = 8 })
        nav.addView(iconButton(R.drawable.ic_tabs, "Abas") { showTabs() })
        nav.addView(iconButton(R.drawable.ic_bookmark, "Favoritos") { bookmark() })
        nav.addView(iconButton(R.drawable.ic_download, "Downloads") { showDownloads() })
        nav.addView(iconButton(R.drawable.ic_settings, "Configurações") { openSettings() })
        root.addView(nav)
        setContentView(root)
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val w = WebView(this)
        val cm = CookieManager.getInstance()
        val cookiesEnabled = prefs.getBoolean("cookies", true)
        val domEnabled = prefs.getBoolean("domStorage", true)
        val indexDbEnabled = prefs.getBoolean("indexdb", true)
        val safeBrowsing = prefs.getBoolean("safeBrowsing", false)
        val httpsOnly = prefs.getBoolean("httpsOnly", false)
        val blockMixed = prefs.getBoolean("blockMixed", true)
        val oauth = prefs.getBoolean("oauth", true)

        cm.setAcceptCookie(cookiesEnabled)
        try { cm.setAcceptThirdPartyCookies(w, cookiesEnabled) } catch (_: Exception) {}
        val s = w.settings
        s.javaScriptEnabled = prefs.getBoolean("js", true)
        s.domStorageEnabled = domEnabled
        s.databaseEnabled = indexDbEnabled
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.allowFileAccessFromFileURLs = false
        s.allowUniversalAccessFromFileURLs = false
        // Media fast load
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
        // Fast media: enable hardware, cache, prefetch
        s.setEnableSmoothTransition(true)
        if (Build.VERSION.SDK_INT >= 26) s.safeBrowsingEnabled = safeBrowsing
        if (Build.VERSION.SDK_INT >= 23) s.offscreenPreRaster = true
        try { if (Build.VERSION.SDK_INT >= 26) w.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true) } catch (_: Exception) {}
        s.userAgentString = if (desktopMode) "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36 GPT-WORK/2.1" else try { WebSettings.getDefaultUserAgent(this).replace(" GPT-WORK/1.0","") + " GPT-WORK/2.1" } catch (_:Exception) { "Mozilla/5.0 GPT-WORK/2.1" }
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        w.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        // Keep alive in background for music/video
        w.setBackgroundColor(Color.TRANSPARENT)

        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                val scheme = request.url.scheme ?: return false
                val oauthDomains = listOf("accounts.google.com","github.com","login.microsoftonline.com","appleid.apple.com","auth0.com","okta.com")
                val isOAuth = oauth && oauthDomains.any { url.contains(it) }
                if (isOAuth) return false
                if (!prefs.getBoolean("redirect",true) && request.isRedirect) return true
                if (prefs.getBoolean("httpsOnly", false) && scheme == "http") {
                    view.loadUrl(url.replace("http://","https://"))
                    return true
                }
                if (scheme == "http" || scheme == "https" || scheme == "javascript" || scheme == "file" || scheme == "data") return false
                return try { startActivity(Intent(Intent.ACTION_VIEW, request.url)); true } catch (_: Exception) { true }
            }
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                if (view == currentWeb) { progress.visibility = View.VISIBLE; progress.progress = 5; address.setText(url); titleText.text = "Carregando…" }
                val idx = tabs.indexOfFirst { it.webView == view }
                if (idx != -1) tabs[idx].url = url
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (view == currentWeb) { address.setText(url); titleText.text = view.title?.take(40) ?: "GPT-WORK"; progress.progress = 100; progress.postDelayed({ progress.visibility = View.GONE }, 180) }
                val idx = tabs.indexOfFirst { it.webView == view }
                if (idx != -1) { tabs[idx].url = url; tabs[idx].title = view.title ?: url }
                if (prefs.getBoolean("dnt", true)) view.evaluateJavascript("try{navigator.doNotTrack='1';window.doNotTrack='1'}catch(e){}",null)
            }
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                AlertDialog.Builder(this@MainActivity).setTitle("⚠️ SSL").setMessage("Certificado inválido. Continuar?").setPositiveButton("Continuar"){_,_-> handler.proceed() }.setNegativeButton("Cancelar"){_,_-> handler.cancel() }.show()
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && view == currentWeb) titleText.text = "Erro: ${error.description}"
            }
        }
        w.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { if (view == currentWeb) { progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE; progress.progress = newProgress } }
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
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                // Fullscreen video - imporante p/ não pausar
                super.onShowCustomView(view, callback)
            }
            override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        }
        w.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            // Pede confirmação - arquivo pode ser nocivo
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType) ?: Uri.parse(url).lastPathSegment ?: "download"
            AlertDialog.Builder(this).setTitle("⬇️ Download")
                .setMessage("Deseja continuar?\n\nArquivo: $fileName\nTipo: $mimeType\n\nO arquivo pode ser nocivo. Só baixe se confia na origem.")
                .setPositiveButton("Baixar") { _, _ ->
                    // Só pede permissão se realmente necessário (Android <13 e não concedida) - não idiota
                    // 1) Salva via DownloadManager em /sdcard/Download
                    try {
                        val req = DownloadManager.Request(Uri.parse(url)).apply {
                            setMimeType(mimeType); addRequestHeader("User-Agent", userAgent)
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            setTitle(fileName); setDescription("Baixando via GPT-WORK")
                        }
                        (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                    } catch(e:Exception){ Toast.makeText(this, "Erro DownloadManager: ${e.message}", Toast.LENGTH_SHORT).show() }
                    // 2) Salva também no data do app (cache/files) em background
                    executor.execute {
                        try {
                            val conn = URL(url).openConnection() as HttpURLConnection
                            conn.setRequestProperty("User-Agent", userAgent)
                            conn.connect()
                            if (conn.responseCode in 200..299) {
                                val dir = File(filesDir, "downloads"); dir.mkdirs()
                                val outFile = File(dir, fileName)
                                conn.inputStream.use { input -> outFile.outputStream().use { output -> input.copyTo(output) } }
                                runOnUiThread { Toast.makeText(this, "Salvo também em app: ${outFile.name}", Toast.LENGTH_SHORT).show() }
                            }
                            conn.disconnect()
                        } catch(_:Exception){}
                    }
                    Toast.makeText(this, "Download iniciado: $fileName", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Ver Downloads") { _, _ -> showDownloads() }
                .show()
        })
        // Don't pause when hidden - keep music/video playing
        return w
    }

    private fun switchTo(index: Int) {
        if (index !in tabs.indices) return
        // hide all
        for (t in tabs) t.webView.visibility = View.GONE
        currentIndex = index
        val tab = tabs[index]
        tab.webView.visibility = View.VISIBLE
        // ensure container has it
        if (tab.webView.parent == null) webContainer.addView(tab.webView, ViewGroup.LayoutParams(-1,-1))
        address.setText(tab.url)
        titleText.text = tab.title
        tabsButton.text = "${index+1}/${tabs.size}"
        // resume
        tab.webView.onResume()
        tab.webView.resumeTimers()
    }

    private fun newTab(url: String) {
        val w = createWebView()
        w.visibility = View.GONE
        webContainer.addView(w, ViewGroup.LayoutParams(-1,-1))
        val tab = GTab(w, url, url)
        tabs.add(tab)
        // pause previous
         // mantem aba anterior rodando p/ audio em background
        switchTo(tabs.size-1)
        w.loadUrl(url)
        updateTabsButton()
    }

    private fun updateTabsButton() {
        tabsButton.text = if (tabs.isEmpty()) "0" else "${currentIndex+1}/${tabs.size}"
    }

    private fun navigateInput() {
        val w = currentWeb ?: return
        val input = address.text.toString().trim(); if (input.isBlank()) return
        val url = if (input.startsWith("http://") || input.startsWith("https://")) input
        else if (input.startsWith("file://")) input
        else if (input.contains(".") && !input.contains(" ")) "https://$input"
        else lua.searchUrl(provider, input)
        val finalUrl = if (prefs.getBoolean("httpsOnly", false) && url.startsWith("http://")) url.replace("http://","https://") else url
        w.loadUrl(finalUrl)
    }

    private fun showTabs() {
        val items = tabs.mapIndexed { i, t -> "${if(i==currentIndex) "● " else ""}${i+1}. ${(t.title.take(30)).ifBlank { t.url.take(50)} }" }.toTypedArray()
        if (items.isEmpty()) { newTab(homepage); return }
        AlertDialog.Builder(this)
            .setTitle("Abas (${tabs.size}) - toque para alternar")
            .setItems(items) { _, which -> switchTo(which) }
            .setPositiveButton("+ Nova aba") { _, _ -> newTab(homepage) }
            .setNeutralButton("✕ Fechar aba") { _, _ ->
                if (currentIndex != -1) {
                    val idx = currentIndex
                    val tab = tabs[idx]
                    webContainer.removeView(tab.webView)
                    tab.webView.destroy()
                    tabs.removeAt(idx)
                    if (tabs.isEmpty()) newTab(homepage) else switchTo((idx-1).coerceAtLeast(0))
                    updateTabsButton()
                }
            }
            .setNegativeButton("Fechar", null).show()
    }

    private fun showDownloads() {
        val dmDir = File(filesDir, "downloads")
        val appFiles = if (dmDir.exists()) dmDir.listFiles()?.map { "app: " + it.name + " (" + (it.length()/1024) + "KB)" } ?: emptyList() else emptyList()
        val sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val sdFiles = if (sdDir.exists()) sdDir.listFiles()?.take(20)?.map { "sdcard: " + it.name } ?: emptyList() else emptyList()
        val all = (appFiles + sdFiles).toTypedArray().ifEmpty { arrayOf("Nenhum download ainda") }
        AlertDialog.Builder(this).setTitle("📁 Downloads")
            .setItems(all) { _, i ->
                val name = all[i]
                Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
                // Abre pasta no sistema
                try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()), "*/*")) } catch(_:Exception){
                    // fallback abre gerenciador
                    try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch(_:Exception){}
                }
            }
            .setPositiveButton("Abrir pasta Download") { _, _ ->
                try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch(_:Exception){
                    Toast.makeText(this, sdDir.absolutePath, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Fechar", null)
            .setNeutralButton("Limpar app") { _, _ -> try { dmDir.listFiles()?.forEach { it.delete() }; Toast.makeText(this,"Downloads do app limpos",Toast.LENGTH_SHORT).show() } catch(_:Exception){} }
            .show()
    }

    private fun bookmark() {
        val url = currentWeb?.url ?: return
        val name = currentWeb?.title ?: url
        val set = prefs.getStringSet("bookmarks", emptySet())!!.toMutableSet(); set.add("$name\t$url")
        prefs.edit().putStringSet("bookmarks", set).apply(); Toast.makeText(this, "Favorito salvo", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadPrefs()
        currentWeb?.settings?.javaScriptEnabled = prefs.getBoolean("js", true)
        currentWeb?.onResume(); currentWeb?.resumeTimers()
        // Opcional: mantem notificação mesmo em foreground, não para ainda
    }
    override fun onPause() {
        super.onPause()
        // Mantém audio em segundo plano - inicia foreground service estilo Spotify
        startBgService()
    }
    override fun onDestroy() {
        // Se tem musica tocando, NAO destroi WebViews - deixa Service segurar
        val hasPlaying = tabs.any { it.webView.url?.contains("youtube") == true || it.webView.url?.contains("spotify") == true || it.webView.url?.contains("music") == true }
        if (!hasPlaying) {
            for (t in tabs) try{ t.webView.destroy() }catch(_:Exception){}
            tabs.clear()
        } else {
            // Mantém tabs vivos no holder, só desanexa da Activity
            for (t in tabs) try{ (t.webView.parent as? android.view.ViewGroup)?.removeView(t.webView) }catch(_:Exception){}
            startBgService()
        }
        executor.shutdownNow()
        super.onDestroy()
    }
    override fun onTaskRemoved(rootIntent: Intent?) { super.onTaskRemoved(rootIntent); startBgService() }
    @Deprecated("Compatibility") override fun onBackPressed() {
        val w = currentWeb
        if (w != null && w.canGoBack()) w.goBack() else {
            // Spotify mode: não fecha, vai p/ segundo plano e continua tocando
            moveTaskToBack(true)
            startBgService()
        }
    }
}
