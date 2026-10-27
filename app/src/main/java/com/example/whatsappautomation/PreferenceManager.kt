package com.example.whatsappautomation

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "whatsapp_automation_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_AUTOMATION_ACTIVE = "is_automation_active"
    private const val KEY_WA_ORIGINAL = "wa_original_enabled"
    private const val KEY_WA_BUSINESS = "wa_business_enabled"
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

    fun isAutomationActive(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTOMATION_ACTIVE, false)
    }

    fun setAutomationActive(context: Context, isActive: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTOMATION_ACTIVE, isActive).apply()
    }

    fun isWaOriginalEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WA_ORIGINAL, true)
    }

    fun setWaOriginalEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WA_ORIGINAL, enabled).apply()
    }

    fun isWaBusinessEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_WA_BUSINESS, true)
    }

    fun setWaBusinessEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_WA_BUSINESS, enabled).apply()
    }

    // --- BATTERY IGNORE LOGIC ---
    fun isBatteryIgnored(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BATTERY_IGNORED, false)
    }

    fun setBatteryIgnored(context: Context, ignored: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BATTERY_IGNORED, ignored).apply()
    }
}
