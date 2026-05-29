package ru.edgarakert.biblenote.ui.screens.bible

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.bible.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleBookPickerScreen(
    currentBookId: Int,
    currentChapter: Int,
    books: List<Book>,
    translation: String,
    onFetchChapterCount: suspend (bookId: Int) -> Int,
    onSelect: (bookId: Int, chapter: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBook: Book? by remember { mutableStateOf(null) }
    val oldTestament = remember(books) { books.filter { it.id <= 39 } }
    val newTestament = remember(books) { books.filter { it.id >= 40 } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedBook?.name(translation)
                            ?: stringResource(R.string.bible_picker_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedBook != null) selectedBook = null else onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (selectedBook == null) {
            BookListContent(
                oldTestament = oldTestament,
                newTestament = newTestament,
                currentBookId = currentBookId,
                translation = translation,
                onBookSelect = { selectedBook = it },
                modifier = Modifier.padding(padding)
            )
        } else {
            val book = selectedBook ?: return@Scaffold
            ChapterGridContent(
                bookId = book.id,
                currentChapter = if (book.id == currentBookId) currentChapter else null,
                onFetchChapterCount = onFetchChapterCount,
                onSelect = { chapter -> onSelect(book.id, chapter) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun BookListContent(
    oldTestament: List<Book>,
    newTestament: List<Book>,
    currentBookId: Int,
    translation: String,
    onBookSelect: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { SectionHeader(stringResource(R.string.bible_old_testament)) }
        items(oldTestament, key = { it.id }) { book ->
            BookRow(
                name = book.name(translation),
                isCurrent = book.id == currentBookId,
                onClick = { onBookSelect(book) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }
        item { SectionHeader(stringResource(R.string.bible_new_testament)) }
        items(newTestament, key = { it.id }) { book ->
            BookRow(
                name = book.name(translation),
                isCurrent = book.id == currentBookId,
                onClick = { onBookSelect(book) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun BookRow(
    name: String,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = FontFamily.Serif,
        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    )
}

@Composable
private fun ChapterGridContent(
    bookId: Int,
    currentChapter: Int?,
    onFetchChapterCount: suspend (bookId: Int) -> Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var chapterCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(bookId) {
        chapterCount = onFetchChapterCount(bookId)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(chapterCount) { index ->
            val chapter = index + 1
            val isCurrent = chapter == currentChapter
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .aspectRatio(1f)
                    .background(
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(chapter) }
            ) {
                Text(
                    text = "$chapter",
                    fontFamily = FontFamily.Serif,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
