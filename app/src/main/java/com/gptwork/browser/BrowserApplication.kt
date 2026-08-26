package com.gptwork.browser

import android.app.Application
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView

class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Warm-up WebView process in background - ganho de ~30-40% no A02
        Thread {
            try {
                CookieManager.getInstance().setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= 28) {
                    WebView.setDataDirectorySuffix("gptwork")
                }
                // Pre-inicializa o renderer - não cria WebView ainda, só aquece
                CookieManager.getInstance().flush()
            } catch (_: Exception) {}
        }.apply { priority = Thread.MIN_PRIORITY; start() }
    }
}
