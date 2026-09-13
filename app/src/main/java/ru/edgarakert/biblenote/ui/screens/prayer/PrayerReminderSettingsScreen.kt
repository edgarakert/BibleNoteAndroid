package ru.edgarakert.biblenote.ui.screens.prayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.edgarakert.biblenote.R
import ru.edgarakert.biblenote.ui.viewmodels.PrayerReminderViewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Настройки ежедневного напоминания (задача 14.13). Разрешение на уведомления запрашивается
 * в момент включения, а не при первом запуске. При отказе переключатель остаётся выключенным и
 * появляется спокойная подсказка с переходом в настройки системы — без неё после «больше не
 * спрашивать» тап по переключателю молча ничего бы не делал.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerReminderSettingsScreen(
    onBack: () -> Unit,
    viewModel: PrayerReminderViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    val minutes by viewModel.minutes.collectAsStateWithLifecycle()
    val permissionDenied by viewModel.permissionDenied.collectAsStateWithLifecycle()
    val notificationsBlocked by viewModel.notificationsBlocked.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.enable() else viewModel.onPermissionDenied()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshNotificationStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onToggle(turnOn: Boolean) {
        if (!turnOn) {
            viewModel.disable()
            return
        }
        val needsRuntimePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsRuntimePermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.enable()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.prayer_reminder_settings_title),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        val isEnabled = enabled ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Вся строка переключает — так её целиком озвучивает TalkBack как переключатель.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = isEnabled, role = Role.Switch, onValueChange = ::onToggle)
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.prayer_reminder_enable_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = isEnabled, onCheckedChange = null)
            }

            if (isEnabled && minutes != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prayer_reminder_time_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatReminderTime(minutes!!),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (permissionDenied || (isEnabled && notificationsBlocked)) {
                Text(
                    text = stringResource(R.string.prayer_reminder_notifications_off),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) {
                    Text(stringResource(R.string.prayer_reminder_open_settings))
                }
            }
        }

        if (showTimePicker && minutes != null) {
            ReminderTimePickerDialog(
                initialMinutes = minutes!!,
                onDismiss = { showTimePicker = false },
                onConfirm = { picked ->
                    showTimePicker = false
                    viewModel.setMinutes(picked)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.prayer_reminder_time_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.folder_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            // Цвета из палитры приложения: стандартные контейнеры Material фиолетовые
            // и выпадают из пергаментной темы — ровно это уже случилось с DatePicker в 14.10.
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = MaterialTheme.colorScheme.surface,
                    timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface,
                    periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    )
}

/** Время в формате локали устройства: «09:00» или «9:00 AM». */
private fun formatReminderTime(minutesSinceMidnight: Int): String =
    LocalTime.of(minutesSinceMidnight / 60, minutesSinceMidnight % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
