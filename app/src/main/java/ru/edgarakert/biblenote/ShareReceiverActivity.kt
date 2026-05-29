package ru.edgarakert.biblenote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.data.settings.SettingsRepository
import ru.edgarakert.biblenote.ui.screens.share.ShareNoteSheetContent
import ru.edgarakert.biblenote.ui.theme.BibleNoteTheme
import ru.edgarakert.biblenote.ui.theme.CardSurface
import ru.edgarakert.biblenote.ui.theme.DarkSurface
import ru.edgarakert.biblenote.ui.viewmodels.ShareNoteViewModel

class ShareReceiverActivity : ComponentActivity() {
    private val settingsRepository: SettingsRepository by inject()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent
            .takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)
            .orEmpty()

        if (sharedText.isBlank()) {
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            val appearanceMode by settingsRepository.appearanceMode
                .collectAsStateWithLifecycle(SettingsRepository.AppearanceMode.SYSTEM)
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
                val viewModel: ShareNoteViewModel = koinViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val notes by viewModel.allNotes.collectAsStateWithLifecycle()

                LaunchedEffect(sharedText) {
                    viewModel.setSharedText(sharedText)
                }
                LaunchedEffect(Unit) {
                    viewModel.dismiss.collect { finish() }
                }

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                ModalBottomSheet(
                    onDismissRequest = ::finish,
                    sheetState = sheetState,
                    containerColor = if (darkTheme) DarkSurface else CardSurface,
                    scrimColor = Color(0x80000000)
                ) {
                    ShareNoteSheetContent(
                        uiState = uiState,
                        notes = notes,
                        darkTheme = darkTheme,
                        onModeChange = viewModel::setMode,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onSelectNote = viewModel::setSelectedNote,
                        onSaveNewNote = viewModel::saveAsNewNote,
                        onAppendToNote = viewModel::appendToNote,
                        onDismiss = ::finish
                    )
                }
            }
        }
    }
}