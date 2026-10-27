package com.example.whatsappautomation

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappautomation.data.AppDatabase
import com.example.whatsappautomation.data.AutomationRule
import com.example.whatsappautomation.data.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LeadCaptureState {
    IDLE,
    AWAITING_NAME,
    AWAITING_CONFIRMATION
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val contactDao = database.contactDao()
    private val ruleDao = database.ruleDao()

    private val _isAutomationActive = MutableStateFlow(true)
    val isAutomationActive: StateFlow<Boolean> = _isAutomationActive

    fun toggleAutomation(isActive: Boolean) {
        _isAutomationActive.value = isActive
    }

    val activeRulesCount = ruleDao.getActiveRuleCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val allContacts = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalContactsCount = contactDao.getContactCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val allRules = ruleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRule(rule: AutomationRule) {
        viewModelScope.launch { ruleDao.insertRule(rule) }
    }
    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch { ruleDao.deleteRuleById(rule.id) }
    }

    // --- LEAD CAPTURE STATE MACHINE ---
    private var currentLeadState = LeadCaptureState.IDLE
    private var currentSimulatedMenuId: Long? = null
    private var tempName: String = ""
    private var activeRule: AutomationRule? = null
    private var currentSenderId = "+1234567890" 

    fun simulateMessage(message: String): String {
        // 1. Handle the Lead Capture State Machine
        when (currentLeadState) {
            LeadCaptureState.AWAITING_NAME -> {
                tempName = RuleEngine.extractName(message)
                currentLeadState = LeadCaptureState.AWAITING_CONFIRMATION
                return activeRule?.confirmQuestion?.replace("{name}", tempName) ?: "Is your name $tempName? Reply YES or NO."
            }
            
            LeadCaptureState.AWAITING_CONFIRMATION -> {
                val lowerMsg = message.trim().lowercase()
                if (lowerMsg == "yes" || lowerMsg == "y") {
                    // Save the contact!
                    viewModelScope.launch {
                        val newContact = Contact(
                            phoneNumber = currentSenderId, 
                            name = tempName, 
                            sourceApp = "WhatsApp",
                            tags = activeRule?.assignTag ?: ""
                        )
                        contactDao.insertContact(newContact)
                        
                        val context = getApplication<Application>()
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            PhoneContactManager.saveContactToPhone(context, tempName, currentSenderId)
                        }
                    }
                    
                    currentLeadState = LeadCaptureState.IDLE
                    val successMsg = activeRule?.successMessage?.replace("{name}", tempName) ?: "Thank you $tempName!"
                    
                    // If the rule has a submenu, move into it so the next message triggers the link
                    val allRulesList = allRules.value
                    val hasSubMenu = allRulesList.any { it.parentRuleId == activeRule?.id }
                    if (hasSubMenu) {
                        currentSimulatedMenuId = activeRule?.id
                    } else {
                        currentSimulatedMenuId = null
                    }
                    activeRule = null
                    return successMsg
                    
                } else if (lowerMsg == "no" || lowerMsg == "n") {
                    currentLeadState = LeadCaptureState.AWAITING_NAME
                    tempName = ""
                    return activeRule?.retryQuestion ?: "No problem. What's your name again?"
                } else {
                    return "Please reply YES or NO. Is your name $tempName?"
                }
            }
            LeadCaptureState.IDLE -> { /* Continue to normal rule matching */ }
        }

        // 2. If we are currently inside a submenu
        val allRulesList = allRules.value
        if (currentSimulatedMenuId != null) {
            val childRules = allRulesList.filter { it.parentRuleId == currentSimulatedMenuId && it.isActive }
            val matchedChild = RuleEngine.findMatchingRule(message, childRules)

            if (matchedChild != null) {
                val hasSubMenu = allRulesList.any { it.parentRuleId == matchedChild.id }
                currentSimulatedMenuId = if (hasSubMenu) matchedChild.id else null
                
                if (matchedChild.askForName) {
                    currentLeadState = LeadCaptureState.AWAITING_NAME
                    activeRule = matchedChild
                    return matchedChild.promptQuestion
                }
                return RuleEngine.getReplyText(matchedChild, number = currentSenderId)
            } else {
                return "" // Stay silent if invalid option in submenu
            }
        }

        // 3. Standard Top-Level Rules
        val topLevelRules = allRulesList.filter { it.parentRuleId == null }
        val matchedRule = RuleEngine.findMatchingRule(message, topLevelRules)
        
        return if (matchedRule != null) {
            val hasSubMenu = allRulesList.any { it.parentRuleId == matchedRule.id }
            currentSimulatedMenuId = if (hasSubMenu) matchedRule.id else null

            if (matchedRule.askForName) {
                currentLeadState = LeadCaptureState.AWAITING_NAME
                activeRule = matchedRule
                return matchedRule.promptQuestion
            }
            RuleRuleEngineReply(matchedRule)
        } else {
            "" // Stay completely silent
        }
    }

    private fun RuleRuleEngineReply(rule: AutomationRule): String {
         return RuleEngine.getReplyText(rule, number = currentSenderId)
    }
}
