package com.example.whatsappautomation

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ConditionChecker {

    fun isScreenOff(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !powerManager.isInteractive
    }

    fun isCharging(context: Context): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun isDndActive(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }
    }

    // NEW: Check if the current time is within the allowed time window
    fun isWithinTimeRange(startStr: String, endStr: String): Boolean {
        if (startStr.isBlank() || endStr.isBlank()) return true // No time restriction
        
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val nowString = format.format(Date())
            
            // Ensure format is HH:mm (e.g., "9:00" becomes "09:00")
            val start = startStr.padStart(5, '0')
            val end = endStr.padStart(5, '0')
            
            if (start <= end) {
                // Normal range (e.g., 09:00 to 17:00)
                nowString in start..end
            } else {
                // Overnight range (e.g., 22:00 to 06:00)
                nowString >= start || nowString <= end
            }
        } catch (e: Exception) {
            true // If the user typed the time incorrectly, default to allowing the reply
        }
    }
}
