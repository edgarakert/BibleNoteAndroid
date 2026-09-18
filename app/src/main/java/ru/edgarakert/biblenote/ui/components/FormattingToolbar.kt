package ru.edgarakert.biblenote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.edgarakert.biblenote.R

/**
 * Панель форматирования под редактором заметки: жирный, курсив, размер текста.
 *
 * Компонент сам ничего не знает ни про фокус, ни про клавиатуру — оба вопроса решает вызывающий
 * (`NoteEditorScreen`):
 * - показывается, только когда поле заметки в фокусе (`isContentFocused`, из `onFocusChanged`
 *   у `BibleEditText`) — не привязана к выделению текста и не является IME accessory view,
 *   в отличие от `FormattingToolbar` в iOS. Нажатие кнопки не забирает фокус у поля (клавиатура
 *   не закрывается при тапе по тулбару — проверено на устройстве), поэтому панель не мигает при
 *   собственных нажатиях.
 * - `imePadding()` на `Column` в `NoteEditorScreen` поднимает весь контент экрана над клавиатурой,
 *   поэтому панель остаётся видна над ней, а не оказывается перекрыта снизу.
 */
@Composable
fun FormattingToolbar(
    isBoldActive: Boolean,
    isItalicActive: Boolean,
    isSizeActive: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onSize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Row закрашивает всю свою высоту сплошным фоном, поэтому разделитель рисуется
        // ПОСЛЕ неё в том же Box — иначе Row перекрывает его собой (оба по умолчанию
        // выравниваются по TopStart и делят одну и ту же верхнюю область).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            FormattingToolbarButton(
                icon = Icons.Default.FormatBold,
                contentDescription = stringResource(R.string.editor_format_bold),
                isActive = isBoldActive,
                onClick = onBold
            )
            FormattingToolbarButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = stringResource(R.string.editor_format_italic),
                isActive = isItalicActive,
                onClick = onItalic
            )
            FormattingToolbarButton(
                icon = Icons.Default.FormatSize,
                contentDescription = stringResource(R.string.editor_format_size),
                isActive = isSizeActive,
                onClick = onSize
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
    }
}

@Composable
private fun FormattingToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
