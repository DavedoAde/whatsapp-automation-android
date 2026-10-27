package com.example.whatsappautomation

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "whatsapp_automation_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_BATTERY_IGNORED = "battery_ignored"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    // NEW: Remembers if the user chose to ignore the battery warning
    fun isBatteryIgnored(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_IGNORED, false)
    }

    fun setBatteryIgnored(context: Context, ignored: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_IGNORED, ignored).apply()
    }
}
