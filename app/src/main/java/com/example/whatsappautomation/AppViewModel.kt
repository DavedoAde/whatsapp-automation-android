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

    fun saveRule(rule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.insertRule(rule)
        }
    }

    fun deleteRule(rule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.deleteRuleById(rule.id)
        }
    }

    // --- SIMULATION STATE ---
    private var currentSimulatedMenuId: Long? = null
    private var currentSenderId = "+1234567890" 
    
    private var isAwaitingContactName = false 
    private var awaitingRule: AutomationRule? = null // Store the rule so we can use its thank you message

    fun simulateMessage(message: String): String {
        
        // 1. If we are waiting for a name
        if (isAwaitingContactName) {
            val extractedName = RuleEngine.extractName(message)
            
            viewModelScope.launch {
                val newContact = Contact(phoneNumber = currentSenderId, name = extractedName, sourceApp = "WhatsApp")
                contactDao.insertContact(newContact)
                
                val context = getApplication<Application>()
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    PhoneContactManager.saveContactToPhone(context, extractedName, currentSenderId)
                }
            }
            
            isAwaitingContactName = false 
            val thankYou = awaitingRule?.thankYouReply ?: "Thank you, {name}!"
            awaitingRule = null
            return thankYou.replace("{name}", extractedName)
        }

        val allRulesList = allRules.value

        // 2. Submenu Routing
        if (currentSimulatedMenuId != null) {
            val childRules = allRulesList.filter { it.parentRuleId == currentSimulatedMenuId && it.isActive }
            val matchedChild = RuleEngine.findMatchingRule(message, childRules)

            if (matchedChild != null) {
                val hasSubMenu = allRulesList.any { it.parentRuleId == matchedChild.id }
                currentSimulatedMenuId = if (hasSubMenu) matchedChild.id else null
                
                if (matchedChild.askForName) {
                    isAwaitingContactName = true
                    awaitingRule = matchedChild
                }
                return RuleEngine.getReplyText(matchedChild, number = currentSenderId)
            } else {
                val parentMenu = allRulesList.find { it.id == currentSimulatedMenuId }
                val parentText = if (parentMenu != null) RuleEngine.getReplyText(parentMenu, number = currentSenderId) else ""
                return "Invalid option. Please choose from the menu:\n\n$parentText"
            }
        }

        // 3. Standard Top-Level
        val topLevelRules = allRulesList.filter { it.parentRuleId == null }
        val matchedRule = RuleEngine.findMatchingRule(message, topLevelRules)
        
        return if (matchedRule != null) {
            val hasSubMenu = allRulesList.any { it.parentRuleId == matchedRule.id }
            currentSimulatedMenuId = if (hasSubMenu) matchedRule.id else null

            if (matchedRule.askForName) {
                isAwaitingContactName = true
                awaitingRule = matchedRule
                return matchedRule.promptQuestion // Return the prompt immediately
            }
            RuleEngine.getReplyText(matchedRule, number = currentSenderId)
        } else {
            "No rule matched. (No reply sent)"
        }
    }
}
