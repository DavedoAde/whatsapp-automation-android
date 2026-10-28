package com.example.whatsappautomation.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val name: String,
    val sourceApp: String = "WhatsApp",
    val captureDate: Long = System.currentTimeMillis(),
    val isConfirmed: Boolean = true,
    val leadStatus: String = "New",
    val tags: String = "",
    // NEW: Pause and Ignore fields
    val pausedUntil: Long = 0L,
    val isIgnored: Boolean = false
)

@Entity(tableName = "rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerKeyword: String,
    val replyMessage: String, 
    val isActive: Boolean = true,
    val priority: Int = 1,
    val matchType: String = "SIMILARITY", 
    val targetChat: String = "ALL", 
    val replyMode: String = "SINGLE", 
    val delayMin: Int = 0,
    val delayMax: Int = 0,
    val askForName: Boolean = false,
    val promptQuestion: String = "What's your name?",
    val confirmQuestion: String = "Is your name {name}? Reply YES or NO.",
    val retryQuestion: String = "No problem. What's your name again?",
    val successMessage: String = "Thank you {name}! Your details have been saved.",
    val assignTag: String = "", 
    val requireScreenOff: Boolean = false,
    val requireCharging: Boolean = false,
    val requireDnd: Boolean = false,
    val parentRuleId: Long? = null 
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String,
    val incomingMessage: String,
    val actionTaken: String,
    val replyText: String = "",
    val isError: Boolean = false
)
