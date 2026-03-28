package com.faster.aiwriter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.faster.aiwriter.ui.AiChatApp
import com.faster.aiwriter.ui.theme.AiChatTheme
import com.faster.aiwriter.viewmodel.ChatViewModel
import com.faster.aiwriter.viewmodel.StoryViewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private val storyViewModel: StoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for modern Android experience
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AiChatTheme {
                AiChatApp(
                    chatViewModel = chatViewModel,
                    storyViewModel = storyViewModel
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Auto-save current conversation when app goes to background
        chatViewModel.autoSave()
    }
}
