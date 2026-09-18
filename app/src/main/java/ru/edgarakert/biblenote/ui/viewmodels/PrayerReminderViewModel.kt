package ru.edgarakert.biblenote.ui.viewmodels

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.prayer.PrayerReminderScheduler
import ru.edgarakert.biblenote.data.settings.SettingsRepository

/**
 * Настройки ежедневного напоминания (задача 14.13). Настройка в DataStore и взведённый будильник
 * меняются только вместе, здесь — чтобы экран не мог рассинхронизировать одно с другим.
 *
 * Разрешение POST_NOTIFICATIONS запрашивает экран (нужен ActivityResult), а сюда приходит уже
 * итог: [enable] — разрешение есть, [onPermissionDenied] — отказ.
 *
 * Перевзвода на ON_START приложения, который предлагал план, здесь нет сознательно: будильник уже
 * перевзводится при холодном старте процесса, после перезагрузки и после обновления приложения —
 * это все случаи, когда он действительно пропадает.
 */
class PrayerReminderViewModel(
    private val appContext: Context,
    private val settings: SettingsRepository,
) : ViewModel() {

    /** null — ещё не прочитано из DataStore: не показываем переключатель, чтобы он не мигал. */
    val enabled: StateFlow<Boolean?> = settings.prayerReminderEnabled
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val minutes: StateFlow<Int?> = settings.prayerReminderMinutes
        .map<Int, Int?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _permissionDenied = MutableStateFlow(false)
    /** Пользователь отказал в разрешении при включении — показать подсказку про настройки системы. */
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    private val _notificationsBlocked = MutableStateFlow(false)
    /** Уведомления выключены в системе — напоминание включено, но показываться не будет. */
    val notificationsBlocked: StateFlow<Boolean> = _notificationsBlocked.asStateFlow()

    init {
        refreshNotificationStatus()
    }

    /** Вызывать при возврате на экран: пользователь мог включить уведомления в настройках системы. */
    fun refreshNotificationStatus() {
        val blocked = !NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        _notificationsBlocked.value = blocked
        if (!blocked) _permissionDenied.value = false
    }

    fun enable() {
        _permissionDenied.value = false
        viewModelScope.launch {
            settings.setPrayerReminderEnabled(true)
            PrayerReminderScheduler.schedule(appContext, settings.prayerReminderMinutes.first())
        }
        refreshNotificationStatus()
    }

    fun disable() {
        _permissionDenied.value = false
        viewModelScope.launch {
            settings.setPrayerReminderEnabled(false)
            PrayerReminderScheduler.cancel(appContext)
        }
    }

    /** Отказ в разрешении: напоминание остаётся выключенным, будильник не взводится. */
    fun onPermissionDenied() {
        _permissionDenied.value = true
    }

    /** Новое время сразу перепланирует будильник, если напоминание включено. */
    fun setMinutes(minutesSinceMidnight: Int) {
        viewModelScope.launch {
            settings.setPrayerReminderMinutes(minutesSinceMidnight)
            if (settings.prayerReminderEnabled.first()) {
                PrayerReminderScheduler.schedule(appContext, minutesSinceMidnight)
            }
        }
    }
}
