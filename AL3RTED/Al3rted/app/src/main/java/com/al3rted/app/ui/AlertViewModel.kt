package com.al3rted.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.al3rted.app.data.Alert
import com.al3rted.app.data.AlertRepository
import com.al3rted.app.utils.AlarmScheduler
import kotlinx.coroutines.flow.StateFlow

class AlertViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AlertRepository.get(app)
    val alerts: StateFlow<List<Alert>> = repo.alerts

    fun save(alert: Alert) {
        repo.upsert(alert)
        AlarmScheduler.schedule(app, alert)
    }

    fun delete(alert: Alert) {
        AlarmScheduler.cancel(app, alert)
        repo.delete(alert.id)
    }

    fun toggle(alert: Alert, enabled: Boolean) {
        val updated = alert.copy(enabled = enabled)
        repo.upsert(updated)
        if (enabled) AlarmScheduler.schedule(app, updated)
        else AlarmScheduler.cancel(app, updated)
    }

    private val app get() = getApplication<Application>()
}
