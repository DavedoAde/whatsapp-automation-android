package com.example.whatsappautomation

import android.app.Application
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

    fun addRule(name: String, keyword: String, reply: String, actionType: String) {
        viewModelScope.launch {
            val newRule = AutomationRule(
                name = name,
                triggerKeyword = keyword,
                replyMessage = reply,
                isActive = true,
                priority = 1,
                actionType = actionType
            )
            ruleDao.insertRule(newRule)
        }
    }

    fun updateRule(updatedRule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.insertRule(updatedRule) 
        }
    }

    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.deleteRuleById(rule.id)
        }
    }

    // --- SIMULATOR & CONVERSATION LOGIC ---
    private var currentCaptureState = CaptureState.IDLE
    private var currentTempName: String? = null
    private var currentSenderId = "+1234567890" 

    fun simulateMessage(message: String): String {
        // 1. If we are already in a conversation flow, process it
        if (currentCaptureState != CaptureState.IDLE) {
            val flowResponse = ConversationManager.processMessage(message, currentCaptureState, currentTempName)
            
            currentCaptureState = flowResponse.nextState
            currentTempName = flowResponse.tempName

            if (flowResponse.saveContact && currentTempName != null) {
                viewModelScope.launch {
                    val newContact = Contact(
                        phoneNumber = currentSenderId,
                        name = currentTempName!!,
                        sourceApp = "WhatsApp"
                    )
                    contactDao.insertContact(newContact)
                }
            }
            return flowResponse.reply
        }

        // 2. If not in a flow, check the user's rules
        val matchedRule = RuleEngine.findMatchingRule(message, allRules.value)
        
        return if (matchedRule != null) {
            if (matchedRule.actionType == "CAPTURE_CONTACT") {
                // Trigger the flow!
                currentCaptureState = CaptureState.AWAITING_NAME
                // Use the rule's reply as the welcome message, or a default one
                matchedRule.replyMessage.ifBlank { "Welcome! What should I call you?" }
            } else {
                // Standard auto-reply
                matchedRule.replyMessage
            }
        } else {
            "No rule matched. (No reply sent)"
        }
    }
}
