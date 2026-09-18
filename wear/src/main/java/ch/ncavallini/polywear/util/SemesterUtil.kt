package ch.ncavallini.polywear.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Maps calendar dates to ETH's `semkez` semester code and computes week bounds.
 *
 * `semkez` = `YYYY` + `S` (Summer / Frühjahrssemester) or `W` (Winter /
 * Herbstsemester). The Winter semester starts in autumn and carries into the
 * next calendar year, so its `semkez` year is the year it *started*
 * (e.g. January 2027 belongs to `2026W`).
 *
 * The month/day cut-offs below are approximate — verify against the live app in
 * Phase 0 and adjust the two boundary constants if needed.
 */
object SemesterUtil {
    /** Winter semester begins around this day in September. */
    private const val WINTER_START_MONTH = 9
    private const val WINTER_START_DAY = 15

    /** Summer semester begins around this day in February. */
    private const val SUMMER_START_MONTH = 2
    private const val SUMMER_START_DAY = 15

    fun semkezFor(date: LocalDate): String {
        val year = date.year
        val afterWinterStart =
            date.monthValue > WINTER_START_MONTH ||
                (date.monthValue == WINTER_START_MONTH && date.dayOfMonth >= WINTER_START_DAY)
        val beforeSummerStart =
            date.monthValue < SUMMER_START_MONTH ||
                (date.monthValue == SUMMER_START_MONTH && date.dayOfMonth < SUMMER_START_DAY)

        return when {
            // mid-Sept .. Dec -> this year's winter semester
            afterWinterStart -> "${year}W"
            // Jan .. mid-Feb -> winter semester that started the previous year
            beforeSummerStart -> "${year - 1}W"
            // mid-Feb .. mid-Sept -> this year's summer semester
            else -> "${year}S"
        }
    }

    /** Monday of the ISO week containing [date]. */
    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Sunday of the ISO week containing [date]. */
    fun weekEnd(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
}
