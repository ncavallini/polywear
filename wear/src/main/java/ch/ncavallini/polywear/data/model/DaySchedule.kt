package ch.ncavallini.polywear.data.model

import java.time.LocalDate
import java.time.LocalDateTime

/** A single class/lesson, already parsed into local (Europe/Zurich) date-time. */
data class ScheduleEvent(
    val id: String,
    val title: String,
    /** Course code, e.g. "263-3010-00" — used to color-group all lessons of a course. */
    val code: String?,
    /** Human-readable lesson type: Lecture / Exercise / Lab / Seminar, or raw code. */
    val category: String?,
    val start: LocalDateTime,
    val end: LocalDateTime?,
    val location: String?,
)

/** All events on one day, sorted by start time. */
data class DaySchedule(
    val date: LocalDate,
    val events: List<ScheduleEvent>,
)
