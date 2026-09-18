package ch.ncavallini.polywear.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SemesterUtilTest {

    @Test
    fun `autumn dates map to winter semester of same year`() {
        assertEquals("2026W", SemesterUtil.semkezFor(LocalDate.of(2026, 9, 15)))
        assertEquals("2026W", SemesterUtil.semkezFor(LocalDate.of(2026, 12, 1)))
    }

    @Test
    fun `january belongs to the winter semester that started last year`() {
        assertEquals("2026W", SemesterUtil.semkezFor(LocalDate.of(2027, 1, 15)))
        assertEquals("2025W", SemesterUtil.semkezFor(LocalDate.of(2026, 2, 10)))
    }

    @Test
    fun `spring and summer dates map to summer semester`() {
        assertEquals("2026S", SemesterUtil.semkezFor(LocalDate.of(2026, 2, 20)))
        assertEquals("2026S", SemesterUtil.semkezFor(LocalDate.of(2026, 5, 1)))
    }

    @Test
    fun `early september before winter start is still summer semester`() {
        assertEquals("2026S", SemesterUtil.semkezFor(LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun `week bounds are monday to sunday and contain the date`() {
        val date = LocalDate.of(2026, 9, 15)
        val start = SemesterUtil.weekStart(date)
        val end = SemesterUtil.weekEnd(date)

        assertEquals(DayOfWeek.MONDAY, start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, end.dayOfWeek)
        assertEquals(start.plusDays(6), end)
        assertTrue(!date.isBefore(start) && !date.isAfter(end))
    }
}
