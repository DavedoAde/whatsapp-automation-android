package com.example.whatsappautomation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.whatsappautomation.data.ActivityLog
import com.example.whatsappautomation.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WhatsAppListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var database: AppDatabase
    private val TAG = "WAListener"
    private val REPLY_CHANNEL_ID = "reply_alerts_channel"

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(applicationContext)
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        
        if (!PreferenceManager.isAutomationActive(applicationContext)) return
        
        // --- WA VERSION CHECK ---
        val waVersion = PreferenceManager.getWaVersion(applicationContext)
        val isMultiUser = PreferenceManager.isMultiUserEnabled(applicationContext)

        if (packageName == "com.whatsapp") {
            if (waVersion == "BUSINESS") return
        } else if (packageName == "com.whatsapp.w4b") {
            if (waVersion == "ORIGINAL") return
        } else {
            if (!isMultiUser) return 
        }
        // ------------------------
        
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((sbn.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = sbn.notification.extras
        val senderName = extras.getString(Notification.EXTRA_TITLE) ?: "Unknown"
        
        if (senderName.equals("Me", ignoreCase = true) || senderName.equals("You", ignoreCase = true)) return

        var messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (messageText.isBlank()) {
            val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!textLines.isNullOrEmpty()) {
                messageText = textLines.last().toString()
            }
        }

        if (messageText.isBlank()) return

        val notificationTime = sbn.postTime
        if (notificationTime <= BotStateManager.lastNotificationTime) return
        BotStateManager.lastNotificationTime = notificationTime

        val messageKey = "$senderName:$messageText"
        if (BotStateManager.recentlySentMessages.contains(messageKey)) return 

        val lastReply = BotStateManager.lastReplyTimeMap[senderName] ?: 0L
        if (System.currentTimeMillis() - lastReply < 3000) return

        scope.launch {
            val savedContact = database.contactDao().getContactByName(senderName)
            if (savedContact != null) {
                if (savedContact.isIgnored) return@launch
                if (savedContact.pausedUntil > System.currentTimeMillis()) return@launch
            }

            val result = processIncomingMessage(senderName, messageText)
            
            if (result != null && !result.replyText.isNullOrBlank()) {
                if (result.delayMax > 0) {
                    val min = result.delayMin
                    val max = result.delayMax
                    val randomDelaySeconds = if (max > min) Random.nextInt(min, max) else min
                    delay(randomDelaySeconds * 1000L) 
                }

                val remoteInputAction = sbn.notification.actions?.find { action ->
                    action.remoteInputs != null && action.remoteInputs.isNotEmpty()
                }
                
                if (remoteInputAction != null) {
                    val remoteInput = remoteInputAction.remoteInputs[0]
                    val intent = Intent()
                    val bundle = Bundle()
                    bundle.putCharSequence(remoteInput.resultKey, result.replyText)
                    
                    RemoteInput.addResultsToIntent(remoteInputAction.remoteInputs, intent, bundle)
                    
                    withContext(Dispatchers.Main) {
                        try {
                            remoteInputAction.actionIntent.send(applicationContext, 0, intent)
                            BotStateManager.recordSentMessage(senderName, result.replyText)
                            
                            // Check if the user wants notifications
                            if (PreferenceManager.isNotificationsEnabled(applicationContext)) {
                                showReplyNotification(senderName, result.replyText)
                            }

                            database.logDao().insertLog(
                                ActivityLog(
                                    senderName = senderName,
                                    incomingMessage = messageText,
                                    actionTaken = "Replied",
                                    replyText = result.replyText
                                )
                            )
                        } catch (e: Exception) {
                            database.logDao().insertLog(
                                ActivityLog(
                                    senderName = senderName,
                                    incomingMessage = messageText,
                                    actionTaken = "Error: Failed to send",
                                    isError = true
                                )
                            )
                        }
                    }
                }
            } else {
                database.logDao().insertLog(
                    ActivityLog(
                        senderName = senderName,
                        incomingMessage = messageText,
                        actionTaken = "No Rule Matched"
                    )
                )
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REPLY_CHANNEL_ID,
                "Bot Reply Alerts",
                NotificationManager.IMPORTANCE_LOW // LOW means it won't make a sound
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showReplyNotification(senderName: String, replyText: String) {
        val notification = NotificationCompat.Builder(this, REPLY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Replied to $senderName")
            .setContentText(replyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(replyText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(this)
        try {
            // Use a random ID so multiple notifications stack instead of overwriting
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing")
        }
    }

    data class ReplyResult(val replyText: String?, val delayMin: Int = 0, val delayMax: Int = 0)

    private suspend fun processIncomingMessage(sender: String, message: String): ReplyResult? {
        val ruleDao = database.ruleDao()
        val contactDao = database.contactDao()
        
        when (BotStateManager.currentLeadState) {
            LeadCaptureState.AWAITING_NAME -> {
                if (BotStateManager.awaitingNameSender == sender) {
                    BotStateManager.tempName = RuleEngine.extractName(message)
                    BotStateManager.currentLeadState = LeadCaptureState.AWAITING_CONFIRMATION
                    return ReplyResult(
                        BotStateManager.activeRule?.confirmQuestion?.replace("{name}", BotStateManager.tempName),
                        BotStateManager.activeRule?.delayMin ?: 0,
                        BotStateManager.activeRule?.delayMax ?: 0
                    )
                }
            }
            LeadCaptureState.AWAITING_CONFIRMATION -> {
                if (BotStateManager.awaitingNameSender == sender) {
                    val lowerMsg = message.trim().lowercase()
                    if (lowerMsg == "yes" || lowerMsg == "y") {
                        val newContact = com.example.whatsappautomation.data.Contact(
                            phoneNumber = sender, 
                            name = BotStateManager.tempName, 
                            sourceApp = "WhatsApp",
                            tags = BotStateManager.activeRule?.assignTag ?: ""
                        )
                        contactDao.insertContact(newContact)
                        
                        database.logDao().insertLog(
                            ActivityLog(
                                senderName = sender,
                                incomingMessage = message,
                                actionTaken = "Lead Saved",
                                replyText = "Name: ${BotStateManager.tempName}"
                            )
                        )

                        val successMsg = BotStateManager.activeRule?.successMessage?.replace("{name}", BotStateManager.tempName) ?: "Saved!"
                        
                        val allRules = ruleDao.getAllRulesSync()
                        val hasSubMenu = allRules.any { it.parentRuleId == BotStateManager.activeRule?.id }
                        if (hasSubMenu) {
                            BotStateManager.currentMenuId = BotStateManager.activeRule?.id
                        } else {
                            BotStateManager.currentMenuId = null
                        }
                        
                        val delayMin = BotStateManager.activeRule?.delayMin ?: 0
                        val delayMax = BotStateManager.activeRule?.delayMax ?: 0
                        
                        BotStateManager.currentLeadState = LeadCaptureState.IDLE
                        BotStateManager.activeRule = null
                        BotStateManager.awaitingNameSender = null
                        
                        return ReplyResult(successMsg, delayMin, delayMax)
                        
                    } else if (lowerMsg == "no" || lowerMsg == "n") {
                        BotStateManager.currentLeadState = LeadCaptureState.AWAITING_NAME
                        BotStateManager.tempName = ""
                        return ReplyResult(BotStateManager.activeRule?.retryQuestion ?: "What's your name again?")
                    } else {
                        return ReplyResult("Please reply YES or NO.")
                    }
                }
            }
            LeadCaptureState.IDLE -> { /* Proceed */ }
        }

        val allRules = ruleDao.getAllRulesSync()
        val topLevelRules = allRules.filter { it.parentRuleId == null && it.isActive }
        
        val matchedRule = RuleEngine.findMatchingRule(applicationContext, message, topLevelRules, checkConditions = true)
        
        return if (matchedRule != null) {
            if (matchedRule.askForName) {
                BotStateManager.currentLeadState = LeadCaptureState.AWAITING_NAME
                BotStateManager.activeRule = matchedRule
                BotStateManager.awaitingNameSender = sender
                return ReplyResult(matchedRule.promptQuestion, matchedRule.delayMin, matchedRule.delayMax)
            }
            ReplyResult(RuleEngine.getReplyText(matchedRule, number = sender), matchedRule.delayMin, matchedRule.delayMax)
        } else {
            null 
        }
    }
}
