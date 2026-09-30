package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Interface abstraction for Gemini API text generation and local chat persistence. */
interface GeminiRepository {
    fun getChatHistory(): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun clearChatHistory()
    suspend fun generateText(prompt: String): Result<String>
}
