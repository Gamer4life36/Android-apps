package com.al3rted.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.al3rted.app.data.AlertRepository
import com.al3rted.app.service.AlertService
import com.al3rted.app.utils.AlarmScheduler

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getLongExtra(EXTRA_ALERT_ID, -1L)
        val day = intent.getIntExtra(EXTRA_DAY, -1)
        if (alertId == -1L) return

        val repo = AlertRepository.get(context)
        val alert = repo.alerts.value.find { it.id == alertId } ?: return

        if (!alert.enabled) return

        AlarmScheduler.rescheduleNext(context, alert, day)

        val serviceIntent = Intent(context, AlertService::class.java).apply {
            putExtra(AlertService.EXTRA_ALERT_ID, alertId)
            putExtra(AlertService.EXTRA_ALERT_NAME, alert.name)
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_DAY = "alert_day"
    }
}
