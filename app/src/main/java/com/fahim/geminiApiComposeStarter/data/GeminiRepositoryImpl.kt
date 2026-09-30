package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.toDomainModel
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyStorage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatSession
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

private const val TAG = "GeminiRepository"
private const val PRIMARY_MODEL = "gemini-2.5-flash"
private const val FALLBACK_MODEL = "gemini-2.0-flash"

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val chatMessageDao: ChatMessageDao? = null,
    private val secureApiKeyStorage: SecureApiKeyStorage? = null,
    private var modelName: String = PRIMARY_MODEL,
) : GeminiRepository {

    override fun getChatHistory(conversationId: String): Flow<List<ChatMessage>> {
        return chatMessageDao?.getMessagesForConversation(conversationId)?.map { entities ->
            entities.map { it.toDomainModel() }
        } ?: emptyFlow()
    }

    override fun getChatSessions(): Flow<List<ChatSession>> {
        return chatMessageDao?.getAllMessages()?.map { entities ->
            entities.groupBy { it.conversationId }
                .map { (convId, messages) ->
                    val firstUserMsg = messages.firstOrNull { it.participant == Participant.USER.name }?.text
                    val title = if (!firstUserMsg.isNullOrBlank()) {
                        if (firstUserMsg.length > 32) "${firstUserMsg.take(32)}..." else firstUserMsg
                    } else {
                        "New Chat"
                    }
                    val lastTimestamp = messages.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                    ChatSession(
                        id = convId,
                        title = title,
                        updatedAt = lastTimestamp,
                    )
                }
                .sortedByDescending { it.updatedAt }
        } ?: emptyFlow()
    }

    override suspend fun saveMessage(message: ChatMessage) {
        chatMessageDao?.insertMessage(message.toEntity())
    }

    override suspend fun clearChatHistory() {
        chatMessageDao?.clearAllMessages()
    }

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
            val activeKey = secureApiKeyStorage?.getDecryptedApiKey()?.takeIf { it.isNotBlank() } ?: apiKey
            if (activeKey.isBlank()) {
                return Result.failure(IllegalStateException("Unable to initialize secure Gemini configuration."))
            }

            val currentModel = GenerativeModel(modelName = modelName, apiKey = activeKey)
            val response = currentModel.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }

            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed for model $modelName")

            if (modelName == PRIMARY_MODEL) {
                try {
                    val activeKey = secureApiKeyStorage?.getDecryptedApiKey()?.takeIf { it.isNotBlank() } ?: apiKey
                    val fallbackModel = GenerativeModel(modelName = FALLBACK_MODEL, apiKey = activeKey)
                    val fallbackResponse = fallbackModel.generateContent(prompt)
                    val fallbackText = fallbackResponse.text?.takeIf { it.isNotBlank() }
                    if (fallbackText != null) {
                        modelName = FALLBACK_MODEL
                        return Result.success(fallbackText)
                    }
                } catch (fallbackException: CancellationException) {
                    throw fallbackException
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Fallback model $FALLBACK_MODEL also failed")
                }
            }

            val sanitizedMessage = sanitizeError(e)
            Result.failure(IllegalStateException(sanitizedMessage))
        }
    }

    private fun sanitizeError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("404") || msg.contains("NOT_FOUND") ->
                "The requested Gemini model is currently unavailable on the server. Please try again shortly."

            msg.contains("503") || msg.contains("UNAVAILABLE") || msg.contains("high demand") ->
                "Gemini is currently experiencing high demand. Please try again in a few moments."

            msg.contains("MissingFieldException") || msg.contains("GRpcError") ->
                "Received an unexpected response from Gemini server. Please try again."

            msg.isNotBlank() && !msg.contains("kotlinx.serialization") && !msg.contains("KeyStore") ->
                msg

            else ->
                "Unable to initialize secure Gemini configuration."
        }
    }
}
