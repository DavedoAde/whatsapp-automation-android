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

    private var currentCaptureState = CaptureState.IDLE
    private var currentTempName: String? = null
    private var currentSenderId = "+1234567890" 

    fun simulateMessage(message: String): String {
        if (currentCaptureState != CaptureState.IDLE) {
            val flowResponse = ConversationManager.processMessage(message, currentCaptureState, currentTempName)
            
            currentCaptureState = flowResponse.nextState
            currentTempName = flowResponse.tempName

            if (flowResponse.saveContact && currentTempName != null) {
                viewModelScope.launch {
                    // 1. Save to App Database
                    val newContact = Contact(
                        phoneNumber = currentSenderId,
                        name = currentTempName!!,
                        sourceApp = "WhatsApp"
                    )
                    contactDao.insertContact(newContact)
                    
                    // 2. Save to Phone Contacts (if permission granted)
                    val context = getApplication<Application>()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        PhoneContactManager.saveContactToPhone(context, currentTempName!!, currentSenderId)
                    }
                }
            }
            return flowResponse.reply
        }

        val matchedRule = RuleEngine.findMatchingRule(message, allRules.value)
        
        return if (matchedRule != null) {
            if (matchedRule.actionType == "CAPTURE_CONTACT") {
                currentCaptureState = CaptureState.AWAITING_NAME
                matchedRule.replyMessage.ifBlank { "Welcome! What should I call you?" }
            } else {
                matchedRule.replyMessage
            }
        } else {
            "No rule matched. (No reply sent)"
        }
    }
}
