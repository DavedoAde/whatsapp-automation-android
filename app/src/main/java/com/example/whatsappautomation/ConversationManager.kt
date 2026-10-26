package com.example.whatsappautomation

enum class CaptureState {
    IDLE,
    AWAITING_NAME,
    AWAITING_CONFIRMATION
}

data class ConversationResponse(
    val reply: String,
    val nextState: CaptureState,
    val tempName: String? = null,
    val saveContact: Boolean = false
)

object ConversationManager {
    
    fun processMessage(
        message: String, 
        currentState: CaptureState, 
        currentTempName: String?
    ): ConversationResponse {
        
        val lowerMsg = message.trim().lowercase()

        return when (currentState) {
            CaptureState.IDLE -> {
                // If we are idle, we don't handle messages here anymore. The ViewModel handles it.
                ConversationResponse(reply = "", nextState = CaptureState.IDLE)
            }

            CaptureState.AWAITING_NAME -> {
                val cleanName = extractName(message)
                ConversationResponse(
                    reply = "Nice to meet you, $cleanName. Is that your correct name? Reply YES to confirm or NO to change it.",
                    nextState = CaptureState.AWAITING_CONFIRMATION,
                    tempName = cleanName
                )
            }

            CaptureState.AWAITING_CONFIRMATION -> {
                if (lowerMsg == "yes" || lowerMsg == "y") {
                    ConversationResponse(
                        reply = "Thank you, ${currentTempName}! Your details have been saved.",
                        nextState = CaptureState.IDLE,
                        saveContact = true,
                        tempName = currentTempName // FIXED: Pass the name back so it saves
                    )
                } else if (lowerMsg == "no" || lowerMsg == "n") {
                    ConversationResponse(
                        reply = "No problem. What is your correct name?",
                        nextState = CaptureState.AWAITING_NAME,
                        tempName = null
                    )
                } else {
                    ConversationResponse(
                        reply = "Sorry, I didn't catch that. Please reply YES to confirm or NO to change it.",
                        nextState = CaptureState.AWAITING_CONFIRMATION,
                        tempName = currentTempName
                    )
                }
            }
        }
    }

    // Smart name extractor (removes "My name is", "I am", etc.)
    private fun extractName(input: String): String {
        val lower = input.trim().lowercase()
        val prefixes = listOf("my name is ", "i am ", "im ", "call me ", "it's ", "its ")
        var name = input.trim()
        
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                name = name.substring(prefix.length).trim()
                break
            }
        }
        // Capitalize first letter
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
