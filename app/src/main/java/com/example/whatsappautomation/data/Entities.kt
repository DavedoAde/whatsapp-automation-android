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
    
    // Card 1: Received Message
    val matchType: String = "SIMILARITY", // EXACT, SIMILARITY, PATTERN, REGEX, WELCOME
    val targetChat: String = "ALL", // ALL, INDIVIDUALS, GROUPS, SPECIFIC
    
    // Card 3: Multiple replies
    val replyMode: String = "SINGLE", // SINGLE (1), ALL, RANDOM
    
    // Card 4: Save Lead
    val askForName: Boolean = false,
    val promptQuestion: String = "What's your name?",
    val thankYouReply: String = "Nice to meet you, {name}!",
    
    // Card 5: Conditions
    val requireScreenOff: Boolean = false,
    val requireCharging: Boolean = false,
    val requireDnd: Boolean = false,
    
    // Submenus
    val parentRuleId: Long? = null 
)
