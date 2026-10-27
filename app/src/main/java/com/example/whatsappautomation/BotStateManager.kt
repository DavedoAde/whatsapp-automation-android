package com.example.whatsappautomation

import com.example.whatsappautomation.data.AutomationRule

// This is the ONLY place LeadCaptureState is defined now
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

    fun reset() {
        currentLeadState = LeadCaptureState.IDLE
        currentMenuId = null
        tempName = ""
        activeRule = null
        awaitingNameSender = null
    }
}
