package ch.ncavallini.polywear.complication

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ComplicationUpdateSchedulerTest {

    private val zurich = ZoneId.of("Europe/Zurich")

    private fun at(day: Int, hour: Int, minute: Int, month: Int = 11) =
        ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zurich)

    private fun next(now: ZonedDateTime) = ComplicationUpdateScheduler.nextUpdateAfter(now)

    @Test
    fun `before the window waits for 06_40 the same day`() {
        assertEquals(at(9, 6, 40), next(at(9, 0, 5)))
        assertEquals(at(9, 6, 40), next(at(9, 5, 59)))
        assertEquals(at(9, 6, 40), next(at(9, 6, 10)))
    }

    @Test
    fun `inside the window picks the next 40 past the hour`() {
        assertEquals(at(9, 9, 40), next(at(9, 9, 15)))
        assertEquals(at(9, 10, 40), next(at(9, 9, 40)))
        assertEquals(at(9, 10, 40), next(at(9, 9, 41)))
        assertEquals(at(9, 20, 40), next(at(9, 20, 0)))
    }

    @Test
    fun `after the last update waits for 06_40 the next day`() {
        assertEquals(at(10, 6, 40), next(at(9, 20, 40)))
        assertEquals(at(10, 6, 40), next(at(9, 23, 59)))
        assertEquals(at(1, 6, 40, month = 12), next(at(30, 21, 0)))
    }

    @Test
    fun `input in another zone is evaluated in Zurich time`() {
        val utc = ZonedDateTime.of(2026, 11, 9, 8, 50, 0, 0, ZoneId.of("UTC")) // 09:50 Zurich
        assertEquals(at(9, 10, 40), next(utc).withZoneSameInstant(zurich))
    }
}
