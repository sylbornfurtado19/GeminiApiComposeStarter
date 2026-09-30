package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import java.util.UUID

enum class Participant {
    USER,
    MODEL,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

/** Immutable UI state for the Gemini chat flow. */
data class ChatUiState(
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
