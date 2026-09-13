package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.PrayerRepository
import ru.edgarakert.biblenote.data.bible.BibleReference
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.db.PrayerEntry
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.prayer.PrayerActions
import ru.edgarakert.biblenote.data.prayer.PrayerAnswerDate

/**
 * Экран карточки просьбы (задача 14.10).
 *
 * [uiState] различает три состояния явно, а не просто `PrayerRequest?`, — иначе нельзя было бы
 * отличить «ещё загружается» от «удалена» (поправка 3 к плану): пока `Room`-flow не прислал ни
 * одного значения, экран должен показывать прогресс, а не закрываться. Как только
 * `repository.observeRequestById` присылает `null` — просьбу удалили (из этого же экрана через
 * меню или откуда-то ещё), и экран обязан закрыться, а не падать на `!!` или показывать пустоту.
 *
 * `hasPrayedToday` пересчитывается тем же способом, что в `PrayerTodayViewModel` (поправка 2):
 * [onResume] — тик, дёргаемый на `Lifecycle.Event.ON_RESUME`, объединённый через `combine` с
 * потоком просьбы, чтобы оба флага считались от одного `now` и не расходились в момент смены дня.
 */
sealed interface PrayerDetailUiState {
    data object Loading : PrayerDetailUiState
    data object NotFound : PrayerDetailUiState
    data class Loaded(val request: PrayerRequest, val hasPrayedToday: Boolean) : PrayerDetailUiState
}

class PrayerDetailViewModel(
    private val requestId: Long,
    private val repository: PrayerRepository,
    private val parser: BibleReferenceParser,
) : ViewModel() {

    private val resumeTick = MutableStateFlow(0)

    val uiState: StateFlow<PrayerDetailUiState> = combine(
        repository.observeRequestById(requestId),
        resumeTick
    ) { request, _ ->
        if (request == null) {
            PrayerDetailUiState.NotFound
        } else {
            PrayerDetailUiState.Loaded(request, PrayerActions.hasPrayedToday(request))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrayerDetailUiState.Loading)

    val entries: StateFlow<List<PrayerEntry>> = repository.observeEntries(requestId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** `request.verseRefs.flatMap { parser.parse(it) }` — ровно как в плане, только реактивно. */
    val verseReferences: StateFlow<List<BibleReference>> = uiState
        .map { state ->
            (state as? PrayerDetailUiState.Loaded)?.request?.verseRefs?.flatMap(parser::parse) ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _entryText = MutableStateFlow("")
    val entryText: StateFlow<String> = _entryText.asStateFlow()

    // Поправка 8 к плану: открытость шторки ответа и введённый в неё текст должны пережить
    // поворот экрана — как и entryText выше, они живут здесь, а не в Compose `remember`.
    private val _showAnswerSheet = MutableStateFlow(false)
    val showAnswerSheet: StateFlow<Boolean> = _showAnswerSheet.asStateFlow()

    private val _answerText = MutableStateFlow("")
    val answerText: StateFlow<String> = _answerText.asStateFlow()

    /** Вызывай на `Lifecycle.Event.ON_RESUME` экрана — см. KDoc класса. */
    fun onResume() {
        resumeTick.update { it + 1 }
    }

    fun setEntryText(value: String) {
        _entryText.value = value
    }

    /** Дописка сохраняется через `PrayerRepository.saveEntry` (`@Upsert`) с датой = сейчас. */
    fun addEntry() {
        val trimmed = _entryText.value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.saveEntry(PrayerEntry(requestId = requestId, text = trimmed))
        }
        _entryText.value = ""
    }

    fun markPrayedToday() {
        val request = currentRequest() ?: return
        viewModelScope.launch { repository.saveRequest(PrayerActions.markPrayedToday(request)) }
    }

    fun openAnswerSheet() {
        _answerText.value = ""
        _showAnswerSheet.value = true
    }

    fun dismissAnswerSheet() {
        _showAnswerSheet.value = false
        _answerText.value = ""
    }

    fun setAnswerText(value: String) {
        _answerText.value = value
    }

    /**
     * [pickerMillisUtc] — сырое `DatePickerState.selectedDateMillis` (полночь UTC выбранного
     * дня, см. `PrayerAnswerDate`). Возвращает `true`, только если ответ реально сохранён —
     * экран закрывает шторку исключительно по этому сигналу, не по факту нажатия кнопки:
     * настоящая защита от пустого текста — в `PrayerActions.markAnswered`, блокировка кнопки
     * в UI — лишь подстраховка поверх неё.
     */
    fun saveAnswer(pickerMillisUtc: Long): Boolean {
        val request = currentRequest() ?: return false
        val answeredAt = PrayerAnswerDate.resolveAnsweredAt(pickerMillisUtc)
        val updated = PrayerActions.markAnswered(request, _answerText.value, answeredAt) ?: return false
        viewModelScope.launch { repository.saveRequest(updated) }
        _showAnswerSheet.value = false
        _answerText.value = ""
        return true
    }

    fun entrust() {
        val request = currentRequest() ?: return
        viewModelScope.launch { repository.saveRequest(PrayerActions.markEntrusted(request)) }
    }

    /** Каскадное удаление дописок обеспечивает `ON DELETE CASCADE` на `prayer_entries` (14.1). */
    fun delete() {
        val request = currentRequest() ?: return
        viewModelScope.launch { repository.deleteRequest(request) }
    }

    private fun currentRequest(): PrayerRequest? =
        (uiState.value as? PrayerDetailUiState.Loaded)?.request
}
