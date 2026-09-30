package com.fahim.geminiApiComposeStarter.data.security

import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepository

class SecureApiKeyStorage(
    private val encryptionManager: ApiKeyEncryptionManager = ApiKeyEncryptionManager(),
    private val preferencesRepository: PreferencesRepository?,
) {

    /** Retrieves decrypted API key in memory, performing first-run encryption migration if needed */
    suspend fun getDecryptedApiKey(): String {
        if (preferencesRepository == null) {
            return BuildConfig.GEMINI_API_KEY
        }

        val (encryptedKey, iv) = preferencesRepository.getEncryptedApiKeyData()

        if (encryptedKey.isNotBlank() && iv.isNotBlank()) {
            val decrypted = encryptionManager.decrypt(encryptedKey, iv)
            if (decrypted.isNotBlank()) {
                return decrypted
            }
        }

        // First-run migration: Encrypt configured BuildConfig key and save to DataStore
        val plainKey = BuildConfig.GEMINI_API_KEY
        if (plainKey.isNotBlank()) {
            val (newEncryptedKey, newIv) = encryptionManager.encrypt(plainKey)
            if (newEncryptedKey.isNotBlank() && newIv.isNotBlank()) {
                preferencesRepository.saveEncryptedApiKey(newEncryptedKey, newIv)
            }
        }

        return plainKey
    }
}
