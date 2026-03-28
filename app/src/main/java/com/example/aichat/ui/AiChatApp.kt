package com.faster.aiwriter.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.faster.aiwriter.ui.screens.ChatScreen
import com.faster.aiwriter.ui.screens.HistoryScreen
import com.faster.aiwriter.ui.screens.SetupScreen
import com.faster.aiwriter.ui.screens.StoryScreen
import com.faster.aiwriter.ui.screens.StorySetupScreen
import com.faster.aiwriter.viewmodel.ChatViewModel
import com.faster.aiwriter.viewmodel.StoryViewModel

@Composable
fun AiChatApp(
    chatViewModel: ChatViewModel,
    storyViewModel: StoryViewModel,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavHost(
        navController = navController,
        startDestination = "chat",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth }
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth }
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth }
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth }
            )
        }
    ) {
        composable("chat") {
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateToSetup = { navController.navigate("setup") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }

        composable("story_setup") {
            StorySetupScreen(
                viewModel = storyViewModel,
                onNavigateToStory = { navController.navigate("story") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("story") {
            StoryScreen(
                viewModel = storyViewModel,
                onNavigateToSetup = { navController.navigate("story_setup") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("setup") {
            SetupScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            HistoryScreen(
                viewModel = chatViewModel,
                onNavigateToChat = {
                    navController.popBackStack()
                    chatViewModel.newConversation()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}