package ru.edgarakert.biblenote.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PrayerCategory { FAMILY, CHURCH, HEALTH, WORK, GRATITUDE, PERSONAL, WORLD, OTHER }

/**
 * ENTRUSTED — «доверить Богу». Это не «отменено» и не «просрочено».
 * ARCHIVED объявлен для совместимости со схемой iOS, но ничем не выставляется.
 */
enum class PrayerStatus { ACTIVE, ANSWERED, ENTRUSTED, ARCHIVED }

@Entity(tableName = "prayer_requests")
data class PrayerRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    /** Сохраняется без trim — в отличие от title, чтобы абзацы пользователя не съедались. */
    val body: String = "",
    val category: PrayerCategory = PrayerCategory.OTHER,
    val status: PrayerStatus = PrayerStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val answeredAt: Long? = null,
    val answerText: String? = null,
    /** Сырые строки ссылок ("Флп 4:6-7"); разбираются BibleReferenceParser при показе. */
    val verseRefs: List<String> = emptyList(),
    val lastPrayedAt: Long? = null,
    /** Количество РАЗНЫХ дней, когда нажимали «помолился». Это не серия подряд. */
    @ColumnInfo(defaultValue = "0") val prayedDaysCount: Int = 0,
)
