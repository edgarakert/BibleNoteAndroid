package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.PrayerRepository
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.prayer.PrayerActions
import ru.edgarakert.biblenote.data.prayer.PrayerSelectionService

/** Просьба на экране «Сегодня» вместе с тем, помолились ли за неё сегодня. */
data class PrayerTodayItem(
    val request: PrayerRequest,
    val hasPrayedToday: Boolean,
)

/**
 * «Помолились ли сегодня» зависит от текущей даты, а поток просьб из БД пересчитывается только
 * при изменении данных — если экран остаётся открытым через полночь, ни порядок групп, ни
 * погасшие кнопки сами не обновятся. [onResume] — тик, который дергает пересчёт при возврате
 * экрана на передний план (см. вызов из `PrayerTodayScreen` на `Lifecycle.Event.ON_RESUME`);
 * полноценный таймер до полуночи не нужен — поправка к плану 14.8 прямо это исключает.
 *
 * И порядок групп ([PrayerSelectionService.todaysSelection]), и `hasPrayedToday` каждой карточки
 * считаются от одного и того же `now` внутри одного `combine` — иначе они могли бы разойтись
 * ровно в момент смены дня. Поэтому наружу отдаётся не «сырой» список просьб, а [PrayerTodayItem]:
 * список из одних и тех же объектов, где изменился только флаг, не пересоздал бы `StateFlow`
 * (он конфлейтит равные значения) и кнопка осталась бы погашенной до следующего изменения в БД.
 */
class PrayerTodayViewModel(
    private val repository: PrayerRepository,
) : ViewModel() {

    private val resumeTick = MutableStateFlow(0)

    val items: StateFlow<List<PrayerTodayItem>> = combine(
        repository.observeAllRequests(),
        resumeTick
    ) { requests, _ ->
        val now = System.currentTimeMillis()
        PrayerSelectionService.todaysSelection(requests, now).map { request ->
            PrayerTodayItem(request, PrayerActions.hasPrayedToday(request, now))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Вызывай на `Lifecycle.Event.ON_RESUME` экрана — см. KDoc класса. */
    fun onResume() {
        resumeTick.update { it + 1 }
    }

    fun markPrayed(request: PrayerRequest) {
        viewModelScope.launch { repository.saveRequest(PrayerActions.markPrayedToday(request)) }
    }
}
