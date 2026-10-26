package com.example.whatsappautomation

enum class CaptureState {
    IDLE,
    AWAITING_NAME,
    AWAITING_CONFIRMATION
}

data class ConversationResponse(
    val reply: String,
    val nextState: CaptureState,
    val tempName: String? = null, // Holds the name while waiting for YES/NO
    val saveContact: Boolean = false // Tells the ViewModel to save to DB
)

object ConversationManager {
    
    // The trigger phrase that starts the flow
    private const val TRIGGER_PHRASE = "join the free class"

    fun processMessage(
        message: String, 
        currentState: CaptureState, 
        currentTempName: String?
    ): ConversationResponse {
        
        val lowerMsg = message.trim().lowercase()

        return when (currentState) {
            CaptureState.IDLE -> {
                // Check if the message contains the trigger phrase
                if (lowerMsg.contains(TRIGGER_PHRASE)) {
                    ConversationResponse(
                        reply = "Welcome! What should I call you?",
                        nextState = CaptureState.AWAITING_NAME
                    )
                } else {
                    // Return an empty reply to signify no conversation flow triggered
                    ConversationResponse(reply = "", nextState = CaptureState.IDLE)
                }
            }

            CaptureState.AWAITING_NAME -> {
                // Capitalize the first letter of their name for a nice touch
                val formattedName = message.trim().replaceFirstChar { it.uppercase() }
                ConversationResponse(
                    reply = "Nice to meet you, $formattedName. Is that your correct name? Reply YES to confirm or NO to change it.",
                    nextState = CaptureState.AWAITING_CONFIRMATION,
                    tempName = formattedName
                )
            }

            CaptureState.AWAITING_CONFIRMATION -> {
                if (lowerMsg == "yes" || lowerMsg == "y") {
                    ConversationResponse(
                        reply = "Thank you, ${currentTempName}! Your details have been saved.",
                        nextState = CaptureState.IDLE,
                        saveContact = true
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
}
