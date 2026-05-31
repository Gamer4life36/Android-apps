package com.al3rted.app.data

import org.json.JSONArray
import org.json.JSONObject

data class Alert(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",
    val hourOfDay: Int = 8,
    val minute: Int = 0,
    val days: Set<Int> = emptySet(), // Calendar.MONDAY..SUNDAY
    val enabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("hourOfDay", hourOfDay)
        put("minute", minute)
        put("days", JSONArray(days.toList()))
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(json: JSONObject): Alert {
            val daysArr = json.getJSONArray("days")
            val days = (0 until daysArr.length()).map { daysArr.getInt(it) }.toSet()
            return Alert(
                id = json.getLong("id"),
                name = json.getString("name"),
                hourOfDay = json.getInt("hourOfDay"),
                minute = json.getInt("minute"),
                days = days,
                enabled = json.getBoolean("enabled")
            )
        }
    }
}
