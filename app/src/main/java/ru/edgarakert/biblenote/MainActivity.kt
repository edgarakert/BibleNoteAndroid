package ru.edgarakert.biblenote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import ru.edgarakert.biblenote.ui.screens.notes.NotesListScreen
import ru.edgarakert.biblenote.ui.theme.BibleNoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BibleNoteTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    // Full NavGraph with 3 tabs will be wired in Phase 8
    NotesListScreen()
}
