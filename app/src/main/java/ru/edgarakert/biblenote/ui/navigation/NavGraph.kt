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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.ui.screens.bible.BibleReaderScreen
import ru.edgarakert.biblenote.ui.screens.editor.NoteEditorScreen
import ru.edgarakert.biblenote.ui.screens.notes.FolderScreen
import ru.edgarakert.biblenote.ui.screens.notes.NotesListScreen
import ru.edgarakert.biblenote.ui.screens.settings.AboutScreen
import ru.edgarakert.biblenote.ui.screens.settings.BibleThemeSettingsScreen
import ru.edgarakert.biblenote.ui.screens.settings.SettingsScreen
import ru.edgarakert.biblenote.ui.screens.settings.TranslationSettingsScreen
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.Hairline
import ru.edgarakert.biblenote.ui.theme.Parchment
import ru.edgarakert.biblenote.ui.theme.WarmGray
import ru.edgarakert.biblenote.ui.viewmodels.SettingsViewModel

private enum class TopLevelRoute(
    val graphRoute: String,
    @param:StringRes val labelRes: Int,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    NOTES("notes_graph", R.string.tab_notes, Icons.Outlined.Description, Icons.Filled.Description),
    BIBLE(
        "bible_graph",
        R.string.tab_bible,
        Icons.AutoMirrored.Outlined.MenuBook,
        Icons.AutoMirrored.Filled.MenuBook
    ),
    SETTINGS(
        "settings_graph",
        R.string.tab_settings,
        Icons.Outlined.Settings,
        Icons.Filled.Settings
    )
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val settingsVm: SettingsViewModel = koinViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Parchment) {
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
                            selectedIconColor = Amber,
                            selectedTextColor = Amber,
                            unselectedIconColor = WarmGray,
                            unselectedTextColor = WarmGray,
                            indicatorColor = Hairline
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "notes_graph",
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) +
                        fadeIn(tween(280))
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) +
                        fadeOut(tween(280))
            }
        ) {
            navigation(route = "notes_graph", startDestination = "notes") {
                composable("notes") {
                    NotesListScreen(
                        onNavigateToNote = { noteId -> navController.navigate("editor/$noteId") },
                        onNavigateToFolder = { folderId -> navController.navigate("folder/$folderId") }
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
                    val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
                    FolderScreen(
                        folderId = folderId,
                        onBack = { navController.popBackStack() },
                        onNavigateToNote = { noteId -> navController.navigate("editor/$noteId") },
                        onNavigateToFolder = { subFolderId -> navController.navigate("folder/$subFolderId") },
                        viewModel = koinViewModel(parameters = { parametersOf(folderId) })
                    )
                }
            }

            navigation(route = "bible_graph", startDestination = "bible") {
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

            navigation(route = "settings_graph", startDestination = "settings") {
                composable("settings") {
                    SettingsScreen(
                        viewModel = settingsVm,
                        onNavigateToTheme = { navController.navigate("settings/theme") },
                        onNavigateToTranslations = { navController.navigate("settings/translations") },
                        onNavigateToAbout = { navController.navigate("settings/about") }
                    )
                }
                composable("settings/theme") {
                    BibleThemeSettingsScreen(
                        viewModel = settingsVm,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("settings/translations") {
                    TranslationSettingsScreen(
                        viewModel = settingsVm,
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
