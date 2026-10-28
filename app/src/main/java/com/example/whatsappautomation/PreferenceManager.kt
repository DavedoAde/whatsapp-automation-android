package com.example.whatsappautomation

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "whatsapp_automation_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_AUTOMATION_ACTIVE = "is_automation_active"
    private const val KEY_BATTERY_IGNORED = "battery_ignored"
    
    // NEW KEYS
    private const val KEY_WA_VERSION = "wa_version"
    private const val KEY_MULTI_USER = "multi_user_enabled"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun isAutomationActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTOMATION_ACTIVE, false)
    }

    fun setAutomationActive(context: Context, isActive: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTOMATION_ACTIVE, isActive).apply()
    }

    fun isBatteryIgnored(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_IGNORED, false)
    }

    fun setBatteryIgnored(context: Context, ignored: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_IGNORED, ignored).apply()
    }

    // --- WA VERSION LOGIC ---
    // Returns "ANY", "ORIGINAL", or "BUSINESS"
    fun getWaVersion(context: Context): String {
        return getPrefs(context).getString(KEY_WA_VERSION, "ANY") ?: "ANY"
    }

    fun setWaVersion(context: Context, version: String) {
        getPrefs(context).edit().putString(KEY_WA_VERSION, version).apply()
    }

    // --- NEW TOGGLES ---
    fun isMultiUserEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MULTI_USER, false)
    }

    fun setMultiUserEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MULTI_USER, enabled).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
}
