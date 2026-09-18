package ru.edgarakert.biblenote.data.prayer

import ru.edgarakert.biblenote.data.db.PrayerCategory
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.util.Locale

/**
 * Фильтр экрана «Все просьбы» (задача 14.9). `ENTRUSTED` и `ARCHIVED` не имеют своей вкладки —
 * они видны только под [ALL], это продуктовое требование (поправка 1 к плану): «Доверить Богу» —
 * не «отменено» и не «просрочено», прятать эти просьбы совсем было бы неверно.
 */
enum class PrayerListFilter { ACTIVE, ANSWERED, ALL }

/** Одна секция списка — категория и просьбы внутри неё в исходном порядке (`createdAt DESC` из DAO). */
data class PrayerListSection(
    val category: PrayerCategory,
    val requests: List<PrayerRequest>,
)

/**
 * Чистая функция «список просьб + фильтр + строка поиска → упорядоченные секции по категориям»
 * для экрана «Все просьбы» (задача 14.9, поправка 1 к плану). Вынесена из ViewModel, чтобы
 * покрыть юнит-тестами без Android-зависимостей — см. `PrayerListGroupingTest`.
 *
 * Порядок применения: сначала фильтр статуса, потом поиск ПОВЕРХ уже отфильтрованного списка —
 * значит просьба, подходящая под поиск, но невидимая под текущим фильтром статуса
 * (например, отвеченная просьба под вкладкой «Активные»), в результат не попадёт.
 *
 * Секции идут в порядке объявления [PrayerCategory] (`entries`, не отсортированный список),
 * пустые категории пропускаются. Внутри секции порядок входного списка сохраняется (`filter`
 * порядок не меняет), поэтому вызывающая сторона обязана передавать уже отсортированный по
 * `createdAt DESC` список (см. `PrayerDao.observeAllRequests`).
 */
object PrayerListGrouping {

    fun buildSections(
        requests: List<PrayerRequest>,
        filter: PrayerListFilter,
        query: String,
    ): List<PrayerListSection> {
        val byStatus = requests.filter { matchesFilter(it.status, filter) }

        val trimmedQuery = query.trim()
        val matched = if (trimmedQuery.isEmpty()) {
            byStatus
        } else {
            // Locale.ROOT — явно, а не полагаясь на дефолтную локаль устройства: на кириллице
            // разницы нет (в отличие от турецкого İ/I), но так регрессия при смене локали
            // устройства невозможна в принципе, а не «проверено и вроде работает».
            val needle = trimmedQuery.lowercase(Locale.ROOT)
            byStatus.filter { request ->
                request.title.lowercase(Locale.ROOT).contains(needle) ||
                    request.body.lowercase(Locale.ROOT).contains(needle)
            }
        }

        return PrayerCategory.entries
            .map { category -> PrayerListSection(category, matched.filter { it.category == category }) }
            .filter { it.requests.isNotEmpty() }
    }

    private fun matchesFilter(status: PrayerStatus, filter: PrayerListFilter): Boolean = when (filter) {
        PrayerListFilter.ACTIVE -> status == PrayerStatus.ACTIVE
        PrayerListFilter.ANSWERED -> status == PrayerStatus.ANSWERED
        PrayerListFilter.ALL -> true
    }
}
