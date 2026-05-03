package com.example.dogmap.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.dogmap.R
import com.example.dogmap.MainActivity
import com.example.dogmap.data.models.TrackPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalkTrackingService : Service() {

    enum class State { Idle, Recording, Paused }

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastLocation: Location? = null
    private var startElapsedMs: Long = 0L
    private var accumulatedMs: Long = 0L
    private var pauseStartMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (_state.value == State.Recording) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        startForeground(NOTIF_ID, buildNotification("Iniciando paseo…"))

        _points.value = emptyList()
        _distanceMeters.value = 0.0
        _durationSeconds.value = 0L
        lastLocation = null
        startElapsedMs = System.currentTimeMillis()
        accumulatedMs = 0L
        _state.value = State.Recording

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (_state.value != State.Recording) return
                for (loc in result.locations) {
                    val tp = TrackPoint(loc.latitude, loc.longitude, System.currentTimeMillis())
                    _points.value = _points.value + tp
                    val prev = lastLocation
                    if (prev != null) {
                        val delta = prev.distanceTo(loc).toDouble()
                        if (delta < 200.0) {
                            _distanceMeters.value = _distanceMeters.value + delta
                        }
                    }
                    lastLocation = loc
                    val elapsedSinceResume = System.currentTimeMillis() - startElapsedMs
                    _durationSeconds.value = (accumulatedMs + elapsedSinceResume) / 1000L
                    notifyProgress()
                }
            }
        }
        locationCallback = cb
        try {
            fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun pauseRecording() {
        if (_state.value != State.Recording) return
        _state.value = State.Paused
        pauseStartMs = System.currentTimeMillis()
        accumulatedMs += pauseStartMs - startElapsedMs
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        notifyProgress()
    }

    private fun resumeRecording() {
        if (_state.value != State.Paused) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        _state.value = State.Recording
        startElapsedMs = System.currentTimeMillis()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(3f)
            .build()
        val cb = locationCallback ?: return
        try {
            fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (_: SecurityException) {}
        notifyProgress()
    }

    private fun stopRecording() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
        if (_state.value == State.Recording) {
            accumulatedMs += System.currentTimeMillis() - startElapsedMs
            _durationSeconds.value = accumulatedMs / 1000L
        }
        _state.value = State.Idle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun notifyProgress() {
        val km = String.format("%.2f", _distanceMeters.value / 1000.0)
        val mins = _durationSeconds.value / 60
        val secs = _durationSeconds.value % 60
        val statusLabel = if (_state.value == State.Paused) "Pausado" else "Grabando"
        val text = "$statusLabel · $km km · ${mins}:${"%02d".format(secs)}"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Grabando paseo")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_dog_marker)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Grabación de paseos",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notificación persistente mientras grabas un paseo"
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.dogmap.walk.START"
        const val ACTION_PAUSE = "com.example.dogmap.walk.PAUSE"
        const val ACTION_RESUME = "com.example.dogmap.walk.RESUME"
        const val ACTION_STOP = "com.example.dogmap.walk.STOP"
        private const val CHANNEL_ID = "walk_tracking"
        private const val NOTIF_ID = 4242

        private val _state = MutableStateFlow(State.Idle)
        val state: StateFlow<State> = _state.asStateFlow()

        private val _points = MutableStateFlow<List<TrackPoint>>(emptyList())
        val points: StateFlow<List<TrackPoint>> = _points.asStateFlow()

        private val _distanceMeters = MutableStateFlow(0.0)
        val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

        private val _durationSeconds = MutableStateFlow(0L)
        val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

        fun start(context: Context) {
            val i = Intent(context, WalkTrackingService::class.java).apply { action = ACTION_START }
            ContextCompat.startForegroundService(context, i)
        }

        fun pause(context: Context) {
            val i = Intent(context, WalkTrackingService::class.java).apply { action = ACTION_PAUSE }
            context.startService(i)
        }

        fun resume(context: Context) {
            val i = Intent(context, WalkTrackingService::class.java).apply { action = ACTION_RESUME }
            context.startService(i)
        }

        fun stop(context: Context) {
            val i = Intent(context, WalkTrackingService::class.java).apply { action = ACTION_STOP }
            context.startService(i)
        }

        fun snapshot(): Triple<List<TrackPoint>, Double, Long> =
            Triple(_points.value, _distanceMeters.value, _durationSeconds.value)

        fun resetSession() {
            _state.value = State.Idle
            _points.value = emptyList()
            _distanceMeters.value = 0.0
            _durationSeconds.value = 0L
        }
    }
}
