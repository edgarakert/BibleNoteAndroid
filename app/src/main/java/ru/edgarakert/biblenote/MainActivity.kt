package ru.edgarakert.biblenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.data.settings.SettingsRepository
import ru.edgarakert.biblenote.ui.viewmodels.SettingsViewModel
import ru.edgarakert.biblenote.ui.screens.bible.BibleReaderScreen
import ru.edgarakert.biblenote.ui.screens.editor.NoteEditorScreen
import ru.edgarakert.biblenote.ui.screens.notes.NotesListScreen
import ru.edgarakert.biblenote.ui.screens.onboarding.OnboardingScreen
import ru.edgarakert.biblenote.ui.screens.onboarding.TranslationSelectionScreen
import ru.edgarakert.biblenote.ui.screens.settings.AboutScreen
import ru.edgarakert.biblenote.ui.screens.settings.BibleThemeSettingsScreen
import ru.edgarakert.biblenote.ui.screens.settings.SettingsScreen
import ru.edgarakert.biblenote.ui.screens.settings.TranslationSettingsScreen
import ru.edgarakert.biblenote.ui.theme.BibleNoteTheme

class MainActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearanceMode by settingsRepository.appearanceMode
                .collectAsStateWithLifecycle(SettingsRepository.AppearanceMode.SYSTEM)
            val onboardingCompleted by settingsRepository.onboardingCompleted
                .collectAsStateWithLifecycle(false)

            val darkTheme = when (appearanceMode) {
                SettingsRepository.AppearanceMode.LIGHT -> false
                SettingsRepository.AppearanceMode.DARK -> true
                SettingsRepository.AppearanceMode.SYSTEM -> isSystemInDarkTheme()
            }

            BibleNoteTheme(darkTheme = darkTheme) {
                if (!onboardingCompleted) {
                    OnboardingFlow()
                } else {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
private fun OnboardingFlow() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "onboarding") {
        composable("onboarding") {
            OnboardingScreen(
                onNavigateToTranslations = { navController.navigate("onboarding/translations") }
            )
        }
        composable("onboarding/translations") {
            TranslationSelectionScreen(
                onComplete = { navController.popBackStack("onboarding", inclusive = true) }
            )
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()
    // Hoisted to AppNavHost level (Activity's ViewModelStore) so all settings screens share one instance
    val settingsVm: SettingsViewModel = koinViewModel()
    NavHost(navController = navController, startDestination = "notes") {
        composable("notes") {
            NotesListScreen(
                onNavigateToNote = { noteId -> navController.navigate("editor/$noteId") }
            )
        }
        composable("bible") {
            BibleReaderScreen()
        }
        composable(
            route = "editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteEditorScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsVm,
                onNavigateToTheme = { navController.navigate("settings/theme") },
                onNavigateToTranslations = { navController.navigate("settings/translations") },
                onNavigateToAbout = { navController.navigate("settings/about") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings/theme") {
            BibleThemeSettingsScreen(viewModel = settingsVm, onBack = { navController.popBackStack() })
        }
        composable("settings/translations") {
            TranslationSettingsScreen(viewModel = settingsVm, onBack = { navController.popBackStack() })
        }
        composable("settings/about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
