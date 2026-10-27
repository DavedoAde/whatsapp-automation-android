package com.example.whatsappautomation

import com.example.whatsappautomation.data.AutomationRule

enum class LeadCaptureState {
    IDLE,
    AWAITING_NAME,
    AWAITING_CONFIRMATION
}

object BotStateManager {
    var currentLeadState = LeadCaptureState.IDLE
    var currentMenuId: Long? = null
    var tempName: String = ""
    var activeRule: AutomationRule? = null
    var awaitingNameSender: String? = null 

    // --- LOOP PREVENTION MEMORY ---
    // Stores recently sent messages (sender:message) to ignore when they come back as notifications
    val recentlySentMessages = mutableSetOf<String>()
    
    // Tracks the last time we replied to a specific sender to enforce a cooldown
    val lastReplyTimeMap = mutableMapOf<String, Long>()
    
    // Track the last processed notification timestamp
    var lastNotificationTime: Long = 0L

    fun reset() {
        currentLeadState = LeadCaptureState.IDLE
        currentMenuId = null
        tempName = ""
        activeRule = null
        awaitingNameSender = null
    }
    
    fun recordSentMessage(sender: String, message: String) {
        val key = "$sender:$message"
        recentlySentMessages.add(key)
        lastReplyTimeMap[sender] = System.currentTimeMillis()
        
        // Keep memory clean (max 50 messages)
        if (recentlySentMessages.size > 50) {
            val iterator = recentlySentMessages.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }
}
