package com.example.whatsappautomation

import com.example.whatsappautomation.data.AutomationRule
import org.json.JSONArray
import org.json.JSONObject

object RuleExporter {

    fun exportRulesToJson(rules: List<AutomationRule>): String {
        val jsonArray = JSONArray()
        for (rule in rules) {
            val jsonObject = JSONObject()
            // We don't export the ID so it doesn't conflict with existing rules on import
            jsonObject.put("name", rule.name)
            jsonObject.put("triggerKeyword", rule.triggerKeyword)
            jsonObject.put("replyMessage", rule.replyMessage)
            jsonObject.put("isActive", rule.isActive)
            jsonObject.put("priority", rule.priority)
            jsonObject.put("matchType", rule.matchType)
            jsonObject.put("targetChat", rule.targetChat)
            jsonObject.put("replyMode", rule.replyMode)
            jsonObject.put("delayMin", rule.delayMin)
            jsonObject.put("delayMax", rule.delayMax)
            jsonObject.put("askForName", rule.askForName)
            jsonObject.put("promptQuestion", rule.promptQuestion)
            jsonObject.put("confirmQuestion", rule.confirmQuestion)
            jsonObject.put("retryQuestion", rule.retryQuestion)
            jsonObject.put("successMessage", rule.successMessage)
            jsonObject.put("assignTag", rule.assignTag)
            jsonObject.put("label", rule.label)
            jsonObject.put("targetVersion", rule.targetVersion)
            jsonObject.put("requireScreenOff", rule.requireScreenOff)
            jsonObject.put("requireCharging", rule.requireCharging)
            jsonObject.put("requireDnd", rule.requireDnd)
            jsonObject.put("startTime", rule.startTime)
            jsonObject.put("endTime", rule.endTime)
            
            // Note: Submenus (parentRuleId) are tricky to export perfectly without keeping IDs.
            // For now, we export top-level rules.
            
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString(2) // 2 spaces for pretty formatting
    }

    fun importRulesFromJson(jsonString: String): List<AutomationRule> {
        val rulesList = mutableListOf<AutomationRule>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val rule = AutomationRule(
                    name = obj.optString("name", "Imported Rule"),
                    triggerKeyword = obj.optString("triggerKeyword", ""),
                    replyMessage = obj.optString("replyMessage", ""),
                    isActive = obj.optBoolean("isActive", true),
                    priority = obj.optInt("priority", 1),
                    matchType = obj.optString("matchType", "SIMILARITY"),
                    targetChat = obj.optString("targetChat", "ALL"),
                    replyMode = obj.optString("replyMode", "SINGLE"),
                    delayMin = obj.optInt("delayMin", 0),
                    delayMax = obj.optInt("delayMax", 0),
                    askForName = obj.optBoolean("askForName", false),
                    promptQuestion = obj.optString("promptQuestion", "What's your name?"),
                    confirmQuestion = obj.optString("confirmQuestion", "Is your name {name}? Reply YES or NO."),
                    retryQuestion = obj.optString("retryQuestion", "No problem. What's your name again?"),
                    successMessage = obj.optString("successMessage", "Thank you {name}! Your details have been saved."),
                    assignTag = obj.optString("assignTag", ""),
                    label = obj.optString("label", ""),
                    targetVersion = obj.optString("targetVersion", "ANY"),
                    requireScreenOff = obj.optBoolean("requireScreenOff", false),
                    requireCharging = obj.optBoolean("requireCharging", false),
                    requireDnd = obj.optBoolean("requireDnd", false),
                    startTime = obj.optString("startTime", ""),
                    endTime = obj.optString("endTime", "")
                )
                rulesList.add(rule)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rulesList
    }
}
