package ru.edgarakert.biblenote.data.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.edgarakert.biblenote.data.db.PrayerCategory
import ru.edgarakert.biblenote.data.db.PrayerRequest
import ru.edgarakert.biblenote.data.db.PrayerStatus

class PrayerListGroupingTest {

    private fun req(
        title: String,
        body: String = "",
        category: PrayerCategory = PrayerCategory.OTHER,
        status: PrayerStatus = PrayerStatus.ACTIVE,
    ) = PrayerRequest(title = title, body = body, category = category, status = status)

    private fun titles(sections: List<PrayerListSection>): List<String> =
        sections.flatMap { it.requests }.map { it.title }

    @Test
    fun `active filter hides answered entrusted and archived`() {
        val requests = listOf(
            req("активная", status = PrayerStatus.ACTIVE),
            req("отвеченная", status = PrayerStatus.ANSWERED),
            req("доверенная", status = PrayerStatus.ENTRUSTED),
            req("архивная", status = PrayerStatus.ARCHIVED),
        )
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ACTIVE, "")
        assertEquals(listOf("активная"), titles(sections))
    }

    @Test
    fun `answered filter shows only answered`() {
        val requests = listOf(
            req("активная", status = PrayerStatus.ACTIVE),
            req("отвеченная", status = PrayerStatus.ANSWERED),
            req("доверенная", status = PrayerStatus.ENTRUSTED),
            req("архивная", status = PrayerStatus.ARCHIVED),
        )
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ANSWERED, "")
        assertEquals(listOf("отвеченная"), titles(sections))
    }

    @Test
    fun `entrusted and archived are visible only under all`() {
        val requests = listOf(
            req("доверенная", status = PrayerStatus.ENTRUSTED),
            req("архивная", status = PrayerStatus.ARCHIVED),
        )
        assertEquals(
            emptyList<String>(),
            titles(PrayerListGrouping.buildSections(requests, PrayerListFilter.ACTIVE, ""))
        )
        assertEquals(
            emptyList<String>(),
            titles(PrayerListGrouping.buildSections(requests, PrayerListFilter.ANSWERED, ""))
        )
        assertEquals(
            listOf("доверенная", "архивная"),
            titles(PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, ""))
        )
    }

    @Test
    fun `search is case-insensitive and matches title or body`() {
        val requests = listOf(
            req("Big Meeting", body = "nothing relevant"),
            req("Other", body = "About my new JOB today"),
        )
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "job")
        assertEquals(listOf("Other"), titles(sections))
    }

    @Test
    fun `search works with cyrillic in any case`() {
        val requests = listOf(
            req("Молитва", body = "Работа идёт хорошо"),
            req("Другое", body = "ничего похожего"),
        )
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "РАБОТА")
        assertEquals(listOf("Молитва"), titles(sections))
    }

    @Test
    fun `search is applied after the status filter`() {
        val requests = listOf(
            req("Work meeting", status = PrayerStatus.ACTIVE),
            req("Work anniversary", status = PrayerStatus.ANSWERED),
        )
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ACTIVE, "work")
        assertEquals(listOf("Work meeting"), titles(sections))
    }

    @Test
    fun `sections are ordered by enum declaration order and skip empty categories`() {
        val requests = listOf(
            req("other-1", category = PrayerCategory.OTHER),
            req("family-1", category = PrayerCategory.FAMILY),
            req("health-1", category = PrayerCategory.HEALTH),
        )
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "")
        assertEquals(
            listOf(PrayerCategory.FAMILY, PrayerCategory.HEALTH, PrayerCategory.OTHER),
            sections.map { it.category }
        )
    }

    @Test
    fun `section header title comes from the category itself`() {
        val requests = listOf(req("a", category = PrayerCategory.CHURCH))
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "")
        assertEquals(PrayerCategory.CHURCH, sections.single().category)
    }

    @Test
    fun `preserves createdAt desc order within a section`() {
        val requests = listOf(
            req("newest", category = PrayerCategory.WORK),
            req("middle", category = PrayerCategory.WORK),
            req("oldest", category = PrayerCategory.WORK),
        )
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "")
        assertEquals(listOf("newest", "middle", "oldest"), titles(sections))
    }

    @Test
    fun `empty query returns everything the filter allows`() {
        val requests = listOf(req("a"), req("b"))
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "")
        assertEquals(listOf("a", "b"), titles(sections))
    }

    @Test
    fun `blank query is treated as no search`() {
        val requests = listOf(req("a"), req("b"))
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "   ")
        assertEquals(listOf("a", "b"), titles(sections))
    }

    @Test
    fun `no matches yields no sections at all`() {
        val requests = listOf(req("a"), req("b"))
        val sections = PrayerListGrouping.buildSections(requests, PrayerListFilter.ALL, "zzz")
        assertEquals(emptyList<PrayerListSection>(), sections)
    }
}
