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

    init {
        // Observe Room chat history Flow as single source of truth
        viewModelScope.launch {
            repository.getChatHistory().collect { history ->
                _uiState.update { state ->
                    state.copy(messages = history)
                }
            }
        }
    }

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
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            // Save user message to Room
            repository.saveMessage(userMessage)

            // Request response from Gemini API
            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    val modelMessage = ChatMessage(text = responseText, participant = Participant.MODEL)
                    repository.saveMessage(modelMessage)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = responseText,
                        )
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "Failed to generate response. Please try again."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = errorMsg,
                        )
                    }
                },
            )
        }
    }

    fun onRetry() {
        val promptToRetry = lastUserPrompt ?: return
        onSendPrompt(promptToRetry)
    }

    fun onClearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            _uiState.update {
                it.copy(
                    prompt = "",
                    response = "",
                    isLoading = false,
                    errorMessage = null,
                )
            }
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
