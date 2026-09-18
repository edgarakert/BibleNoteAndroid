package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.edgarakert.biblenote.data.PrayerRepository
import ru.edgarakert.biblenote.data.prayer.AnsweredPrayers
import ru.edgarakert.biblenote.data.prayer.AnsweredYearGroup

/**
 * Экран «Отвеченные молитвы» (задача 14.12). Группировка и порядок — чистая функция
 * [AnsweredPrayers.groupByYear] с юнит-тестами, здесь только источник данных.
 */
class AnsweredPrayersViewModel(
    repository: PrayerRepository,
) : ViewModel() {

    val groups: StateFlow<List<AnsweredYearGroup>> = repository.observeAllRequests()
        .map { AnsweredPrayers.groupByYear(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
