package com.al3rted.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.al3rted.app.data.AlertRepository
import com.al3rted.app.utils.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED") return

        val repo = AlertRepository.get(context)
        repo.alerts.value.filter { it.enabled }.forEach { alert ->
            AlarmScheduler.schedule(context, alert)
        }
    }
}
