package com.opofit.miapp.gps.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.opofit.miapp.R
import com.opofit.miapp.ui.MainActivity
import com.opofit.miapp.utils.TimeFormatUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mantiene el cronómetro activo en segundo plano con notificación persistente.
 * Usado por simulacro oficial y sesiones de entrenamiento.
 */
class ChronoForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundTimer()
            ACTION_PAUSE -> {
                SessionTimerTracker.pause()
                refreshNotification()
            }
            ACTION_RESUME -> {
                SessionTimerTracker.resume()
                refreshNotification()
            }
            ACTION_STOP -> stopForegroundTimer()
            // Antes: si nos relanza el sistema sin sesión activa hacíamos stopSelf
            // pero NO quitábamos la notificación foreground → quedaba colgada en
            // la barra hasta reiniciar. Ahora forzamos cleanup completo.
            null -> if (!SessionTimerTracker.state.value.active) stopForegroundTimer()
        }
        // START_NOT_STICKY evita que Android nos relance con intent=null y revivamos
        // la notificación fantasma cuando ya no hay sesión.
        return START_NOT_STICKY
    }

    private fun startForegroundTimer() {
        startForegroundCompat()
        tickJob?.cancel()
        tickJob = scope.launch {
            while (SessionTimerTracker.state.value.active) {
                delay(50L)
                SessionTimerTracker.tick(50L)
                refreshNotification()
            }
        }
        refreshNotification()
    }

    private fun stopForegroundTimer() {
        tickJob?.cancel()
        tickJob = null
        SessionTimerTracker.stop()
        stopForegroundCompat()
        stopSelf()
    }

    private fun refreshNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val st = SessionTimerTracker.state.value
        val time = TimeFormatUtil.formatElapsedMs(st.elapsedMs)
        val title = if (st.paused) "⏸ ${st.label} (pausado)" else "⏱ ${st.label}"
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResume = if (st.paused) {
            actionIntent(ACTION_RESUME, "Reanudar")
        } else {
            actionIntent(ACTION_PAUSE, "Pausar")
        }
        val stop = actionIntent(ACTION_STOP, "Detener")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            // Icono monocromo dedicado: si usábamos R.mipmap.ic_launcher la
            // status bar mostraba un círculo blanco genérico porque Android
            // exige icono sin color para notificaciones desde API 21.
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(time)
            .setContentIntent(tapIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, pauseResume.first, pauseResume.second)
            .addAction(0, stop.first, stop.second)
            .build()
    }

    private fun actionIntent(action: String, label: String): Pair<String, PendingIntent> {
        val intent = Intent(this, ChronoForegroundService::class.java).setAction(action)
        val pi = PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return label to pi
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Cronómetro de entreno",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Mantiene el cronómetro activo mientras entrenas o haces un simulacro."
                        setShowBadge(false)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.opofit.miapp.chrono.START"
        const val ACTION_PAUSE = "com.opofit.miapp.chrono.PAUSE"
        const val ACTION_RESUME = "com.opofit.miapp.chrono.RESUME"
        const val ACTION_STOP = "com.opofit.miapp.chrono.STOP"

        private const val CHANNEL_ID = "chrono_timer_channel"
        private const val NOTIF_ID = 4343

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ChronoForegroundService::class.java).setAction(ACTION_START)
            )
        }

        fun pause(context: Context) {
            context.startService(
                Intent(context, ChronoForegroundService::class.java).setAction(ACTION_PAUSE)
            )
        }

        fun resume(context: Context) {
            context.startService(
                Intent(context, ChronoForegroundService::class.java).setAction(ACTION_RESUME)
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ChronoForegroundService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
