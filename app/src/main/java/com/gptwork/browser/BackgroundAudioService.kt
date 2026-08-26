package com.gptwork.browser

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BackgroundAudioService : Service() {
    companion object {
        const val CHANNEL_ID = "gptwork_playback"
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "GPT-WORK Playback", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Reprodução em segundo plano"
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val openIntent = Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        val pendingOpen = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPT-WORK • Tocando em segundo plano")
            .setContentText("YouTube/Spotify web continua tocando • Toque para voltar")
            .setSmallIcon(R.drawable.ic_music_note) // fallback to ic_home if missing
            .setOngoing(true)
            .setColor(Color.rgb(124,156,255))
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_back, "Parar", pendingStop)
            // MediaStyle removido p/ evitar dependencia extra
            .build()
        try {
            // Try with foregroundServiceType for Android 14+
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Exception) {
            try { startForeground(NOTIF_ID, notif) } catch (_:Exception) {}
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
