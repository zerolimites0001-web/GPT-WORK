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
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var tabs: TextView
    private val lua = LuaEngine()
    private val pages = mutableListOf<String>()
    private var provider = "google"
    private var dark = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi(); configureWebView(); newTab("https://www.google.com")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(16,17,20)) }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(8,8,8,4) }
        fun button(label: String, action: () -> Unit) = TextView(this).apply { text=label; textSize=18f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(10,8,10,8); setOnClickListener { action() } }
        bar.addView(button("‹") { if (web.canGoBack()) web.goBack() })
        bar.addView(button("›") { if (web.canGoForward()) web.goForward() })
        address = EditText(this).apply { hint="Search or enter address"; singleLine=true; imeOptions=5; setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); setBackgroundColor(Color.rgb(30,32,38)); setPadding(18,0,18,0); setOnEditorActionListener { _,_,_ -> navigateInput(); true } }
        bar.addView(address, LinearLayout.LayoutParams(0,52,1f))
        bar.addView(button("↻") { web.reload() })
        tabs = button("1") { showMenu(it) }; bar.addView(tabs)
        root.addView(bar)
        web = WebView(this); root.addView(web, LinearLayout.LayoutParams(-1,0,1f))
        val nav = LinearLayout(this).apply { gravity=Gravity.CENTER; setPadding(4,2,4,4) }
        nav.addView(button("⌂") { web.loadUrl("https://www.google.com") })
        nav.addView(button("＋") { newTab("https://www.google.com") })
        nav.addView(button("☆") { bookmark() })
        nav.addView(button("☰") { showMenu(it) })
        root.addView(nav); setContentView(root)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.databaseEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.setSupportZoom(true)
        web.settings.builtInZoomControls = false
        web.settings.displayZoomControls = false
        web.settings.mediaPlaybackRequiresUserGesture = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) web.settings.safeBrowsingEnabled = true
        web.settings.userAgentString = web.settings.userAgentString + " GPT-WORK/1.0"
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme ?: return false
                return scheme != "http" && scheme != "https"
            }
            override fun onPageFinished(view: WebView, url: String) { address.setText(url); title = view.title ?: "GPT-WORK" }
        }
        web.webChromeClient = WebChromeClient()
        web.setDownloadListener(DownloadListener { url, userAgent, _, mimeType, _ ->
            val req = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType); addRequestHeader("User-Agent", userAgent); setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Uri.parse(url).lastPathSegment ?: "download")
            }
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
        })
    }

    private fun navigateInput() {
        val input = address.text.toString().trim(); if (input.isBlank()) return
        val url = if (input.startsWith("http://") || input.startsWith("https://")) input else lua.searchUrl(provider, input)
        web.loadUrl(url)
    }
    private fun newTab(url: String) { pages.add(url); web.loadUrl(url); tabs.text=pages.size.toString() }
    private fun bookmark() { getPreferences(0).edit().putString("bookmark_${System.currentTimeMillis()}", web.url).apply(); Toast.makeText(this,"Bookmarked",Toast.LENGTH_SHORT).show() }

    private fun showMenu(anchor: View) {
        val menu = PopupMenu(this, anchor)
        menu.menu.add(if(provider=="google") "Search: Google ✓" else "Search: Google").setOnMenuItemClickListener { provider="google"; true }
        menu.menu.add(if(provider=="duckduckgo") "Search: DuckDuckGo ✓" else "Search: DuckDuckGo").setOnMenuItemClickListener { provider="duckduckgo"; true }
        menu.menu.add(if(dark) "Theme: Dark ✓" else "Theme: Dark").setOnMenuItemClickListener { dark=true; applyTheme(); true }
        menu.menu.add(if(!dark) "Theme: Light ✓" else "Theme: Light").setOnMenuItemClickListener { dark=false; applyTheme(); true }
        menu.menu.add("Clear browsing data").setOnMenuItemClickListener { web.clearHistory(); web.clearCache(true); true }
        menu.menu.add("Reload").setOnMenuItemClickListener { web.reload(); true }; menu.show()
    }
    private fun applyTheme() { val bg=if(dark) Color.rgb(16,17,20) else Color.WHITE; window.statusBarColor=bg; window.navigationBarColor=bg; address.setTextColor(if(dark) Color.WHITE else Color.DKGRAY) }
    @Deprecated("Deprecated in Android 13; browser history handling remains compatible")
    override fun onBackPressed() { if(web.canGoBack()) web.goBack() else super.onBackPressed() }
}
