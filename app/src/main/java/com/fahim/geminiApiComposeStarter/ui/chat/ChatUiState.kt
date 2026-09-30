package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import java.util.UUID

enum class Participant {
    USER,
    MODEL,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "default_conversation",
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

data class ChatSession(
    val id: String,
    val title: String,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Immutable UI state for the Gemini chat flow. */
data class ChatUiState(
    val activeConversationId: String = UUID.randomUUID().toString(),
    val conversations: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val response: String = "",
)

enum class PromptError {
    EMPTY,
}
