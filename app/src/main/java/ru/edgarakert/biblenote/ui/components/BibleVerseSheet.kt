package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.VerseSnippetBuilder
import ru.edgarakert.biblenote.data.bible.translationShortName
import ru.edgarakert.biblenote.ui.viewmodels.BibleVerseSheetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVerseSheet(
    reference: BibleReference,
    onDismiss: () -> Unit,
    onOpenChapter: (BibleReference) -> Unit,
    onVersesChanged: ((List<Int>) -> Unit)? = null,
    onInsertVerses: ((List<VerseSnippetBuilder.Verse>) -> Unit)? = null,
    viewModel: BibleVerseSheetViewModel = koinViewModel(
        key = reference.id,
        parameters = { parametersOf(reference) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isEditable = onVersesChanged != null
    val listState = rememberLazyListState()
    var didScroll by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.verses.size) {
        if (didScroll || uiState.verses.isEmpty()) return@LaunchedEffect
        val target = viewModel.scrollTarget ?: return@LaunchedEffect
        val position = uiState.verses.indexOfFirst { it.first == target }
        if (position < 0) return@LaunchedEffect
        listState.scrollToItem(position)
        didScroll = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = uiState.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp)
            )

            if (uiState.enabledTranslations.size > 1) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 10.dp)
                ) {
                    uiState.enabledTranslations.forEach { t ->
                        val selected = t == uiState.selectedTranslation
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.setTranslation(t) }
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = translationShortName(t),
                                fontSize = 12.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )
            }

            if (uiState.isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.verses.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.verse_not_found),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(uiState.verses, key = { it.first }) { (num, text) ->
                        val isSelected = num in uiState.selectedVerses
                        val isDimmed = isEditable && uiState.selectedVerses.isNotEmpty() && !isSelected

                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 2.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                            )
                            BibleVerse(
                                verseNumber = num,
                                text = text,
                                highlightColor = uiState.highlights[num],
                                verseScale = uiState.verseScale,
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (isDimmed) 0.55f else 1f)
                                    .then(
                                        if (isEditable) {
                                            // toggleable, а не clickable: выделение стиха
                                            // показано только полосой слева, которую screen
                                            // reader не видит, поэтому состояние «выбрано»
                                            // нужно сообщить семантикой.
                                            Modifier.toggleable(
                                                value = isSelected,
                                                onValueChange = {
                                                    onVersesChanged.invoke(viewModel.toggleVerse(num))
                                                }
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                        }
                    }
                }

                if (isEditable && uiState.verses.isNotEmpty()) {
                    val selectionCount = uiState.selectedVerses.size
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.title,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectionCount > 0) {
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.bible_verse_count,
                                            selectionCount,
                                            selectionCount
                                        ),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.verse_select_hint),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Вставка текста выделенных стихов в заметку — сверх паритета с iOS, где шторка
                // умеет только открыть главу. Кнопки нет, пока нечего вставлять: у ссылки на
                // главу целиком выбор пуст, и вставился бы весь текст главы.
                // Фильтруем заранее, а не в обработчике: номера из ссылки может не оказаться в
                // загруженной главе (разная нумерация в переводах), и кнопка при непустом выборе
                // оказалась бы «мёртвой» — нажатие без результата.
                val insertableVerses = uiState.verses
                    .filter { it.first in uiState.selectedVerses }
                    .map { VerseSnippetBuilder.Verse(it.first, it.second) }

                if (onInsertVerses != null && insertableVerses.isNotEmpty()) {
                    TextButton(
                        onClick = { onInsertVerses(insertableVerses) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.verse_insert_text),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }

                TextButton(
                    onClick = { onOpenChapter(reference) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.verse_open_chapter),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
