package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.PrayerRepository
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.prayer.PrayerActions
import ru.edgarakert.biblenote.data.prayer.PrayerListFilter
import ru.edgarakert.biblenote.data.prayer.PrayerListGrouping
import ru.edgarakert.biblenote.data.prayer.PrayerListSection

/**
 * Экран «Все просьбы» (задача 14.9). Вся логика «что показывать» — в чистой функции
 * [PrayerListGrouping.buildSections] (поправка 1 к плану, юнит-тесты — `PrayerListGroupingTest`),
 * здесь только состояние экрана и его источники.
 *
 * [filter] и [query] хранятся во ViewModel, а не в `remember`/`rememberSaveable` на экране —
 * ViewModel переживает поворот экрана (поправка 5 к плану). `SavedStateHandle` намеренно не
 * подключён: поправка отмечает его как опциональный шаг «если хочешь пережить и смерть процесса»,
 * а не как жёсткое требование, и ни один ViewModel в проекте пока его не использует — заводить
 * первый прецедент ради необязательного свойства здесь не стоит.
 */
class PrayerListViewModel(
    private val repository: PrayerRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(PrayerListFilter.ACTIVE)
    val filter: StateFlow<PrayerListFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val sections: StateFlow<List<PrayerListSection>> = combine(
        repository.observeAllRequests(),
        _filter,
        _query
    ) { requests, filter, query ->
        PrayerListGrouping.buildSections(requests, filter, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(value: PrayerListFilter) {
        _filter.value = value
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    /** «Доверить Богу» — без подтверждения (поправка 2 к плану). */
    fun entrust(request: PrayerRequest) {
        viewModelScope.launch { repository.saveRequest(PrayerActions.markEntrusted(request)) }
    }

    /** Каскадом удаляет дописки просьбы — контракт `PrayerDao`, проверенный тестом в 14.2. */
    fun delete(request: PrayerRequest) {
        viewModelScope.launch { repository.deleteRequest(request) }
    }
}
