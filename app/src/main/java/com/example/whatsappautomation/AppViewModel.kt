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

    // Now we get the list of contacts for the Contacts Screen
    val allContacts = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalContactsCount = contactDao.getContactCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allRules = ruleDao.getAllRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(name: String, keyword: String, reply: String) {
        viewModelScope.launch {
            val newRule = AutomationRule(
                name = name,
                triggerKeyword = keyword,
                replyMessage = reply,
                isActive = true,
                priority = 1
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
    private var currentSenderId = "+1234567890" // Mock sender ID for simulation

    fun simulateMessage(message: String): String {
        // 1. Check if we are in a contact capture flow
        val flowResponse = ConversationManager.processMessage(message, currentCaptureState, currentTempName)
        
        if (flowResponse.reply.isNotEmpty()) {
            // Update the state for the next message
            currentCaptureState = flowResponse.nextState
            currentTempName = flowResponse.tempName

            // If the flow says "Save this contact", write to database
            if (flowResponse.saveContact && currentTempName != null) {
                viewModelScope.launch {
                    val newContact = Contact(
                        phoneNumber = currentSenderId,
                        name = currentTempName!!,
                        sourceApp = "WhatsApp",
                        isConfirmed = true,
                        leadStatus = "New"
                    )
                    contactDao.insertContact(newContact)
                }
            }
            return flowResponse.reply
        }

        // 2. If not in a flow, check standard rules
        val currentRules = allRules.value
        val matchedRule = RuleEngine.findMatchingRule(message, currentRules)
        
        return if (matchedRule != null) {
            matchedRule.replyMessage
        } else {
            "No rule matched. (No reply sent)"
        }
    }
}
