package ru.edgarakert.biblenote.data.prayer

import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus
import java.time.ZoneId

/**
 * Что показывать на экране «Сегодня».
 *
 * Ограничения по количеству НЕТ: показывается весь активный список, где просьбы,
 * за которые сегодня ещё не молились, идут выше уже помоленных. Нажатие «Помолился»
 * не убирает карточку — она просто переезжает в нижнюю группу.
 */
object PrayerSelectionService {

    fun todaysSelection(
        requests: List<PrayerRequest>,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<PrayerRequest> = requests
        .filter { it.status == PrayerStatus.ACTIVE }
        // sortedBy устойчива: порядок внутри каждой группы сохраняется — это часть контракта.
        .sortedBy { if (PrayerActions.hasPrayedToday(it, now, zone)) 1 else 0 }
}
