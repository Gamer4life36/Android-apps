package com.al3rted.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.al3rted.app.data.AlertType
import com.al3rted.app.data.AppSettings
import com.al3rted.app.data.VibrationPattern
import com.al3rted.app.ui.AlertActivity
import kotlin.math.roundToInt

class AlertService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val savedVolumes = mutableMapOf<Int, Int>()
    private lateinit var audioManager: AudioManager

    private var fadeHandler: Handler? = null

    private var cameraManager: CameraManager? = null
    private var flashCameraId: String? = null
    private var flashHandler: Handler? = null
    private var flashOn = false

    private val allStreams = listOf(
        AudioManager.STREAM_ALARM,
        AudioManager.STREAM_RING,
        AudioManager.STREAM_MUSIC,
        AudioManager.STREAM_NOTIFICATION
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            stopAlert()
            stopSelf()
            return START_NOT_STICKY
        }

        val alertId   = intent?.getLongExtra(EXTRA_ALERT_ID, -1L) ?: -1L
        val alertName = intent?.getStringExtra(EXTRA_ALERT_NAME) ?: "Alert"
        val settings  = AppSettings.load(this)

        startForeground(NOTIFICATION_ID, buildNotification(alertName, alertId))

        val playSound = settings.alertType == AlertType.SOUND_AND_VIBRATION ||
                        settings.alertType == AlertType.SOUND_ONLY
        val doVibrate = settings.alertType == AlertType.SOUND_AND_VIBRATION ||
                        settings.alertType == AlertType.VIBRATION_ONLY

        if (playSound) {
            blastVolume(settings.volume)
            blastAudio(settings.soundUri, settings.volume, settings.fadeIn)
        }
        if (doVibrate) blastVibration(settings.vibrationPattern)
        if (settings.flashlight) startFlashlight()

        return START_STICKY
    }

    private fun blastVolume(volume: Int) {
        for (stream in allStreams) {
            savedVolumes[stream] = audioManager.getStreamVolume(stream)
            val maxVol = audioManager.getStreamMaxVolume(stream)
            val target = (volume.coerceIn(0, 100) / 100f * maxVol).roundToInt().coerceIn(0, maxVol)
            audioManager.setStreamVolume(stream, target, 0)
        }
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(false)
            .build()
        audioFocusRequest = focusRequest
        audioManager.requestAudioFocus(focusRequest)
    }

    private fun blastVibration(pattern: VibrationPattern) {
        val effect = when (pattern) {
            VibrationPattern.PULSE ->
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 500),
                    intArrayOf(0, 255, 0), 0
                )
            VibrationPattern.RAPID ->
                VibrationEffect.createWaveform(
                    longArrayOf(0, 80, 80),
                    intArrayOf(0, 255, 0), 0
                )
            VibrationPattern.ESCALATING ->
                VibrationEffect.createWaveform(
                    longArrayOf(0, 150, 100, 300, 100, 500, 100, 700, 300),
                    intArrayOf(0, 60, 0, 120, 0, 180, 0, 255, 0), 0
                )
            VibrationPattern.SOS -> {
                val dot = 100L; val dash = 300L; val gap = 100L; val pause = 400L
                VibrationEffect.createWaveform(
                    longArrayOf(
                        0, dot, gap, dot, gap, dot, pause,
                        dash, gap, dash, gap, dash, pause,
                        dot, gap, dot, gap, dot, pause
                    ),
                    intArrayOf(
                        0, 255, 0, 255, 0, 255, 0,
                        255, 0, 255, 0, 255, 0,
                        255, 0, 255, 0, 255, 0
                    ), 0
                )
            }
            VibrationPattern.LONG_WAVE ->
                VibrationEffect.createWaveform(
                    longArrayOf(0, 1500, 500),
                    intArrayOf(0, 255, 0), 0
                )
        }
        vibrator?.vibrate(effect)
    }

    private fun blastAudio(soundUri: String, volume: Int, fadeIn: Boolean) {
        try {
            val uri = soundUri.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                if (fadeIn) setVolume(0f, 0f)
                start()
                if (volume > 100) {
                    try {
                        val gainMb = ((volume - 100) * 30).coerceIn(0, 1500)
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                            setTargetGain(gainMb)
                            enabled = true
                        }
                    } catch (_: Exception) {}
                }
            }
            if (fadeIn) startFadeIn()
        } catch (_: Exception) {}
    }

    // Ramps MediaPlayer track volume 0→1 over 30 seconds (60 steps × 500ms)
    private fun startFadeIn() {
        val steps = 60
        var step = 0
        val handler = Handler(Looper.getMainLooper())
        fadeHandler = handler
        val runnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer ?: return
                if (step >= steps) { player.setVolume(1f, 1f); return }
                val v = step.toFloat() / steps
                player.setVolume(v, v)
                step++
                handler.postDelayed(this, 500)
            }
        }
        handler.post(runnable)
    }

    private fun startFlashlight() {
        try {
            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            flashCameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return
            flashHandler = Handler(Looper.getMainLooper())
            strobeFlash()
        } catch (_: Exception) {}
    }

    private fun strobeFlash() {
        flashHandler?.postDelayed({
            try {
                flashOn = !flashOn
                flashCameraId?.let { cameraManager?.setTorchMode(it, flashOn) }
            } catch (_: Exception) {}
            strobeFlash()
        }, 500)
    }

    private fun stopFlashlight() {
        flashHandler?.removeCallbacksAndMessages(null)
        flashHandler = null
        try { flashCameraId?.let { cameraManager?.setTorchMode(it, false) } } catch (_: Exception) {}
        cameraManager = null
        flashCameraId = null
        flashOn = false
    }

    private fun stopAlert() {
        fadeHandler?.removeCallbacksAndMessages(null)
        fadeHandler = null
        stopFlashlight()
        vibrator?.cancel()
        loudnessEnhancer?.apply { enabled = false; release() }
        loudnessEnhancer = null
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
        for ((stream, vol) in savedVolumes) {
            audioManager.setStreamVolume(stream, vol, 0)
        }
        savedVolumes.clear()
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    private fun buildNotification(alertName: String, alertId: Long): Notification {
        val dismissIntent = Intent(this, AlertService::class.java).apply { action = ACTION_DISMISS }
        val dismissPi = PendingIntent.getService(
            this, alertId.toInt(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenIntent = Intent(this, AlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(AlertActivity.EXTRA_ALERT_ID, alertId)
            putExtra(AlertActivity.EXTRA_ALERT_NAME, alertName)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, (alertId + 1).toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alertName)
            .setContentText("Tap to view alert")
            .setContentIntent(fullScreenPi)
            .setFullScreenIntent(fullScreenPi, true)
            .addAction(Notification.Action.Builder(null, "Dismiss", dismissPi).build())
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .build()
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(CHANNEL_ID, "Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Al3rted alarm notifications"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALERT_ID   = "alert_id"
        const val EXTRA_ALERT_NAME = "alert_name"
        const val ACTION_DISMISS   = "com.al3rted.app.DISMISS"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID      = "al3rted_channel"
    }
}
