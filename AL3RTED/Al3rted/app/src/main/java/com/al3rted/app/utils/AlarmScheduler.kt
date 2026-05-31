package com.al3rted.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.al3rted.app.data.Alert
import com.al3rted.app.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alert: Alert) {
        cancel(context, alert)
        if (!alert.enabled || alert.days.isEmpty()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in alert.days) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, day)
                set(Calendar.HOUR_OF_DAY, alert.hourOfDay)
                set(Calendar.MINUTE, alert.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }

            val rc = (alert.id % Int.MAX_VALUE).toInt() * 10 + day
            val pi = pendingIntent(context, alert.id, day)
            val showIntent = PendingIntent.getActivity(
                context, rc + 1000,
                android.content.Intent(context, com.al3rted.app.ui.AlertActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, showIntent), pi)
        }
    }

    fun cancel(context: Context, alert: Alert) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            am.cancel(pendingIntent(context, alert.id, day))
        }
    }

    fun rescheduleNext(context: Context, alert: Alert, firedDay: Int) {
        if (!alert.enabled) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firedDay)
            set(Calendar.HOUR_OF_DAY, alert.hourOfDay)
            set(Calendar.MINUTE, alert.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.WEEK_OF_YEAR, 1)
        }
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent(context, alert.id, firedDay))
    }

    private fun pendingIntent(context: Context, alertId: Long, day: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALERT_ID, alertId)
            putExtra(AlarmReceiver.EXTRA_DAY, day)
        }
        val requestCode = (alertId % Int.MAX_VALUE).toInt() * 10 + day
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
