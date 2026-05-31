package com.al3rted.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.al3rted.app.data.AppSettings
import com.al3rted.app.service.AlertService

class AlertActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Max brightness + keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val lp = window.attributes
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = lp

        val alertName     = intent.getStringExtra(EXTRA_ALERT_NAME) ?: "ALERT"
        val snoozeDuration = AppSettings.load(this).snoozeDuration

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#CC0000"))
            val pad = (32 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = alertName.uppercase()
            textSize = 42f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val timeView = TextView(this).apply {
            val cal = java.util.Calendar.getInstance()
            text = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
            textSize = 64f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            val topMargin = (24 * resources.displayMetrics.density).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = topMargin }
        }

        val btnDismiss = Button(this).apply {
            text = "DISMISS"
            textSize = 22f
            setTextColor(android.graphics.Color.parseColor("#CC0000"))
            setBackgroundColor(android.graphics.Color.WHITE)
            val margin = (32 * resources.displayMetrics.density).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (72 * resources.displayMetrics.density).toInt()
            ).also { it.topMargin = margin }
            setOnClickListener { dismiss() }
        }

        val btnSnooze = Button(this).apply {
            text = if (snoozeDuration == 1) "SNOOZE 1 MIN" else "SNOOZE $snoozeDuration MIN"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#880000"))
            val margin = (12 * resources.displayMetrics.density).toInt()
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (60 * resources.displayMetrics.density).toInt()
            ).also { it.topMargin = margin }
            setOnClickListener { snooze(snoozeDuration) }
        }

        layout.addView(title)
        layout.addView(timeView)
        layout.addView(btnDismiss)
        layout.addView(btnSnooze)
        setContentView(layout)
    }

    private fun dismiss() {
        stopAlertService()
        finish()
    }

    private fun snooze(minutes: Int = 5) {
        stopAlertService()
        val alertId = intent.getLongExtra(EXTRA_ALERT_ID, -1L)
        val alertName = intent.getStringExtra(EXTRA_ALERT_NAME) ?: ""
        if (alertId != -1L) {
            val pi = android.app.PendingIntent.getService(
                this,
                (alertId + 500).toInt(),
                Intent(this, AlertService::class.java).apply {
                    putExtra(AlertService.EXTRA_ALERT_ID, alertId)
                    putExtra(AlertService.EXTRA_ALERT_NAME, alertName)
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val am = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            am.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + minutes * 60 * 1000L,
                pi
            )
        }
        finish()
    }

    private fun stopAlertService() {
        val intent = Intent(this, AlertService::class.java).apply {
            action = AlertService.ACTION_DISMISS
        }
        startService(intent)
    }

    companion object {
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_ALERT_NAME = "alert_name"
    }
}
