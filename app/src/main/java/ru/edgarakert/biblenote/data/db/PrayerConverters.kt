package ru.edgarakert.biblenote.data.db

import androidx.room.TypeConverter

class PrayerConverters {
    /** Разделитель — перевод строки: в ссылке на стих он встретиться не может. */
    @TypeConverter
    fun verseRefsToString(refs: List<String>): String = refs.joinToString("\n")

    @TypeConverter
    fun stringToVerseRefs(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split("\n")

    @TypeConverter fun categoryToString(c: PrayerCategory): String = c.name
    @TypeConverter fun stringToCategory(v: String): PrayerCategory = PrayerCategory.valueOf(v)
    @TypeConverter fun statusToString(s: PrayerStatus): String = s.name
    @TypeConverter fun stringToStatus(v: String): PrayerStatus = PrayerStatus.valueOf(v)
}
