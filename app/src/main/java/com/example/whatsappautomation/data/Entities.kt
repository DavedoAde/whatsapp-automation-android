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
    val tags: String = "" // e.g., "TikTok, Facebook"
)

@Entity(tableName = "rules")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerKeyword: String,
    val replyMessage: String, // Pipe-separated string for multiple replies
    val isActive: Boolean = true,
    val priority: Int = 1,
    
    // Card 1: Received Message
    val matchType: String = "SIMILARITY", // EXACT, SIMILARITY, PATTERN, REGEX, WELCOME
    val targetChat: String = "ALL", // ALL, INDIVIDUALS, GROUPS
    
    // Card 2 & 3: Replies
    val replyMode: String = "SINGLE", // SINGLE, ALL, RANDOM
    
    // Card 4: Save Lead Machine
    val askForName: Boolean = false,
    val promptQuestion: String = "What's your name?",
    val confirmQuestion: String = "Is your name {name}? Reply YES or NO.",
    val retryQuestion: String = "No problem. What's your name again?",
    val successMessage: String = "Thank you {name}! Your details have been saved.",
    val assignTag: String = "", 
    
    // Card 5: Conditions
    val requireScreenOff: Boolean = false,
    val requireCharging: Boolean = false,
    val requireDnd: Boolean = false,
    
    // Submenus
    val parentRuleId: Long? = null 
)
