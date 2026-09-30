package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepository
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: GeminiRepository,
    private val preferencesRepository: PreferencesRepository? = null,
    private val hasApiKey: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var historyObservationJob: Job? = null
    private var lastUserPrompt: String? = null

    init {
        // Observe list of all chat sessions
        viewModelScope.launch {
            repository.getChatSessions().collect { sessions ->
                _uiState.update { state ->
                    state.copy(conversations = sessions)
                }
            }
        }

        // Start observing current active conversation history
        observeConversationHistory(_uiState.value.activeConversationId)

        // Observe DataStore Theme preference
        preferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.themeMode.collect { mode ->
                    _uiState.update { state ->
                        state.copy(themeMode = mode)
                    }
                }
            }
        }
    }

    private fun observeConversationHistory(conversationId: String) {
        historyObservationJob?.cancel()
        historyObservationJob = viewModelScope.launch {
            repository.getChatHistory(conversationId).collect { history ->
                _uiState.update { state ->
                    state.copy(
                        activeConversationId = conversationId,
                        messages = history,
                    )
                }
            }
        }
    }

    fun onNewChat() {
        if (_uiState.value.messages.isEmpty()) return // Already empty

        val newConversationId = UUID.randomUUID().toString()
        _uiState.update {
            it.copy(
                activeConversationId = newConversationId,
                prompt = "",
                response = "",
                errorMessage = null,
                promptError = null,
            )
        }
        observeConversationHistory(newConversationId)
    }

    fun onSelectConversation(conversationId: String) {
        if (conversationId == _uiState.value.activeConversationId) return
        _uiState.update {
            it.copy(
                prompt = "",
                response = "",
                errorMessage = null,
                promptError = null,
            )
        }
        observeConversationHistory(conversationId)
    }

    fun onThemeModeSelected(mode: ThemeMode) {
        preferencesRepository?.let { prefs ->
            viewModelScope.launch {
                prefs.setThemeMode(mode)
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
        val activeConvId = _uiState.value.activeConversationId

        val userMessage = ChatMessage(
            conversationId = activeConvId,
            text = promptText,
            participant = Participant.USER,
        )

        _uiState.update {
            it.copy(
                prompt = "",
                isLoading = true,
                errorMessage = null,
                promptError = null,
            )
        }

        viewModelScope.launch {
            repository.saveMessage(userMessage)

            repository.generateText(promptText).fold(
                onSuccess = { responseText ->
                    val modelMessage = ChatMessage(
                        conversationId = activeConvId,
                        text = responseText,
                        participant = Participant.MODEL,
                    )
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

        fun factory(
            repository: GeminiRepository,
            preferencesRepository: PreferencesRepository? = null,
            hasApiKey: Boolean,
        ) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ChatViewModel(repository, preferencesRepository, hasApiKey) as T
            }
    }
}
