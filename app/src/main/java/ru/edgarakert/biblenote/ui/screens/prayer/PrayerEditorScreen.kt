package ru.edgarakert.biblenote.ui.screens.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.data.db.PrayerCategory
import ru.edgarakert.biblenote.ui.components.prayer.prayerCategoryLabel
import ru.edgarakert.biblenote.ui.viewmodels.PrayerEditorViewModel

/**
 * Один экран на создание и редактирование просьбы (задача 14.11), различаются по [requestId]
 * (`-1L` — создание). Поля хранятся во [PrayerEditorViewModel], а не в `rememberSaveable` здесь —
 * так и поворот экрана, и пересоздание Composable не теряют введённое (в фазе 13 уже был баг
 * «поворот съел состояние»).
 *
 * Пока [PrayerEditorViewModel.isLoaded] не стал `true` (при редактировании — до ответа
 * `getRequestById`), поля не рисуются: иначе пользователь успел бы набрать текст в пустые поля,
 * а следом их перезаписала бы загрузка.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerEditorScreen(
    requestId: Long,
    onBack: () -> Unit,
    viewModel: PrayerEditorViewModel = koinViewModel(parameters = { parametersOf(requestId) })
) {
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val notFound by viewModel.notFound.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val body by viewModel.body.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val versesInput by viewModel.versesInput.collectAsStateWithLifecycle()
    val canSave by viewModel.canSave.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { onBack() }
    }

    // Просьбу удалили между тапом на неё и загрузкой редактора (поправка 3 к плану 14.10) —
    // закрываемся, а не показываем пустую форму, которая при сохранении создала бы новую просьбу.
    LaunchedEffect(notFound) {
        if (notFound) onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (requestId == -1L) R.string.prayer_editor_new_title
                            else R.string.prayer_editor_edit_title
                        ),
                        fontFamily = FontFamily.Serif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.editor_back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::save, enabled = canSave) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(R.string.common_save),
                            tint = if (canSave) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (!isLoaded || notFound) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::setTitle,
                label = { Text(stringResource(R.string.prayer_editor_title_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = body,
                onValueChange = viewModel::setBody,
                label = { Text(stringResource(R.string.prayer_editor_body_placeholder)) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            PrayerCategoryDropdown(
                category = category,
                onCategorySelected = viewModel::setCategory
            )

            OutlinedTextField(
                value = versesInput,
                onValueChange = viewModel::setVersesInput,
                label = { Text(stringResource(R.string.prayer_editor_verses_label)) },
                placeholder = { Text(stringResource(R.string.prayer_editor_verses_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerCategoryDropdown(
    category: PrayerCategory,
    onCategorySelected: (PrayerCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = prayerCategoryLabel(category),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.prayer_editor_category_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PrayerCategory.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(prayerCategoryLabel(entry)) },
                    onClick = {
                        onCategorySelected(entry)
                        expanded = false
                    }
                )
            }
        }
    }
}
