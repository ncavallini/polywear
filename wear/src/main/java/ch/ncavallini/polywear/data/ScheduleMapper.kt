package ch.ncavallini.polywear.data

import ch.ncavallini.polywear.data.dto.LessonDto
import ch.ncavallini.polywear.data.model.ScheduleEvent
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Maps the eduapp [LessonDto]s into UI [ScheduleEvent]s (times in Europe/Zurich). */
object ScheduleMapper {

    private val zone: ZoneId = ZoneId.of("Europe/Zurich")

    fun toEvents(lessons: List<LessonDto>): List<ScheduleEvent> = lessons.mapNotNull { it.toEvent() }

    private fun LessonDto.toEvent(): ScheduleEvent? {
        val startDt = start?.let(::parseDateTime) ?: return null
        val endDt = end?.let(::parseDateTime)
        val name = title["en"] ?: title["de"] ?: title.values.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Untitled"
        val location = locations
            .filter { it.isNotBlank() && !it.equals("n / a", ignoreCase = true) }
            .joinToString(", ")
            .ifBlank { null }
        return ScheduleEvent(
            id = id?.toString() ?: "$startDt-$name",
            title = name,
            code = code,
            category = categoryLabel(type),
            start = startDt,
            end = endDt,
            location = location,
        )
    }

    private fun categoryLabel(type: String?): String? = when (type?.uppercase()) {
        "V" -> "Lecture"
        "U" -> "Exercise"
        "P" -> "Lab"
        "S" -> "Seminar"
        "G" -> "Group"
        else -> type
    }

    /** Parses ISO-8601 (offset/`Z` or local) or epoch seconds/millis into Europe/Zurich local time. */
    fun parseDateTime(raw: String): LocalDateTime? {
        raw.toLongOrNull()?.let { epoch ->
            val instant = if (raw.length > 12) Instant.ofEpochMilli(epoch) else Instant.ofEpochSecond(epoch)
            return LocalDateTime.ofInstant(instant, zone)
        }
        runCatching { return OffsetDateTime.parse(raw).atZoneSameInstant(zone).toLocalDateTime() }
        runCatching { return LocalDateTime.parse(raw) }
        runCatching {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        }
        runCatching {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        }
        return null
    }
}
