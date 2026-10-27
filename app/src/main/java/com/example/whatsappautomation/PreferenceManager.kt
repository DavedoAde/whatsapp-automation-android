package com.example.whatsappautomation

import android.content.Context
import android.content.SharedPreferences

object PreferenceManager {
    private const val PREF_NAME = "whatsapp_automation_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
}
