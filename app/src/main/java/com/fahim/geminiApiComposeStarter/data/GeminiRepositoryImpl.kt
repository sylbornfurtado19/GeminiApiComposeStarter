package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.toDomainModel
import com.fahim.geminiApiComposeStarter.data.local.toEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
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
    private val modelName: String = PRIMARY_MODEL,
) : GeminiRepository {

    private var model = GenerativeModel(modelName = modelName, apiKey = apiKey)

    override fun getChatHistory(): Flow<List<ChatMessage>> {
        return chatMessageDao?.getAllMessages()?.map { entities ->
            entities.map { it.toDomainModel() }
        } ?: emptyFlow()
    }

    override suspend fun saveMessage(message: ChatMessage) {
        chatMessageDao?.insertMessage(message.toEntity())
    }

    override suspend fun clearChatHistory() {
        chatMessageDao?.clearAllMessages()
    }

    override suspend fun generateText(prompt: String): Result<String> {
        try {
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                return Result.success(text)
            } else {
                return Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed for model ${model.modelName}", e)

            if (model.modelName == PRIMARY_MODEL) {
                try {
                    Log.i(TAG, "Attempting fallback to $FALLBACK_MODEL")
                    val fallbackModel = GenerativeModel(modelName = FALLBACK_MODEL, apiKey = apiKey)
                    val fallbackResponse = fallbackModel.generateContent(prompt)
                    val fallbackText = fallbackResponse.text?.takeIf { it.isNotBlank() }
                    if (fallbackText != null) {
                        model = fallbackModel
                        return Result.success(fallbackText)
                    }
                } catch (fallbackException: CancellationException) {
                    throw fallbackException
                } catch (fallbackException: Exception) {
                    Log.e(TAG, "Fallback model $FALLBACK_MODEL also failed", fallbackException)
                }
            }

            val sanitizedMessage = sanitizeError(e)
            return Result.failure(IllegalStateException(sanitizedMessage, e))
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

            msg.isNotBlank() && !msg.contains("kotlinx.serialization") ->
                msg

            else ->
                "Failed to connect to Gemini API. Please check your network connection and try again."
        }
    }
}
