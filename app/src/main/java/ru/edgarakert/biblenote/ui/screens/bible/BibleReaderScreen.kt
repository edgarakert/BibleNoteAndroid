package ru.edgarakert.biblenote.ui.screens.bible

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.bible.HighlightColor
import ru.edgarakert.biblenote.ui.components.BibleVerse
import ru.edgarakert.biblenote.ui.theme.Amber
import ru.edgarakert.biblenote.ui.theme.AmberSoft
import ru.edgarakert.biblenote.ui.theme.CardSurface
import ru.edgarakert.biblenote.ui.theme.Ink
import ru.edgarakert.biblenote.ui.theme.Parchment
import ru.edgarakert.biblenote.ui.theme.WarmGray
import androidx.compose.runtime.LaunchedEffect
import ru.edgarakert.biblenote.ui.viewmodels.BibleReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    pendingBookId: Int = -1,
    pendingChapter: Int = -1,
    viewModel: BibleReaderViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pendingBookId, pendingChapter) {
        if (pendingBookId > 0 && pendingChapter > 0) {
            viewModel.navigateTo(pendingBookId, pendingChapter)
        }
    }

    val context = LocalContext.current
    var showingPicker by remember { mutableStateOf(false) }
    val activeHighlight = remember(uiState.selectedVerseNumbers, uiState.highlights) {
        sharedHighlight(uiState.selectedVerseNumbers, uiState.highlights)
    }

    if (showingPicker) {
        BibleBookPickerScreen(
            currentBookId = uiState.bookId,
            currentChapter = uiState.chapter,
            books = uiState.books,
            translation = uiState.translation,
            onFetchChapterCount = { bookId -> viewModel.fetchChapterCount(bookId) },
            onSelect = { bookId, chapter ->
                viewModel.navigateTo(bookId, chapter)
                showingPicker = false
            },
            onDismiss = { showingPicker = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextButton(
                        onClick = { if (uiState.books.isNotEmpty()) showingPicker = true }
                    ) {
                        Text(
                            text = "${uiState.bookName} ${uiState.chapter}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Amber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                actions = {
                    if (uiState.enabledTranslations.size > 1) {
                        TranslationMenu(
                            translations = uiState.enabledTranslations,
                            selectedTranslation = uiState.translation,
                            onSelect = viewModel::setTranslation
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment)
            )
        },
        containerColor = Parchment
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = Amber,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.verses.isEmpty() -> {
                    EmptyStateContent(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    VersesContent(
                        uiState = uiState,
                        onToggleVerse = viewModel::toggleVerseSelection,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = uiState.selectedVerseNumbers.isEmpty() && uiState.verses.isNotEmpty() && !uiState.isLoading,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    ChapterNavBar(
                        canGoPrev = uiState.canGoPrev,
                        canGoNext = uiState.canGoNext,
                        onPrev = viewModel::navigatePrev,
                        onNext = viewModel::navigateNext
                    )
                }
                AnimatedVisibility(
                    visible = uiState.selectedVerseNumbers.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    VerseActionBar(
                        selectedCount = uiState.selectedVerseNumbers.size,
                        sharedHighlight = activeHighlight,
                        onDismiss = viewModel::clearSelection,
                        onCopy = {
                            copyToClipboard(context, viewModel.copySelectedVerses())
                            viewModel.clearSelection()
                        },
                        onHighlight = viewModel::applyHighlight,
                        onRemoveHighlight = viewModel::removeHighlights
                    )
                }
            }
        }
    }
}

@Composable
private fun VersesContent(
    uiState: BibleReaderViewModel.UiState,
    onToggleVerse: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 180.dp),
        modifier = modifier
    ) {
        items(uiState.verses, key = { it.first }) { (verseNum, text) ->
            BibleVerse(
                verseNumber = verseNum,
                text = text,
                highlightColor = uiState.highlights[verseNum],
                verseScale = uiState.verseScale,
                isSelected = verseNum in uiState.selectedVerseNumbers,
                modifier = Modifier.clickable { onToggleVerse(verseNum) }
            )
        }
    }
}

@Composable
private fun EmptyStateContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuOpen,
            contentDescription = null,
            tint = AmberSoft,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.verse_not_found),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Serif,
            color = WarmGray
        )
    }
}

@Composable
private fun ChapterNavBar(
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        if (canGoPrev) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier
                    .size(48.dp)
                    .background(CardSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }

        if (canGoNext) {
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .background(CardSurface, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Amber,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun VerseActionBar(
    selectedCount: Int,
    sharedHighlight: HighlightColor?,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onHighlight: (HighlightColor) -> Unit,
    onRemoveHighlight: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = CardSurface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(WarmGray.copy(alpha = 0.1f), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = WarmGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.bible_verse_count,
                        selectedCount,
                        selectedCount
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmGray
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Amber
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ColorPicker(
                sharedHighlight = sharedHighlight,
                onHighlight = onHighlight,
                onRemoveHighlight = onRemoveHighlight
            )
        }
    }
}

@Composable
private fun ColorPicker(
    sharedHighlight: HighlightColor?,
    onHighlight: (HighlightColor) -> Unit,
    onRemoveHighlight: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        HighlightColor.entries.forEach { hColor ->
            val isActive = sharedHighlight == hColor
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .background(hColor.lightColor, CircleShape)
                    .clickable {
                        if (isActive) onRemoveHighlight() else onHighlight(hColor)
                    }
            ) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationMenu(
    translations: List<String>,
    selectedTranslation: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = translationShortName(selectedTranslation),
                style = MaterialTheme.typography.labelMedium,
                color = Amber
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            translations.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (t == selectedTranslation) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Amber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(translationDisplayName(t), color = Ink)
                        }
                    },
                    onClick = {
                        onSelect(t)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun sharedHighlight(
    selectedVerses: Set<Int>,
    highlights: Map<Int, HighlightColor>
): HighlightColor? {
    if (selectedVerses.isEmpty()) return null
    val colors = selectedVerses.mapNotNull { highlights[it] }
    if (colors.size != selectedVerses.size) return null
    return if (colors.toSet().size == 1) colors.first() else null
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Bible verse", text))
}

private fun translationDisplayName(translation: String) = when (translation) {
    "synodal" -> "Синодальный"
    "nrt" -> "NRT"
    "kjv" -> "KJV"
    "niv" -> "NIV"
    else -> translation.uppercase()
}

private fun translationShortName(translation: String) = when (translation) {
    "synodal" -> "Синод."
    "nrt" -> "NRT"
    "kjv" -> "KJV"
    "niv" -> "NIV"
    else -> translation.uppercase()
}
