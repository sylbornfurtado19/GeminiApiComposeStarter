package com.fahim.geminiApiComposeStarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import com.fahim.geminiApiComposeStarter.data.local.AppDatabase
import com.fahim.geminiApiComposeStarter.data.preferences.PreferencesRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRoute
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val preferencesRepository = PreferencesRepositoryImpl(applicationContext)
        ChatViewModel.factory(
            repository = GeminiRepositoryImpl(
                apiKey = BuildConfig.GEMINI_API_KEY,
                chatMessageDao = database.chatMessageDao(),
            ),
            preferencesRepository = preferencesRepository,
            hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            GeminiApiComposeStarterTheme(themeMode = uiState.themeMode) {
                ChatRoute(viewModel = viewModel)
            }
        }
    }
}
