package ch.ncavallini.polywear.data

import ch.ncavallini.polywear.data.dto.LessonDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleMappingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Reference week: Mon 2026-09-14 .. Sun 2026-09-20. */
    private val refDate = LocalDate.of(2026, 9, 15)

    /** Trimmed real response (top-level array, UTC times, title map, locations array). */
    private val sample = """
        [
          { "id": 1, "type": "V", "code": "263-4640-00", "semkez": "2026W",
            "start": "2026-09-15T08:15:00Z", "end": "2026-09-15T10:00:00Z",
            "locations": ["HG E 1.2"], "title": { "de": "Network Security" },
            "assets": { "CLICKER": { "type": "CLICKER", "available": false } } },
          { "id": 2, "type": "V", "code": "263-3010-00", "semkez": "2026W",
            "start": "2026-09-15T12:15:00Z", "end": "2026-09-15T14:00:00Z",
            "locations": ["HG E 7"], "title": { "de": "Big Data" } },
          { "id": 3, "type": "U", "code": "263-3010-00", "semkez": "2026W",
            "start": "2026-09-16T12:15:00Z", "end": "2026-09-16T14:00:00Z",
            "locations": ["n / a"], "title": { "de": "Big Data" } },
          { "id": 4, "type": "V", "code": "263-4640-00", "semkez": "2026W",
            "start": "2026-09-22T08:15:00Z", "end": "2026-09-22T10:00:00Z",
            "locations": ["HG E 1.2"], "title": { "de": "Network Security" } }
        ]
    """.trimIndent()

    private fun decode(): List<LessonDto> = json.decodeFromString(sample)

    @Test
    fun `decodes real schema and maps fields`() {
        val events = ScheduleMapper.toEvents(decode())
        assertEquals(4, events.size)

        val networkSecurity = events.first { it.id == "1" }
        assertEquals("Network Security", networkSecurity.title)
        assertEquals("Lecture", networkSecurity.category)
        assertEquals("HG E 1.2", networkSecurity.location)

        val exercise = events.first { it.id == "3" }
        assertEquals("Exercise", exercise.category)
        assertNull("'n / a' locations should be dropped", exercise.location)
    }

    @Test
    fun `converts UTC to Europe Zurich local time across DST`() {
        // Summer (CEST, +2): 08:15Z -> 10:15 local.
        assertEquals(10, ScheduleMapper.parseDateTime("2026-09-15T08:15:00Z")!!.hour)
        assertEquals(15, ScheduleMapper.parseDateTime("2026-09-15T08:15:00Z")!!.minute)
        // Winter (CET, +1): 09:15Z -> 10:15 local.
        assertEquals(10, ScheduleMapper.parseDateTime("2026-12-01T09:15:00Z")!!.hour)
    }

    @Test
    fun `groups current week by day, sorted, excluding other weeks`() {
        val events = ScheduleMapper.toEvents(decode())
        val days = ScheduleRepository.groupWeek(events, refDate)

        // Tue 15th and Wed 16th only (22nd is next week).
        assertEquals(2, days.size)
        assertEquals(LocalDate.of(2026, 9, 15), days[0].date)
        assertEquals(LocalDate.of(2026, 9, 16), days[1].date)

        // Tuesday sorted by start: Network Security (10:15) before Big Data (14:15).
        assertEquals(listOf("Network Security", "Big Data"), days[0].events.map { it.title })
    }

    @Test
    fun `parses epoch seconds and millis equivalently`() {
        val seconds = ScheduleMapper.parseDateTime("1789891200")
        val millis = ScheduleMapper.parseDateTime("1789891200000")
        assertTrue(seconds != null && millis != null)
        assertEquals(seconds, millis)
    }
}
