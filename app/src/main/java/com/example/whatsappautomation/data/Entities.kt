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
    
    // Advanced Match Types: EXACT, SIMILARITY, PATTERN, REGEX
    val matchType: String = "SIMILARITY", 
    
    // Action Type: AUTO_REPLY, CAPTURE_CONTACT
    val actionType: String = "AUTO_REPLY", 
    
    // Reply Mode: SINGLE, ALL, RANDOM
    val replyMode: String = "SINGLE", 
    
    val delaySeconds: Int = 0,
    val recipientType: String = "BOTH", 
    val requireScreenOff: Boolean = false,
    val requireCharging: Boolean = false,
    val requireDnd: Boolean = false,
    
    // Links a submenu to its parent rule
    val parentRuleId: Long? = null 
)
