package ru.edgarakert.biblenote.data.bible

import java.util.regex.Pattern

class BibleReferenceParser {

    private val bookAliases: Map<String, Int> = mapOf(
        // ── Russian Old Testament ──────────────────────────────────
        "быт" to 1, "бытие" to 1,
        "исх" to 2, "исход" to 2,
        "лев" to 3, "левит" to 3,
        "чис" to 4, "числа" to 4, "числ" to 4,
        "втор" to 5, "второзаконие" to 5,
        "нав" to 6, "иисус навин" to 6,
        "суд" to 7, "судей" to 7, "судьи" to 7,
        "руф" to 8, "руфь" to 8,
        "1цар" to 9, "1 цар" to 9, "1царств" to 9, "1 царств" to 9,
        "2цар" to 10, "2 цар" to 10, "2царств" to 10, "2 царств" to 10,
        "3цар" to 11, "3 цар" to 11, "3царств" to 11, "3 царств" to 11,
        "4цар" to 12, "4 цар" to 12, "4царств" to 12, "4 царств" to 12,
        "1пар" to 13, "1 пар" to 13, "1паралипоменон" to 13, "1 паралипоменон" to 13,
        "2пар" to 14, "2 пар" to 14, "2паралипоменон" to 14, "2 паралипоменон" to 14,
        "езд" to 15, "ездра" to 15,
        "неем" to 16, "неемия" to 16,
        "есф" to 17, "есфирь" to 17,
        "иов" to 18,
        "пс" to 19, "псалтирь" to 19, "псалм" to 19,
        "прит" to 20, "притчи" to 20, "притч" to 20,
        "екк" to 21, "екклесиаст" to 21, "еккл" to 21,
        "песн" to 22, "песня" to 22, "песня песней" to 22,
        "ис" to 23, "исаия" to 23,
        "иер" to 24, "иеремия" to 24,
        "плач" to 25, "плач иеремии" to 25,
        "иез" to 26, "иезекииль" to 26,
        "дан" to 27, "даниил" to 27,
        "ос" to 28, "осия" to 28,
        "иоил" to 29, "иоиль" to 29,
        "ам" to 30, "амос" to 30,
        "авд" to 31, "авдий" to 31,
        "ион" to 32, "иона" to 32,
        "мих" to 33, "михей" to 33,
        "наум" to 34,
        "авв" to 35, "аввакум" to 35,
        "соф" to 36, "софия" to 36, "софония" to 36,
        "агг" to 37, "аггей" to 37,
        "зах" to 38, "захария" to 38,
        "мал" to 39, "малахия" to 39,
        // ── Russian New Testament ──────────────────────────────────
        "мф" to 40, "матф" to 40, "матфея" to 40, "матфей" to 40,
        "мк" to 41, "мар" to 41, "марка" to 41, "марк" to 41,
        "лк" to 42, "луки" to 42, "лука" to 42,
        "ин" to 43, "иоанна" to 43, "иоан" to 43, "иоанн" to 43,
        "деян" to 44, "деяния" to 44,
        "рим" to 45, "римлянам" to 45,
        "1кор" to 46, "1 кор" to 46, "1коринфянам" to 46, "1 коринфянам" to 46,
        "2кор" to 47, "2 кор" to 47, "2коринфянам" to 47, "2 коринфянам" to 47,
        "гал" to 48, "галатам" to 48,
        "еф" to 49, "ефесянам" to 49,
        "флп" to 50, "фил" to 50, "филиппийцам" to 50,
        "кол" to 51, "колоссянам" to 51,
        "1фес" to 52, "1 фес" to 52, "1фессалоникийцам" to 52, "1 фессалоникийцам" to 52,
        "2фес" to 53, "2 фес" to 53, "2фессалоникийцам" to 53, "2 фессалоникийцам" to 53,
        "1тим" to 54, "1 тим" to 54, "1тимофею" to 54, "1 тимофею" to 54,
        "2тим" to 55, "2 тим" to 55, "2тимофею" to 55, "2 тимофею" to 55,
        "тит" to 56, "титу" to 56,
        "флм" to 57, "филимону" to 57,
        "евр" to 58, "евреям" to 58,
        "иак" to 59, "иакова" to 59,
        "1пет" to 60, "1 пет" to 60, "1петра" to 60, "1 петра" to 60,
        "2пет" to 61, "2 пет" to 61, "2петра" to 61, "2 петра" to 61,
        "1ин" to 62, "1 ин" to 62, "1иоанна" to 62, "1 иоанна" to 62,
        "2ин" to 63, "2 ин" to 63, "2иоанна" to 63, "2 иоанна" to 63,
        "3ин" to 64, "3 ин" to 64, "3иоанна" to 64, "3 иоанна" to 64,
        "иуд" to 65, "иуды" to 65, "иуда" to 65,
        "откр" to 66, "откровение" to 66,
        // ── English Old Testament ──────────────────────────────────
        "genesis" to 1, "gen" to 1,
        "exodus" to 2, "exod" to 2, "ex" to 2,
        "leviticus" to 3,
        "numbers" to 4, "num" to 4,
        "deuteronomy" to 5, "deut" to 5,
        "joshua" to 6, "josh" to 6,
        "judges" to 7, "judg" to 7,
        "ruth" to 8,
        "1 samuel" to 9, "1 sam" to 9, "1sam" to 9,
        "2 samuel" to 10, "2 sam" to 10, "2sam" to 10,
        "1 kings" to 11, "1 kgs" to 11, "1kgs" to 11,
        "2 kings" to 12, "2 kgs" to 12, "2kgs" to 12,
        "1 chronicles" to 13, "1 chr" to 13, "1chr" to 13,
        "2 chronicles" to 14, "2 chr" to 14, "2chr" to 14,
        "ezra" to 15,
        "nehemiah" to 16, "neh" to 16,
        "esther" to 17, "esth" to 17,
        "job" to 18,
        "psalms" to 19, "psalm" to 19, "psa" to 19,
        "proverbs" to 20, "prov" to 20,
        "ecclesiastes" to 21, "eccl" to 21,
        "song of solomon" to 22, "song of songs" to 22, "song" to 22,
        "isaiah" to 23, "isa" to 23,
        "jeremiah" to 24, "jer" to 24,
        "lamentations" to 25, "lam" to 25,
        "ezekiel" to 26, "ezek" to 26,
        "daniel" to 27, "dan" to 27,
        "hosea" to 28, "hos" to 28,
        "joel" to 29,
        "amos" to 30,
        "obadiah" to 31, "obad" to 31,
        "jonah" to 32,
        "micah" to 33, "mic" to 33,
        "nahum" to 34, "nah" to 34,
        "habakkuk" to 35, "hab" to 35,
        "zephaniah" to 36, "zeph" to 36,
        "haggai" to 37, "hag" to 37,
        "zechariah" to 38, "zech" to 38,
        "malachi" to 39,
        // ── English New Testament ──────────────────────────────────
        "matthew" to 40, "matt" to 40,
        "mark" to 41,
        "luke" to 42,
        "john" to 43, "jn" to 43,
        "acts" to 44,
        "romans" to 45, "rom" to 45,
        "1 corinthians" to 46, "1 cor" to 46, "1cor" to 46,
        "2 corinthians" to 47, "2 cor" to 47, "2cor" to 47,
        "galatians" to 48,
        "ephesians" to 49, "eph" to 49,
        "philippians" to 50, "phil" to 50,
        "colossians" to 51,
        "1 thessalonians" to 52, "1 thess" to 52, "1thess" to 52,
        "2 thessalonians" to 53, "2 thess" to 53, "2thess" to 53,
        "1 timothy" to 54, "1 tim" to 54, "1tim" to 54,
        "2 timothy" to 55, "2 tim" to 55, "2tim" to 55,
        "titus" to 56,
        "philemon" to 57, "phlm" to 57,
        "hebrews" to 58, "heb" to 58,
        "james" to 59, "jas" to 59,
        "1 peter" to 60, "1 pet" to 60, "1pet" to 60,
        "2 peter" to 61, "2 pet" to 61, "2pet" to 61,
        "1 john" to 62, "1jn" to 62, "1 jn" to 62,
        "2 john" to 63, "2jn" to 63, "2 jn" to 63,
        "3 john" to 64, "3jn" to 64, "3 jn" to 64,
        "jude" to 65,
        "revelation" to 66, "rev" to 66
    )

    private val pattern: Pattern

    init {
        // Longest aliases first so e.g. "1 Кор" is preferred over "Кор"
        val sorted = bookAliases.keys.sortedByDescending { it.length }
        val alternation = sorted.joinToString("|") { Pattern.quote(it) }
        // (?<!\p{L}) — not preceded by any Unicode letter (avoids matching inside words)
        val patternStr = "(?<!\\p{L})($alternation)\\s*\\.?\\s*(\\d+)(?::(\\d+)(?:-(\\d+))?)?"
        pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
    }

    fun parse(text: String): List<BibleReference> {
        if (text.isEmpty()) return emptyList()
        val matcher = pattern.matcher(text)
        val results = mutableListOf<BibleReference>()
        while (matcher.find()) {
            val bookText = matcher.group(1)?.lowercase() ?: continue
            val bookId = bookAliases[bookText] ?: continue
            val chapter = matcher.group(2)?.toIntOrNull() ?: continue
            val verseStart = matcher.group(3)?.toIntOrNull()
            val verseEnd = matcher.group(4)?.toIntOrNull()
            results.add(
                BibleReference(
                    bookId = bookId,
                    chapter = chapter,
                    verseStart = verseStart,
                    verseEnd = verseEnd,
                    displayText = text.substring(matcher.start(), matcher.end()),
                    startIndex = matcher.start(),
                    endIndex = matcher.end()
                )
            )
        }
        return results
    }
}
