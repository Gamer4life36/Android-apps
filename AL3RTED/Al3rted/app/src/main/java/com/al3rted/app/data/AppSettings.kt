package com.al3rted.app.data

import android.content.Context
import android.media.RingtoneManager

enum class AlertType(val label: String, val description: String) {
    SOUND_AND_VIBRATION("Sound & Vibration", "Ring and vibrate"),
    SOUND_ONLY("Sound Only", "Ring only, no vibration"),
    VIBRATION_ONLY("Vibration Only", "Silent vibration, no sound"),
    SCREEN_ONLY("Screen Only", "Bright screen only, no sound or vibration");
    companion object { fun fromOrdinal(o: Int) = entries.getOrElse(o) { SOUND_AND_VIBRATION } }
}

enum class VibrationPattern(val label: String, val description: String) {
    PULSE("Pulse", "Steady on-off beats"),
    RAPID("Rapid", "Fast short bursts"),
    ESCALATING("Escalating", "Ramps up in intensity"),
    SOS("SOS", "· · · — — — · · ·"),
    LONG_WAVE("Long Wave", "Slow rolling waves");
    companion object { fun fromOrdinal(o: Int) = entries.getOrElse(o) { PULSE } }
}

data class AppSettings(
    val soundUri: String,
    val volume: Int,              // 0–150; >100 boosts via LoudnessEnhancer
    val alertType: AlertType,
    val vibrationPattern: VibrationPattern,
    val snoozeDuration: Int,      // minutes
    val fadeIn: Boolean,
    val flashlight: Boolean
) {
    companion object {
        private const val KEY_SOUND_URI         = "sound_uri"
        private const val KEY_VOLUME            = "volume"
        private const val KEY_ALERT_TYPE        = "alert_type"
        private const val KEY_VIBRATION_PATTERN = "vibration_pattern"
        private const val KEY_SNOOZE_DURATION   = "snooze_duration"
        private const val KEY_FADE_IN           = "fade_in"
        private const val KEY_FLASHLIGHT        = "flashlight"

        fun load(context: Context): AppSettings {
            val prefs = context.getSharedPreferences("al3rted_prefs", Context.MODE_PRIVATE)
            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString() ?: ""
            return AppSettings(
                soundUri         = prefs.getString(KEY_SOUND_URI, defaultUri) ?: defaultUri,
                volume           = prefs.getInt(KEY_VOLUME, 100),
                alertType        = AlertType.fromOrdinal(prefs.getInt(KEY_ALERT_TYPE, 0)),
                vibrationPattern = VibrationPattern.fromOrdinal(prefs.getInt(KEY_VIBRATION_PATTERN, 0)),
                snoozeDuration   = prefs.getInt(KEY_SNOOZE_DURATION, 5),
                fadeIn           = prefs.getBoolean(KEY_FADE_IN, false),
                flashlight       = prefs.getBoolean(KEY_FLASHLIGHT, false)
            )
        }

        fun save(context: Context, settings: AppSettings) {
            context.getSharedPreferences("al3rted_prefs", Context.MODE_PRIVATE).edit()
                .putString(KEY_SOUND_URI, settings.soundUri)
                .putInt(KEY_VOLUME, settings.volume)
                .putInt(KEY_ALERT_TYPE, settings.alertType.ordinal)
                .putInt(KEY_VIBRATION_PATTERN, settings.vibrationPattern.ordinal)
                .putInt(KEY_SNOOZE_DURATION, settings.snoozeDuration)
                .putBoolean(KEY_FADE_IN, settings.fadeIn)
                .putBoolean(KEY_FLASHLIGHT, settings.flashlight)
                .apply()
        }
    }
}
