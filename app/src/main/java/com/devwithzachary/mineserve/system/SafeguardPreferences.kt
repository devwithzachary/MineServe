package com.devwithzachary.mineserve.system

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SafeguardConfig(
    val batteryAutoStopEnabled: Boolean = false,
    val minBatteryPercent: Int = 15,
    val thermalAutoStopEnabled: Boolean = false,
    val maxBatteryTempC: Int = 45
)

class SafeguardPreferences(context: Context) {
    private val prefs: SharedPreferences =
        (context.applicationContext ?: context).getSharedPreferences("mineserve_safeguard_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(load())
    val config: StateFlow<SafeguardConfig> = _config.asStateFlow()

    fun load(): SafeguardConfig {
        return SafeguardConfig(
            batteryAutoStopEnabled = prefs.getBoolean("battery_auto_stop", false),
            minBatteryPercent = prefs.getInt("min_battery_pct", 15).coerceIn(5, 50),
            thermalAutoStopEnabled = prefs.getBoolean("thermal_auto_stop", false),
            maxBatteryTempC = prefs.getInt("max_temp_c", 45).coerceIn(38, 55)
        )
    }

    fun save(updated: SafeguardConfig) {
        prefs.edit()
            .putBoolean("battery_auto_stop", updated.batteryAutoStopEnabled)
            .putInt("min_battery_pct", updated.minBatteryPercent)
            .putBoolean("thermal_auto_stop", updated.thermalAutoStopEnabled)
            .putInt("max_temp_c", updated.maxBatteryTempC)
            .apply()
        _config.value = updated
    }
}

