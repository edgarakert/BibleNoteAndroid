package ru.edgarakert.biblenote

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.android.ext.android.inject
import ru.edgarakert.biblenote.data.settings.SettingsRepository
import ru.edgarakert.biblenote.ui.navigation.AppNavHost
import ru.edgarakert.biblenote.ui.screens.onboarding.OnboardingScreen
import ru.edgarakert.biblenote.ui.screens.onboarding.TranslationSelectionScreen
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

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
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
                onNavigateToTranslations = {
                    navController.navigate("onboarding/translations") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("onboarding/translations") {
            TranslationSelectionScreen(
                onComplete = { navController.popBackStack("onboarding", inclusive = true) }
            )
        }
    }
}
