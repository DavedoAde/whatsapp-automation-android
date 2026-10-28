package com.example.whatsappautomation

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappautomation.data.AppDatabase
import com.example.whatsappautomation.data.AutomationRule
import com.example.whatsappautomation.data.Contact
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class AppViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val contactDao = database.contactDao()
    private val ruleDao = database.ruleDao()
    private val logDao = database.logDao()

    private val _isAutomationActive = MutableStateFlow(PreferenceManager.isAutomationActive(application))
    val isAutomationActive: StateFlow<Boolean> = _isAutomationActive

    val allLogs = logDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearLogs() {
        viewModelScope.launch { logDao.clearAllLogs() }
    }

    fun toggleAutomation(isActive: Boolean) {
        _isAutomationActive.value = isActive
        PreferenceManager.setAutomationActive(getApplication(), isActive)
        
        val intent = Intent(getApplication(), ForegroundService::class.java)
        if (isActive) {
            ContextCompat.startForegroundService(getApplication(), intent)
        } else {
            getApplication<Application>().stopService(intent)
        }
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
    fun toggleRuleActiveState(rule: AutomationRule) {
        viewModelScope.launch {
            ruleDao.insertRule(rule.copy(isActive = !rule.isActive))
        }
    }

    // --- NEW: PAUSE AND IGNORE ---
    fun pauseContact(name: String, minutes: Int) {
        viewModelScope.launch {
            val pauseUntil = System.currentTimeMillis() + (minutes * 60 * 1000L)
            contactDao.pauseContact(name, pauseUntil)
        }
    }

    fun toggleIgnoreContact(name: String, isIgnored: Boolean) {
        viewModelScope.launch {
            contactDao.toggleIgnoreContact(name, isIgnored)
        }
    }

    private var currentSenderId = "+1234567890" 

    suspend fun simulateMessage(message: String): String {
        val allRulesList = allRules.value
        
        // ... (Simulation logic remains the same, no pause logic needed here because it's just a test) ...
        // Paste the simulateMessage function here EXACTLY as it is in your current AppViewModel.kt
        
        when (BotStateManager.currentLeadState) {
            LeadCaptureState.AWAITING_NAME -> {
                BotStateManager.tempName = RuleEngine.extractName(message)
                BotStateManager.currentLeadState = LeadCaptureState.AWAITING_CONFIRMATION
                return BotStateManager.activeRule?.confirmQuestion?.replace("{name}", BotStateManager.tempName) ?: "Is your name ${BotStateManager.tempName}? Reply YES or NO."
            }
            
            LeadCaptureState.AWAITING_CONFIRMATION -> {
                val lowerMsg = message.trim().lowercase()
                if (lowerMsg == "yes" || lowerMsg == "y") {
                    viewModelScope.launch {
                        val newContact = Contact(
                            phoneNumber = currentSenderId, 
                            name = BotStateManager.tempName, 
                            sourceApp = "WhatsApp",
                            tags = BotStateManager.activeRule?.assignTag ?: ""
                        )
                        contactDao.insertContact(newContact)
                    }
                    
                    val successMsg = BotStateManager.activeRule?.successMessage?.replace("{name}", BotStateManager.tempName) ?: "Saved!"
                    
                    val hasSubMenu = allRulesList.any { it.parentRuleId == BotStateManager.activeRule?.id }
                    if (hasSubMenu) {
                        BotStateManager.currentMenuId = BotStateManager.activeRule?.id
                    } else {
                        BotStateManager.currentMenuId = null
                    }
                    
                    BotStateManager.currentLeadState = LeadCaptureState.IDLE
                    BotStateManager.activeRule = null
                    return successMsg
                    
                } else if (lowerMsg == "no" || lowerMsg == "n") {
                    BotStateManager.currentLeadState = LeadCaptureState.AWAITING_NAME
                    BotStateManager.tempName = ""
                    return BotStateManager.activeRule?.retryQuestion ?: "No problem. What's your name again?"
                } else {
                    return "Please reply YES or NO. Is your name ${BotStateManager.tempName}?"
                }
            }
            LeadCaptureState.IDLE -> { /* Continue */ }
        }

        if (BotStateManager.currentMenuId != null) {
            val childRules = allRulesList.filter { it.parentRuleId == BotStateManager.currentMenuId && it.isActive }
            val matchedChild = RuleEngine.findMatchingRule(message, childRules)

            if (matchedChild != null) {
                if (matchedChild.delayMax > 0) {
                    val min = matchedChild.delayMin
                    val max = matchedChild.delayMax
                    val randomDelaySeconds = if (max > min) Random.nextInt(min, max) else min
                    delay(randomDelaySeconds * 1000L)
                }

                val hasSubMenu = allRulesList.any { it.parentRuleId == matchedChild.id }
                BotStateManager.currentMenuId = if (hasSubMenu) matchedChild.id else null
                
                if (matchedChild.askForName) {
                    BotStateManager.currentLeadState = LeadCaptureState.AWAITING_NAME
                    BotStateManager.activeRule = matchedChild
                    return matchedChild.promptQuestion
                }
                return RuleEngine.getReplyText(matchedChild, number = currentSenderId)
            } else {
                return ""
            }
        }

        val topLevelRules = allRulesList.filter { it.parentRuleId == null }
        val matchedRule = RuleEngine.findMatchingRule(message, topLevelRules)
        
        return if (matchedRule != null) {
            if (matchedRule.delayMax > 0) {
                val min = matchedRule.delayMin
                val max = matchedRule.delayMax
                val randomDelaySeconds = if (max > min) Random.nextInt(min, max) else min
                delay(randomDelaySeconds * 1000L)
            }

            val hasSubMenu = allRulesList.any { it.parentRuleId == matchedRule.id }
            BotStateManager.currentMenuId = if (hasSubMenu) matchedRule.id else null

            if (matchedRule.askForName) {
                BotStateManager.currentLeadState = LeadCaptureState.AWAITING_NAME
                BotStateManager.activeRule = matchedRule
                return matchedRule.promptQuestion
            }
            RuleEngine.getReplyText(matchedRule, number = currentSenderId)
        } else {
            "" 
        }
    }
}
