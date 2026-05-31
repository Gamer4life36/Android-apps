package com.mj.screenslayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mj.screenslayer.service.WallpaperTriggerService

/**
 * Restarts [WallpaperTriggerService] after the device reboots,
 * if the user had the screen-trigger setting enabled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("screenslayer", Context.MODE_PRIVATE)
        if (prefs.getBoolean("screen_trigger_enabled", false)) {
            WallpaperTriggerService.start(context)
        }
    }
}
