package ru.edgarakert.biblenote.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.edgarakert.biblenote.data.PrayerRepository
import ru.edgarakert.biblenote.data.bible.BibleReferenceParser
import ru.edgarakert.biblenote.data.db.PrayerCategory
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.prayer.PrayerVerseRefs

/**
 * Один экран на создание и редактирование просьбы (задача 14.11): различаются по [requestId],
 * `-1L` — создание. Поля живут здесь, а не в `rememberSaveable` на экране, — так они переживают
 * и поворот экрана, и (если когда-нибудь понадобится) пересоздание Composable по другой причине.
 *
 * Загрузка при редактировании отражена в [isLoaded]: пока существующая просьба не пришла из
 * репозитория, экран не должен показывать пустые поля, которые пользователь успеет заполнить,
 * а потом их затрёт загрузка. Поля инициализируются из загруженной просьбы ровно один раз — в
 * [init], который выполняется один раз за жизнь ViewModel.
 */
class PrayerEditorViewModel(
    private val requestId: Long,
    private val repository: PrayerRepository,
    private val parser: BibleReferenceParser,
) : ViewModel() {

    private val _isLoaded = MutableStateFlow(requestId == -1L)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /**
     * `true`, когда открыли редактор по [requestId], а `getRequestById` не нашёл такую просьбу —
     * например, её удалили (из карточки просьбы 14.10 или откуда-то ещё) между тапом на неё и
     * загрузкой редактора. Без этой проверки экран тихо показывал бы пустую форму под заголовком
     * «Изменить просьбу», а «Сохранить» создало бы новую просьбу вместо ожидаемого редактирования
     * (поправка 3 к плану 14.10). Экран должен закрыться, а не показывать эту пустую форму.
     */
    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private val _category = MutableStateFlow(PrayerCategory.OTHER)
    val category: StateFlow<PrayerCategory> = _category.asStateFlow()

    private val _versesInput = MutableStateFlow("")
    val versesInput: StateFlow<String> = _versesInput.asStateFlow()

    /** «Сохранить» включается только когда заголовок непуст после trim — единственное обязательное поле. */
    private val _canSave = MutableStateFlow(false)
    val canSave: StateFlow<Boolean> = _canSave.asStateFlow()

    private val _saved = MutableSharedFlow<Unit>()
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    /** Просьба, загруженная для редактирования — её нетронутые поля (createdAt, status, счётчики…) переносятся при сохранении как есть. */
    private var existing: PrayerRequest? = null

    init {
        if (requestId != -1L) {
            viewModelScope.launch {
                val loaded = repository.getRequestById(requestId)
                existing = loaded
                if (loaded != null) {
                    _title.value = loaded.title
                    _body.value = loaded.body
                    _category.value = loaded.category
                    _versesInput.value = PrayerVerseRefs.toInput(loaded.verseRefs)
                    _canSave.value = loaded.title.trim().isNotEmpty()
                } else {
                    _notFound.value = true
                }
                _isLoaded.value = true
            }
        }
    }

    fun setTitle(value: String) {
        _title.value = value
        _canSave.value = value.trim().isNotEmpty()
    }

    fun setBody(value: String) {
        _body.value = value
    }

    fun setCategory(value: PrayerCategory) {
        _category.value = value
    }

    fun setVersesInput(value: String) {
        _versesInput.value = value
    }

    /**
     * `title` сохраняется обрезанным, `body` — как есть, без trim (иначе абзацы пользователя
     * съедаются). Стихи разбираются через [PrayerVerseRefs.fromInput], не по запятой напрямую —
     * см. комментарий в `PrayerVerseRefs`. При редактировании — `existing.copy(...)`, чтобы
     * `createdAt`/`status`/`answeredAt`/`answerText`/`lastPrayedAt`/`prayedDaysCount` не трогались.
     * Сохранение — через `PrayerRepository.saveRequest` (`@Upsert`), не через `INSERT OR REPLACE`
     * (см. комментарий в `PrayerDao.upsertRequest`: REPLACE стёр бы дописки просьбы).
     */
    fun save() {
        val trimmedTitle = _title.value.trim()
        if (trimmedTitle.isEmpty()) return

        val verseRefs = PrayerVerseRefs.fromInput(_versesInput.value, parser)
        val base = existing ?: PrayerRequest()
        val toSave = base.copy(
            title = trimmedTitle,
            body = _body.value,
            category = _category.value,
            verseRefs = verseRefs,
        )

        viewModelScope.launch {
            repository.saveRequest(toSave)
            _saved.emit(Unit)
        }
    }
}
