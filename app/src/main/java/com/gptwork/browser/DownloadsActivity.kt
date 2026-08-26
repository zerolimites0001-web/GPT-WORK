package com.gptwork.browser

import android.app.DownloadManager
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DownloadsActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private lateinit var handler: Handler
    private val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        val bg = Color.rgb(15,17,20)
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18,18,18,18) }
        scroll.addView(container)
        setContentView(scroll)
        // Toolbar
        val tb = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0,0,0,12) }
        tb.addView(TextView(this).apply { text = "📁 Downloads"; textSize = 22f; setTextColor(Color.WHITE); typeface = android.graphics.Typeface.DEFAULT_BOLD })
        tb.addView(Space(this).apply { minimumWidth = 16 })
        val refresh = Button(this).apply { text = "↻"; setOnClickListener { load() } }
        tb.addView(refresh)
        container.addView(tb)
        container.addView(TextView(this).apply { text = "Salva em: app/files/downloads e /sdcard/Download"; setTextColor(Color.rgb(122,133,145)); textSize = 11f; setPadding(0,0,0,12) })
        load()
        // Auto refresh a cada 1s p/ tempo
        handler.postDelayed(object: Runnable{ override fun run(){ refreshTimes(); handler.postDelayed(this, 1000) } }, 1000)
    }

    private fun refreshTimes() {
        // só atualiza tempos visíveis sem recarregar lista completa
        for(i in 0 until container.childCount){
            val v = container.getChildAt(i)
            if(v is LinearLayout && v.tag is Long){
                val start = v.tag as Long
                val elapsed = (System.currentTimeMillis() - start)/1000
                val tm = v.findViewWithTag<TextView>("time_$i")
                tm?.let{ it.text = "⏱ ${elapsed}s • ${sdf.format(Date(start))}" }
            }
        }
    }

    private fun load() {
        // limpa mas mantém header
        while(container.childCount > 3) container.removeViewAt(3)
        val appDir = File(filesDir, "downloads")
        val sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appFiles = if(appDir.exists()) appDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList() else emptyList()
        val dm = getSystemService(DownloadManager::class.java)
        val cursor: Cursor? = try { dm.query(DownloadManager.Query()) } catch(_:Exception){ null }

        var count = 0
        // App files
        if(appFiles.isNotEmpty()){
            container.addView(section("📱 App local (files/downloads) - ${appFiles.size}"))
            for(f in appFiles.take(20)){
                container.addView(cardForAppFile(f))
                count++
            }
        }
        // DownloadManager
        if(cursor != null){
            val ids = mutableListOf<Map<String,Any>>()
            try{
                while(cursor.moveToNext()){
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                    val title = try{ cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) }catch(_:Exception){ "?" }
                    val status = try{ cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) }catch(_:Exception){ -1 }
                    val bytesSoFar = try{ cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) }catch(_:Exception){0}
                    val bytesTotal = try{ cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)) }catch(_:Exception){0}
                    val localUri = try{ cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)) }catch(_:Exception){null}
                    val time = try{ cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)) }catch(_:Exception){ System.currentTimeMillis()}
                    ids.add(mapOf("id" to id, "title" to title, "status" to status, "sofar" to bytesSoFar, "total" to bytesTotal, "uri" to (localUri?:""), "time" to time))
                }
            }finally{ cursor.close() }
            if(ids.isNotEmpty()){
                container.addView(section("⬇️ DownloadManager (/sdcard/Download) - ${ids.size}"))
                for(m in ids.take(20)){
                    container.addView(cardForDM(m, dm))
                    count++
                }
            }
        }
        // SD dir manual
        val sdFiles = if(sdDir.exists()) sdDir.listFiles()?.sortedByDescending { it.lastModified() }?.take(20) ?: emptyList() else emptyList()
        if(sdFiles.isNotEmpty() && count==0){
            container.addView(section("💾 /sdcard/Download - ${sdFiles.size}"))
            for(f in sdFiles) container.addView(cardForAppFile(f))
        }
        if(count==0){
            container.addView(TextView(this).apply { text = "Nenhum download ainda.\nBaixe algo pelo browser."; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0,40,0,0) })
        }
        // Permissão info
        container.addView(TextView(this).apply {
            text = "\nPermissões: no Android 13+ não precisa pedir WRITE_EXTERNAL_STORAGE para /sdcard/Download (usa Media). Em <13 pede automaticamente ao baixar.";
            setTextColor(Color.rgb(122,133,145)); textSize = 11f; setPadding(0,16,0,0)
        })
    }

    private fun section(t:String) = TextView(this).apply { text = t; textSize = 13f; setTextColor(Color.rgb(124,156,255)); setPadding(0,16,0,8); typeface = android.graphics.Typeface.DEFAULT_BOLD }

    private fun cardForAppFile(f: File): LinearLayout {
        val start = f.lastModified()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(30,32,38))
            setPadding(14,12,14,12)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(30,32,38)); cornerRadius = 14f; setStroke(1, Color.rgb(42,46,56)) }
            setPadding(14,12,14,12)
            val params = LinearLayout.LayoutParams(-1, -2); params.setMargins(0,0,0,10); layoutParams = params
            tag = start
            addView(TextView(this@DownloadsActivity).apply { text = "📄 ${f.name}"; setTextColor(Color.WHITE); textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD; maxLines = 2 })
            addView(TextView(this@DownloadsActivity).apply { text = "📦 ${f.length()/1024} KB • ${sdf.format(Date(start))}"; setTextColor(Color.rgb(122,133,145)); textSize = 11f })
            val prog = ProgressBar(this@DownloadsActivity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 100; progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(124,156,255)) }
            addView(prog, LinearLayout.LayoutParams(-1, 6).apply { setMargins(0,6,0,0) })
            addView(TextView(this@DownloadsActivity).apply { tag = "time_${container.childCount}"; text = "⏱ ${ (System.currentTimeMillis()-start)/1000 }s • ${sdf.format(Date(start))}"; setTextColor(Color.rgb(158,228,147)); textSize = 11f })
            val row = LinearLayout(this@DownloadsActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(Button(this@DownloadsActivity).apply { text = "Abrir"; textSize = 11f; setOnClickListener { openFile(f) } }, LinearLayout.LayoutParams(0, -2, 1f).apply{ setMargins(0,6,4,0)})
            row.addView(Button(this@DownloadsActivity).apply { text = "Deletar"; textSize = 11f; setOnClickListener {
                AlertDialog.Builder(this@DownloadsActivity).setTitle("Deletar?").setMessage(f.name).setPositiveButton("Deletar"){_,_-> f.delete(); Toast.makeText(this@DownloadsActivity,"Deletado",Toast.LENGTH_SHORT).show(); load() }.setNegativeButton("Cancelar",null).show()
            } }, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,6,0,0)})
            addView(row)
        }
    }

    private fun cardForDM(m: Map<String,Any>, dm: DownloadManager): LinearLayout {
        val title = m["title"] as String
        val status = m["status"] as Int
        val sofar = m["sofar"] as Long
        val total = m["total"] as Long
        val id = m["id"] as Long
        val time = m["time"] as Long
        val statusStr = when(status){
            DownloadManager.STATUS_RUNNING -> "Baixando"
            DownloadManager.STATUS_PAUSED -> "Pausado"
            DownloadManager.STATUS_SUCCESSFUL -> "Concluído"
            DownloadManager.STATUS_FAILED -> "Falhou"
            DownloadManager.STATUS_PENDING -> "Pendente"
            else -> "Desconhecido"
        }
        val prog = if(total>0) (sofar*100/total).toInt() else 0
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(30,32,38))
            setPadding(14,12,14,12)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.rgb(30,32,38)); cornerRadius = 14f; setStroke(1, Color.rgb(42,46,56)) }
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,0,0,10)}
            tag = time
            addView(TextView(this@DownloadsActivity).apply { text = "⬇️ $title"; setTextColor(Color.WHITE); textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD; maxLines = 2 })
            addView(TextView(this@DownloadsActivity).apply { text = "$statusStr • ${sofar/1024}KB / ${if(total>0) "${total/1024}KB" else "?"} • $prog%"; setTextColor(Color.rgb(122,133,145)); textSize = 11f })
            val pb = ProgressBar(this@DownloadsActivity, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = prog; progressTintList = android.content.res.ColorStateList.valueOf(if(status==DownloadManager.STATUS_FAILED) Color.RED else Color.rgb(124,156,255)) }
            addView(pb, LinearLayout.LayoutParams(-1,6).apply{ setMargins(0,6,0,0)})
            addView(TextView(this@DownloadsActivity).apply { tag = "time_${container.childCount}"; text = "⏱ ${ (System.currentTimeMillis()-time)/1000 }s • ${sdf.format(Date(time))}"; setTextColor(Color.rgb(158,228,147)); textSize = 11f })
            val row = LinearLayout(this@DownloadsActivity).apply { orientation = LinearLayout.HORIZONTAL }
            // Pause/Resume not natively supported - we simulate by cancel and restart
            val pauseBtn = Button(this@DownloadsActivity).apply {
                text = if(status==DownloadManager.STATUS_RUNNING) "Pausar" else "Retomar"
                textSize = 11f
                setOnClickListener {
                    // DownloadManager não tem pause nativo - remove e avisa
                    Toast.makeText(this@DownloadsActivity, "DownloadManager não suporta pausa nativa. Use 'Deletar' e baixe de novo.", Toast.LENGTH_LONG).show()
                }
            }
            row.addView(pauseBtn, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,6,4,0)})
            row.addView(Button(this@DownloadsActivity).apply { text = "Deletar"; textSize = 11f; setOnClickListener {
                AlertDialog.Builder(this@DownloadsActivity).setTitle("Deletar download $id?").setPositiveButton("Deletar"){_,_-> dm.remove(id); Toast.makeText(this@DownloadsActivity,"Deletado",Toast.LENGTH_SHORT).show(); load() }.setNegativeButton("Cancelar",null).show()
            } }, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,6,4,0)})
            row.addView(Button(this@DownloadsActivity).apply { text = "Abrir"; textSize = 11f; setOnClickListener {
                try{
                    val uri = dm.getUriForDownloadedFile(id)
                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                } catch(e:Exception){ Toast.makeText(this@DownloadsActivity,"Arquivo não pronto: $e",Toast.LENGTH_SHORT).show() }
            } }, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,6,0,0)})
            addView(row)
        }
    }

    private fun openFile(f: File){
        try{
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", f)
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Abrir com"))
        } catch(e:Exception){
            // fallback sem FileProvider - tenta abrir direto
            try{ startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(f), "*/*")) } catch(_:Exception){
                Toast.makeText(this, f.absolutePath, Toast.LENGTH_LONG).show()
            }
        }
    }
}
