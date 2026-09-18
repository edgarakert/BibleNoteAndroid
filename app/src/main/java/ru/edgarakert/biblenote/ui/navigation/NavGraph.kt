package ru.edgarakert.biblenote.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.NoteRepository
import ru.edgarakert.biblenote.data.settings.NotesPathEntry
import ru.edgarakert.biblenote.data.settings.SettingsRepository
import ru.edgarakert.biblenote.ui.screens.bible.BibleReaderScreen
import ru.edgarakert.biblenote.ui.screens.editor.NoteEditorScreen
import ru.edgarakert.biblenote.ui.screens.notes.FolderScreen
import ru.edgarakert.biblenote.ui.screens.notes.NotesListScreen
import ru.edgarakert.biblenote.ui.screens.settings.AboutScreen
import ru.edgarakert.biblenote.ui.screens.settings.BibleThemeSettingsScreen
import ru.edgarakert.biblenote.ui.screens.settings.SettingsScreen
import ru.edgarakert.biblenote.ui.screens.settings.TranslationSettingsScreen

private enum class TopLevelRoute(
    val graphRoute: String,
    @param:StringRes val labelRes: Int,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    NOTES(
        graphRoute = "notes_graph",
        labelRes = R.string.tab_notes,
        outlinedIcon = Icons.Outlined.Description,
        filledIcon = Icons.Filled.Description
    ),
    BIBLE(
        graphRoute = "bible_graph",
        labelRes = R.string.tab_bible,
        outlinedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        filledIcon = Icons.AutoMirrored.Filled.MenuBook
    ),
    SETTINGS(
        graphRoute = "settings_graph",
        labelRes = R.string.tab_settings,
        outlinedIcon = Icons.Outlined.Settings,
        filledIcon = Icons.Filled.Settings
    )
}

@Composable
fun AppNavHost(initialNotesPath: List<NotesPathEntry> = emptyList()) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val settingsRepository: SettingsRepository = koinInject()
    val noteRepository: NoteRepository = koinInject()
    val scope = rememberCoroutineScope()

    // Начинаем пустым и наращиваем через pushNotesPath (в том числе во время восстановления
    // ниже) — так путь всегда отражает реальный back stack, независимо от того, кто до него
    // довёл: обычный тап пользователя или посев при старте.
    var notesPath by remember { mutableStateOf(emptyList<NotesPathEntry>()) }

    fun pushNotesPath(entry: NotesPathEntry) {
        notesPath = notesPath + entry
        scope.launch { settingsRepository.setNotesLastPath(notesPath) }
    }

    fun popNotesPathTo(depth: Int) {
        if (notesPath.size <= depth) return
        notesPath = notesPath.take(depth)
        scope.launch { settingsRepository.setNotesLastPath(notesPath) }
    }

    // Устойчиво к back-навигации любого рода (кнопка «назад» в тулбаре, системный жест,
    // переключение вкладок с restoreState): реагирует на фактическую смену вершины back
    // stack, а не на явные вызовы pop, поэтому не может рассинхронизироваться с реальной
    // навигацией. Во время посева ниже это тоже срабатывает на каждый шаг, но идемпотентно —
    // notesPath уже равен только что запрошенной глубине.
    LaunchedEffect(navBackStackEntry) {
        val entry = navBackStackEntry ?: return@LaunchedEffect
        when (entry.destination.route) {
            "notes" -> popNotesPathTo(0)
            "folder/{folderId}" -> {
                val folderId = entry.arguments?.getLong("folderId") ?: return@LaunchedEffect
                val depth = notesPath.indexOfLast {
                    it is NotesPathEntry.Folder && it.id == folderId
                } + 1
                if (depth > 0) popNotesPathTo(depth)
            }
        }
    }

    // Восстановление до первого кадра: сеет back stack вкладки «Заметки» тем же механизмом
    // push, что и обычная навигация, — так notesPath и персистентный путь остаются
    // консистентны. Риск R3: заметка/папка могла быть удалена (свайпом, из меню, или
    // автоудалена как пустая при выходе из редактора) — на первом неразрешимом элементе
    // восстановление останавливается, сохраняя уже восстановленный префикс.
    LaunchedEffect(Unit) {
        for (entry in initialNotesPath) {
            val exists = when (entry) {
                is NotesPathEntry.Folder -> noteRepository.observeFolderById(entry.id).first() != null
                is NotesPathEntry.Note -> noteRepository.getNoteById(entry.id) != null
            }
            if (!exists) break

            pushNotesPath(entry)
            when (entry) {
                is NotesPathEntry.Folder -> navController.navigate("folder/${entry.id}")
                is NotesPathEntry.Note -> navController.navigate("editor/${entry.id}")
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                TopLevelRoute.entries.forEach { tab ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == tab.graphRoute } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.graphRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = stringResource(tab.labelRes)
                            )
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelRoute.NOTES.graphRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    tween(280)
                ) +
                        fadeIn(tween(280))
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) +
                        fadeOut(tween(280))
            }
        ) {
            navigation(route = TopLevelRoute.NOTES.graphRoute, startDestination = "notes") {
                composable("notes") {
                    NotesListScreen(
                        onNavigateToNote = { noteId ->
                            pushNotesPath(NotesPathEntry.Note(noteId))
                            navController.navigate("editor/$noteId")
                        },
                        onNavigateToFolder = { folderId ->
                            pushNotesPath(NotesPathEntry.Folder(folderId))
                            navController.navigate("folder/$folderId")
                        }
                    )
                }
                composable(
                    route = "editor/{noteId}",
                    arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                    NoteEditorScreen(
                        noteId = noteId,
                        onBack = { navController.popBackStack() },
                        onOpenChapter = { ref ->
                            navController.navigate("bible_at/${ref.bookId}/${ref.chapter}") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        viewModel = koinViewModel(parameters = { parametersOf(noteId) })
                    )
                }
                composable(
                    route = "folder/{folderId}",
                    arguments = listOf(navArgument("folderId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val folderId =
                        backStackEntry.arguments?.getLong("folderId") ?: return@composable
                    FolderScreen(
                        folderId = folderId,
                        onBack = { navController.popBackStack() },
                        onNavigateToNote = { noteId ->
                            pushNotesPath(NotesPathEntry.Note(noteId))
                            navController.navigate("editor/$noteId")
                        },
                        onNavigateToFolder = { subFolderId ->
                            pushNotesPath(NotesPathEntry.Folder(subFolderId))
                            navController.navigate("folder/$subFolderId")
                        },
                        viewModel = koinViewModel(parameters = { parametersOf(folderId) })
                    )
                }
            }

            navigation(route = TopLevelRoute.BIBLE.graphRoute, startDestination = "bible") {
                composable("bible") {
                    BibleReaderScreen()
                }
                composable(
                    route = "bible_at/{bookId}/{chapter}",
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.IntType },
                        navArgument("chapter") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getInt("bookId") ?: return@composable
                    val chapter = backStackEntry.arguments?.getInt("chapter") ?: return@composable
                    BibleReaderScreen(pendingBookId = bookId, pendingChapter = chapter)
                }
            }

            navigation(route = TopLevelRoute.SETTINGS.graphRoute, startDestination = "settings") {
                composable("settings") {
                    SettingsScreen(
                        onNavigateToTheme = { navController.navigate("settings/theme") },
                        onNavigateToTranslations = { navController.navigate("settings/translations") },
                        onNavigateToAbout = { navController.navigate("settings/about") }
                    )
                }
                composable("settings/theme") {
                    BibleThemeSettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("settings/translations") {
                    TranslationSettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("settings/about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
