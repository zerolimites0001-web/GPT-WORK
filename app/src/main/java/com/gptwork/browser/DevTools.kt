package com.gptwork.browser
import java.text.SimpleDateFormat
import java.util.*
object DevTools {
    data class NetEntry(val time:String, val url:String, val method:String, val headers:Map<String,String>, val type:String)
    data class ConsoleEntry(val time:String, val level:String, val msg:String)
    val network = mutableListOf<NetEntry>()
    val console = mutableListOf<ConsoleEntry>()
    val resources = mutableListOf<String>()
    private fun now() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    fun logNet(url:String, method:String="GET", headers:Map<String,String> = emptyMap(), type:String="request"){ synchronized(network){ network.add(NetEntry(now(),url,method,headers,type)); if(network.size>500) network.removeAt(0) }}
    fun logConsole(level:String, msg:String){ synchronized(console){ console.add(ConsoleEntry(now(),level,msg)); if(console.size>500) console.removeAt(0) }}
    fun logResource(url:String){ synchronized(resources){ if(url !in resources){ resources.add(url); if(resources.size>500) resources.removeAt(0) }}}
    fun clear(){ network.clear(); console.clear(); resources.clear() }
    fun dumpSource(web:String):String = web
}
