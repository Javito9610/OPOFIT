package com.opofit.miapp.gps.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.opofit.miapp.R
import com.opofit.miapp.gps.model.ActivityType
import com.opofit.miapp.gps.util.GpsMetrics
import com.opofit.miapp.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the FusedLocationProvider subscription while a GPS
 * activity is being recorded. Emits raw locations to [GpsTracker] and keeps a
 * persistent notification with quick controls.
 */
class GpsTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var fused: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var timerJob: Job? = null
    private var sensorManager: SensorManager? = null
    private var stepListener: SensorEventListener? = null
    private var hrBle: HrBleManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        // El puente HrBleManager → GpsTracker lo establece OpoFitApp via Flows.
        // Aquí solo nos aseguramos de reintentar la auto-reconexión por si la sesión
        // se inicia con el reloj recién encendido.
        hrBle = runCatching { HrBleManager.get(this).also { it.autoConnectSavedDevice() } }.getOrNull()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val type = ActivityType.fromName(intent.getStringExtra(EXTRA_TYPE))
                startSession(type)
            }
            ACTION_PAUSE -> GpsTracker.pause()
            ACTION_RESUME -> GpsTracker.resume()
            ACTION_STOP -> stopSession()
            null -> {
                // Possible restart: only continue if a session is active.
                if (!GpsTracker.isActive()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        refreshNotification()
        return START_STICKY
    }

    private fun startSession(type: ActivityType) {
        if (!hasLocationPermission()) {
            GpsTracker.setError("Permiso de ubicación denegado")
            stopSelf()
            return
        }
        val weight = WeightPreferences.get(this)
        GpsTracker.begin(type, weight)
        startInForeground()
        startLocationUpdates()
        startStepSensor(type)
        startTimer()
    }

    private fun stopSession() {
        stopLocationUpdates()
        stopStepSensor()
        timerJob?.cancel()
        timerJob = null
        ContextCompat.getMainExecutor(this).execute { stopForegroundCompat() }
        stopSelf()
    }

    private fun startStepSensor(type: ActivityType) {
        stopStepSensor()
        if (type == ActivityType.BIKE) return
        val activityRecGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        if (!activityRecGranted) return
        val sm = sensorManager ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) ?: return
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
                    GpsTracker.onStepDetected()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        stepListener = listener
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    private fun stopStepSensor() {
        stepListener?.let { sensorManager?.unregisterListener(it) }
        stepListener = null
    }

    private fun startInForeground() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif)
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

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationUpdates() {
        stopLocationUpdates()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500L)
            .setMinUpdateDistanceMeters(2f)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { GpsTracker.onLocation(it) }
                refreshNotification()
            }
        }
        locationCallback = cb
        try {
            fused.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (e: SecurityException) {
            GpsTracker.setError("Sin permiso de ubicación")
            stopSelf()
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fused.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                GpsTracker.tickSecond()
            }
        }
    }

    private fun refreshNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val state = GpsTracker.state.value
        val dist = GpsMetrics.formatDistance(state.distanceM)
        val dur = GpsMetrics.formatDuration(state.durationSec)
        val pace = GpsMetrics.formatPace(state.avgPaceSecPerKm) + " /km"

        val title = "${state.type.emoji} ${state.type.display} en curso"
        val body = "$dist · $dur · $pace"

        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pauseResume = if (state.paused) {
            actionIntent(ACTION_RESUME, "Reanudar")
        } else {
            actionIntent(ACTION_PAUSE, "Pausar")
        }
        val stop = actionIntent(ACTION_STOP, "Finalizar")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(tapIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(0, pauseResume.first, pauseResume.second)
            .addAction(0, stop.first, stop.second)
            .build()
    }

    private fun actionIntent(action: String, label: String): Pair<String, PendingIntent> {
        val intent = Intent(this, GpsTrackingService::class.java).setAction(action)
        val pi = PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return label to pi
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Grabación GPS",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mostrado mientras OpoFit registra una actividad GPS."
                    setShowBadge(false)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    override fun onDestroy() {
        stopLocationUpdates()
        stopStepSensor()
        timerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.opofit.miapp.gps.START"
        const val ACTION_PAUSE = "com.opofit.miapp.gps.PAUSE"
        const val ACTION_RESUME = "com.opofit.miapp.gps.RESUME"
        const val ACTION_STOP = "com.opofit.miapp.gps.STOP"
        const val EXTRA_TYPE = "type"

        private const val CHANNEL_ID = "gps_tracking_channel"
        private const val NOTIF_ID = 4242

        fun start(context: Context, type: ActivityType) {
            val intent = Intent(context, GpsTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TYPE, type.name)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            context.startService(Intent(context, GpsTrackingService::class.java).setAction(ACTION_PAUSE))
        }

        fun resume(context: Context) {
            context.startService(Intent(context, GpsTrackingService::class.java).setAction(ACTION_RESUME))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, GpsTrackingService::class.java).setAction(ACTION_STOP))
        }
    }
}
