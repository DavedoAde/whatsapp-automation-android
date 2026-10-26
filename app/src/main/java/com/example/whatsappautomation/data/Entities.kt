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
    val leadStatus: String = "New"
)

@Entity(tableName = "rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerKeyword: String,
    val replyMessage: String,
    val isActive: Boolean = true,
    val priority: Int = 1,
    val actionType: String = "AUTO_REPLY" // Can be "AUTO_REPLY" or "CAPTURE_CONTACT"
)
