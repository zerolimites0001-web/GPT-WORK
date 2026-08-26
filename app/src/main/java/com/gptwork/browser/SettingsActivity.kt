package com.gptwork.browser

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class SettingsActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("gptwork", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val act = this
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.rgb(15,17,20)) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,24) }

        fun title(t: String) = TextView(this).apply { text = t; textSize = 18f; setTextColor(Color.WHITE); setPadding(0,22,0,10); typeface = android.graphics.Typeface.DEFAULT_BOLD }
        fun subtitle(t: String) = TextView(this).apply { text = t; setTextColor(Color.rgb(122,133,145)); textSize = 12f; setPadding(0,0,0,8) }
        fun card(block: LinearLayout.()->Unit) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(30,32,38)); setPadding(18,16,18,16); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(30,32,38)); cornerRadius = 18f; setStroke(1, Color.rgb(42,46,56)) }; block() }

        // Header
        root.addView(TextView(this).apply { text = "⚙️ Configurações"; textSize = 26f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD })
        root.addView(subtitle("GPT-WORK Browser • Privacidade, segurança e desempenho"))

        // Geral
        root.addView(title("🌐 Geral"))
        root.addView(card {
            addView(subtitle("Homepage • Tela inicial própria"))
            val home = EditText(act).apply { setText(prefs.getString("homepage","file:///android_asset/home.html")); hint = "file:///android_asset/home.html ou https://..."; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
            addView(home)
            addView(Switch(act).apply { text = "Modo desktop (UA)"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("desktop",false); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("desktop", v) } } })
            addView(Switch(act).apply { text = "JavaScript"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("js",true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("js", v) } } })
            addView(subtitle("Buscador: toque para alternar Google/DuckDuckGo"))
            val prov = prefs.getString("provider","google")
            addView(Button(act).apply { text = "Buscador atual: $prov (tocar para alternar)"; setOnClickListener { val n = if (prefs.getString("provider","google")=="google") "duckduckgo" else "google"; prefs.edit { putString("provider", n) }; text = "Buscador atual: $n"; Toast.makeText(act,"Buscador: $n",Toast.LENGTH_SHORT).show() } })
        })

        // Armazenamento
        root.addView(title("💾 Armazenamento"))
        root.addView(card {
            addView(subtitle("IndexDB • LocalStorage • Cookies • Cache"))
            addView(Switch(act).apply { text = "LocalStorage / DOM Storage"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("domStorage",true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("domStorage", v) } } })
            addView(Switch(act).apply { text = "IndexDB (databaseEnabled)"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("indexdb",true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("indexdb", v) } } })
            addView(Switch(act).apply { text = "Cookies (incl. terceiro)"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("cookies",true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("cookies", v) }; CookieManager.getInstance().setAcceptCookie(v); try{ CookieManager.getInstance().setAcceptThirdPartyCookies(null as android.webkit.WebView?, v)}catch(_:Exception){} } })
            addView(subtitle("Gerenciar dados"))
            addView(LinearLayout(act).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(Button(act).apply { text = "Ver Cookies"; setOnClickListener { showCookies() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,6,0) })
                addView(Button(act).apply { text = "Limpar tudo"; setOnClickListener { clearAll() } }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            })
        })

        // Redirect & OAuth
        root.addView(title("🔐 Redirect & OAuth"))
        root.addView(card {
            addView(subtitle("Permite login com Google, GitHub, etc. via OAuth"))
            addView(Switch(act).apply { text = "Permitir Redirects"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("redirect",true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("redirect", v) } } })
            addView(Switch(act).apply { text = "OAuth Google / GitHub / Microsoft"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("oauth",true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("oauth", v) } } })
            addView(Switch(act).apply { text = "Pop-ups (OAuth precisa)"; setTextColor(Color.WHITE); isChecked = !prefs.getBoolean("popups",true); text = if(isChecked) "Pop-ups permitidos" else "Pop-ups bloqueados"; setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("popups", !v) } } })
            addView(subtitle("Domínios OAuth permitidos: accounts.google.com, github.com, login.microsoftonline.com, appleid.apple.com"))
        })

        // Segurança básica
        root.addView(title("🛡️ Segurança básica"))
        root.addView(card {
            addView(Switch(act).apply { text = "Safe Browsing (Google)"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("safeBrowsing", false); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("safeBrowsing", v) } } })
            addView(Switch(act).apply { text = "HTTPS apenas (bloqueia HTTP)"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("httpsOnly", false); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("httpsOnly", v) } } })
            addView(Switch(act).apply { text = "Bloquear conteúdo misto"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("blockMixed", true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("blockMixed", v) } } })
        })

        // Privacidade básica
        root.addView(title("👁️ Privacidade básica"))
        root.addView(card {
            addView(Switch(act).apply { text = "Do Not Track"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("dnt", true); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("dnt", v) } } })
            addView(Switch(act).apply { text = "Modo privado (não salva histórico)"; setTextColor(Color.WHITE); isChecked = prefs.getBoolean("incognito", false); setOnCheckedChangeListener { _, v -> prefs.edit { putBoolean("incognito", v) } } })
            addView(subtitle("DNS / DoH"))
            val dns = EditText(act).apply { setText(prefs.getString("dns","")); hint = "https://dns.google/dns-query"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
            addView(dns)
            addView(Button(act).apply { text = "Salvar DNS"; setOnClickListener { prefs.edit { putString("dns", dns.text.toString()) }; Toast.makeText(act,"DNS salvo",Toast.LENGTH_SHORT).show() } })
        })

        // Sobre
        root.addView(card {
            addView(TextView(this).apply { text = "GPT-WORK Browser v2.1 • Turbo A02 • SVG + Lua\nWebView 35 • IndexDB / LocalStorage / Cookies • OAuth"; setTextColor(Color.rgb(122,133,145)); textSize = 11f; gravity = Gravity.CENTER; setPadding(0,12,0,0) })
        })

        root.addView(Space(this).apply { minimumHeight = 24 })
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun showCookies() {
        val cm = CookieManager.getInstance()
        val cookies = cm.getCookie("https://www.google.com") ?: "Nenhum cookie"
        AlertDialog.Builder(this).setTitle("🍪 Cookies").setMessage(cookies.take(2000)).setPositiveButton("Fechar",null).setNeutralButton("Limpar cookies"){_,_-> cm.removeAllCookies(null); cm.flush(); Toast.makeText(this,"Cookies limpos",Toast.LENGTH_SHORT).show() }.show()
    }
    private fun clearAll() {
        AlertDialog.Builder(this).setTitle("Limpar tudo?").setMessage("Vai apagar cookies, cache, localStorage e IndexDB").setPositiveButton("Limpar"){_,_->
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
            getSharedPreferences("gptwork", MODE_PRIVATE).edit { clear(); apply() }
            Toast.makeText(this,"Tudo limpo! Reinicie o app",Toast.LENGTH_LONG).show()
        }.setNegativeButton("Cancelar",null).show()
    }
}
