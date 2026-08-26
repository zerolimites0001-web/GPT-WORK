package com.gptwork.browser

import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform

class LuaEngine {
    private val globals: Globals = JsePlatform.standardGlobals()

    fun eval(script: String): String = try {
        globals.load(script, "app.lua").call().tojstring()
    } catch (e: Exception) {
        ""
    }

    fun searchUrl(provider: String, query: String): String {
        val encoded = java.net.URLEncoder.encode(query, Charsets.UTF_8.name())
        val script = "local q='${encoded.replace("'", "\\'")}'; if '$provider' == 'duckduckgo' then return 'https://duckduckgo.com/?q='..q else return 'https://www.google.com/search?q='..q end"
        return eval(script).ifBlank { "https://www.google.com/search?q=$encoded" }
    }
}
