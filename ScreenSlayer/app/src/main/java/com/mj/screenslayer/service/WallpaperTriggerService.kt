package com.mj.screenslayer.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mj.screenslayer.MainActivity
import com.mj.screenslayer.util.WallpaperHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Foreground service that drives wallpaper rotation via two triggers:
 *
 * 1. Screen unlock — listens for ACTION_SCREEN_ON / ACTION_USER_PRESENT.
 *    Active when SharedPreferences["screen_trigger_enabled"] = true.
 *
 * 2. Shake — listens to TYPE_ACCELEROMETER and fires when total acceleration
 *    exceeds the user-configured threshold (default 14 m/s²).
 *    Active when SharedPreferences["shake_enabled"] = true.
 *
 * Both triggers are checked at runtime, so the service can remain running
 * while individual features are toggled on/off without restarting.
 */
class WallpaperTriggerService : Service() {

    companion object {
        const val CHANNEL_ID      = "wallpaper_trigger"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, WallpaperTriggerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WallpaperTriggerService::class.java))
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Screen-unlock trigger ─────────────────────────────────────────────────

    /** Debounce: ignore duplicate events within 2 s (SCREEN_ON then USER_PRESENT). */
    private var lastScreenChangedAt = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val prefs = getSharedPreferences("screenslayer", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("screen_trigger_enabled", false)) return

            when (intent.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    val now = System.currentTimeMillis()
                    if (now - lastScreenChangedAt > 2_000) {
                        lastScreenChangedAt = now
                        scope.launch { WallpaperHelper.applyNextWallpaper(ctx) }
                    }
                }
            }
        }
    }

    // ── Shake trigger ─────────────────────────────────────────────────────────

    private var sensorManager: SensorManager? = null
    private var lastShakeAt = 0L

    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

            val prefs = getSharedPreferences("screenslayer", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("shake_enabled", false)) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            // Net acceleration above gravity
            val accel = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH
            val threshold = prefs.getFloat("shake_sensitivity", 14f)

            if (accel > threshold) {
                val now = System.currentTimeMillis()
                if (now - lastShakeAt > 1_500) {          // 1.5 s debounce
                    lastShakeAt = now
                    scope.launch { WallpaperHelper.applyNextWallpaper(applicationContext) }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())

        // Screen unlock receiver
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        })

        // Shake sensor
        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accel ->
            sensorManager?.registerListener(shakeListener, accel, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { sensorManager?.unregisterListener(shakeListener) }
        super.onDestroy()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("ScreenSlayer")
            .setContentText("Wallpaper rotation active")
            .setContentIntent(tap)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
    }
}
