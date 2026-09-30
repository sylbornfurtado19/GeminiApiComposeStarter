package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatSession
import kotlinx.coroutines.flow.Flow

/** Interface abstraction for Gemini API text generation and local chat persistence. */
interface GeminiRepository {
    fun getChatHistory(conversationId: String): Flow<List<ChatMessage>>
    fun getChatSessions(): Flow<List<ChatSession>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun clearChatHistory()
    suspend fun generateText(prompt: String): Result<String>
}
