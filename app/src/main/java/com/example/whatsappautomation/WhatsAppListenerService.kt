package com.example.whatsappautomation

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.whatsappautomation.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhatsAppListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 1. Check if it's from WhatsApp or WhatsApp Business
        val packageName = sbn.packageName
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") return

        // 2. Ignore group summaries or ongoing notifications
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = sbn.notification.extras
        val senderName = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
        
        // 3. Extract message text
        var messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        // If it's a grouped message, EXTRA_TEXT might be null, try to get the last line
        if (messageText.isBlank()) {
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!textLines.isNullOrEmpty()) {
                messageText = textLines.last().toString()
            }
        }

        if (messageText.isBlank()) return

        // 4. Check if the app is ON
        // For simplicity, we'll assume it's ON, but in a real app, you'd check a SharedPreference

        // 5. Process the message through our engine
        scope.launch {
            val replyText = processIncomingMessage(senderName, messageText)
            
            if (!replyText.isNullOrBlank()) {
                // 6. Find the RemoteInput Action (The "Reply" button in the notification)
                val remoteInputAction = sbn.notification.actions?.find { it.remoteInputs != null }
                
                if (remoteInputAction != null) {
                    val remoteInput = remoteInputAction.remoteInputs[0]
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(remoteInput.resultKey, replyText)
                    
                    RemoteInput.addResultsToIntent(remoteInputAction.remoteInputs, intent, bundle)
                    
                    withContext(Dispatchers.Main) {
                        try {
                            remoteInputAction.actionIntent.send(applicationContext, 0, intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private suspend fun processIncomingMessage(sender: String, message: String): String? {
        val ruleDao = database.ruleDao()
        val contactDao = database.contactDao()
        
        // 1. Handle Lead Capture State Machine
        when (BotStateManager.currentLeadState) {
            LeadCaptureState.AWAITING_NAME -> {
                // Ensure this message is from the same person we asked
                if (BotStateManager.awaitingNameSender == sender) {
                    BotStateManager.tempName = RuleEngine.extractName(message)
                    BotStateManager.currentLeadState = LeadCaptureState.AWAITING_CONFIRMATION
                    return BotStateManager.activeRule?.confirmQuestion?.replace("{name}", BotStateManager.tempName)
                }
            }
            LeadCaptureState.AWAITING_CONFIRMATION -> {
                if (BotStateManager.awaitingNameSender == sender) {
                    val lowerMsg = message.trim().lowercase()
                    if (lowerMsg == "yes" || lowerMsg == "y") {
                        // Save Contact
                        val newContact = com.example.whatsappautomation.data.Contact(
                            phoneNumber = sender, 
                            name = BotStateManager.tempName, 
                            sourceApp = "WhatsApp",
                            tags = BotStateManager.activeRule?.assignTag ?: ""
                        )
                        contactDao.insertContact(newContact)
                        
                        // (Optional) Save to Phone Contacts here using PhoneContactManager
                        
                        val successMsg = BotStateManager.activeRule?.successMessage?.replace("{name}", BotStateManager.tempName) ?: "Saved!"
                        
                        // Activate Submenu if it exists
                        val allRules = ruleDao.getAllRulesList() // Need a synchronous or list-based getter
                        // For simplicity, we skip submenu routing here and just reset state
                        BotStateManager.reset()
                        return successMsg
                    } else if (lowerMsg == "no" || lowerMsg == "n") {
                        BotStateManager.currentLeadState = LeadCaptureState.AWAITING_NAME
                        BotStateManager.tempName = ""
                        return BotStateManager.activeRule?.retryQuestion ?: "What's your name again?"
                    } else {
                        return "Please reply YES or NO."
                    }
                }
            }
            LeadCaptureState.IDLE -> { /* Proceed to normal rules */ }
        }

        // 2. Normal Rule Matching
        val allRules = ruleDao.getAllRulesSync() // We need a synchronous getter for the service
        val topLevelRules = allRules.filter { it.parentRuleId == null && it.isActive }
        
        val matchedRule = RuleEngine.findMatchingRule(message, topLevelRules)
        
        return if (matchedRule != null) {
            if (matchedRule.askForName) {
                BotStateManager.currentLeadState = LeadCaptureState.AWAITING_NAME
                BotStateManager.activeRule = matchedRule
                BotStateManager.awaitingNameSender = sender
                return matchedRule.promptQuestion
            }
            RuleEngine.getReplyText(matchedRule, number = sender)
        } else {
            null // Stay silent
        }
    }
}
