package com.al3rted.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class AlertRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("al3rted_alerts", Context.MODE_PRIVATE)

    private val _alerts = MutableStateFlow(loadAlerts())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private fun loadAlerts(): List<Alert> {
        val json = prefs.getString(KEY_ALERTS, "[]") ?: "[]"
        val arr = JSONArray(json)
        return (0 until arr.length()).map { Alert.fromJson(arr.getJSONObject(it)) }
    }

    private fun saveAlerts(list: List<Alert>) {
        val arr = JSONArray(list.map { it.toJson() })
        prefs.edit().putString(KEY_ALERTS, arr.toString()).apply()
        _alerts.value = list
    }

    fun upsert(alert: Alert) {
        val current = _alerts.value.toMutableList()
        val idx = current.indexOfFirst { it.id == alert.id }
        if (idx >= 0) current[idx] = alert else current.add(alert)
        saveAlerts(current)
    }

    fun delete(id: Long) {
        saveAlerts(_alerts.value.filter { it.id != id })
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        val alert = _alerts.value.find { it.id == id } ?: return
        upsert(alert.copy(enabled = enabled))
    }

    companion object {
        private const val KEY_ALERTS = "alerts"

        @Volatile
        private var instance: AlertRepository? = null

        fun get(context: Context): AlertRepository =
            instance ?: synchronized(this) {
                instance ?: AlertRepository(context.applicationContext).also { instance = it }
            }
    }
}
