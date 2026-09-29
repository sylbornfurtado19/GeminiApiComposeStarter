package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: GeminiRepository,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var lastUserPrompt: String? = null

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, promptError = null) }
    }

    fun onSendPrompt(text: String) {
        onPromptChange(text)
        onSend()
    }

    fun onSend() {
        val promptText = _uiState.value.prompt.trim()
        if (promptText.isEmpty()) {
            _uiState.update { it.copy(promptError = PromptError.EMPTY) }
            return
        }
        if (!hasApiKey) {
            _uiState.update { it.copy(errorMessage = MISSING_API_KEY_MESSAGE) }
            return
        }
        if (_uiState.value.isLoading) return

        lastUserPrompt = promptText

        val userMessage = ChatMessage(text = promptText, participant = Participant.USER)

        _uiState.update {
            it.copy(
                prompt = "",
                messages = it.messages + userMessage,
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    val modelMessage = ChatMessage(text = responseText, participant = Participant.MODEL)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = responseText,
                            messages = it.messages + modelMessage,
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to generate response. Please try again."
                    val errorMessageObj = ChatMessage(
                        text = errorMsg,
                        participant = Participant.MODEL,
                        isError = true,
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg,
                            messages = it.messages + errorMessageObj,
                        )
                    }
                },
            )
        }
    }

    fun onRetry() {
        val promptToRetry = lastUserPrompt ?: return
        // Remove trailing error message if present
        _uiState.update { state ->
            val updatedMessages = if (state.messages.lastOrNull()?.isError == true) {
                state.messages.dropLast(1)
            } else {
                state.messages
            }
            state.copy(messages = updatedMessages)
        }
        onSendPrompt(promptToRetry)
    }

    fun onClearChat() {
        _uiState.update {
            ChatUiState(
                prompt = "",
                messages = emptyList(),
                response = "",
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    companion object {
        const val MISSING_API_KEY_MESSAGE =
            "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

        fun factory(repository: GeminiRepository, hasApiKey: Boolean) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, hasApiKey) as T
            }
    }
}
