package ch.ncavallini.polywear.data

import ch.ncavallini.polywear.data.dto.LessonDto
import ch.ncavallini.polywear.data.model.DaySchedule
import ch.ncavallini.polywear.data.model.ScheduleEvent
import ch.ncavallini.polywear.util.SemesterUtil
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

sealed interface ScheduleResult {
    data class Success(val days: List<DaySchedule>, val fromCache: Boolean) : ScheduleResult
    data object Unauthorized : ScheduleResult
    data class Error(val throwable: Throwable, val cachedDays: List<DaySchedule>?) : ScheduleResult
}

/**
 * Fetches the semester schedule, filters it to the current week and groups it by
 * day. Falls back to the cached copy on network errors, and surfaces HTTP 401 as
 * [ScheduleResult.Unauthorized] so the UI can prompt for re-auth on the phone.
 */
class ScheduleRepository(
    private val api: ScheduleApi,
    private val cache: ScheduleCache,
    private val json: Json,
    private val today: () -> LocalDate = { LocalDate.now(ZoneId.of("Europe/Zurich")) },
    /** Invoked after a successful fetch so dependents (e.g. complications) can refresh. */
    private val onDataUpdated: () -> Unit = {},
) {
    private val lessonListSerializer = ListSerializer(LessonDto.serializer())

    suspend fun getCurrentWeek(): ScheduleResult = getWeek(today())

    /**
     * Fetches the semester containing [refDate] and returns the Mon–Sun week
     * around it. Since a whole semester is fetched at once, navigating between
     * weeks of the same semester can be served from the cache via [cachedWeek]
     * without a network round-trip.
     */
    suspend fun getWeek(refDate: LocalDate): ScheduleResult {
        val date = refDate
        val semkez = SemesterUtil.semkezFor(date)
        return try {
            val lessons = api.getSchedule(semkez)
            cache.save(semkez, json.encodeToString(lessonListSerializer, lessons))
            onDataUpdated()
            ScheduleResult.Success(groupWeek(ScheduleMapper.toEvents(lessons), date), fromCache = false)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                ScheduleResult.Unauthorized
            } else {
                ScheduleResult.Error(e, loadCached(semkez, date))
            }
        } catch (e: IOException) {
            ScheduleResult.Error(e, loadCached(semkez, date))
        }
    }

    /**
     * The next upcoming (or currently ongoing) event, read only from the cache —
     * safe to call from a complication/tile where no network/auth is available.
     * Returns null if nothing is cached yet.
     */
    suspend fun nextEventFromCache(now: LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/Zurich"))): ScheduleEvent? {
        val semkez = SemesterUtil.semkezFor(now.toLocalDate())
        val entry = cache.load(semkez) ?: return null
        val events = runCatching {
            ScheduleMapper.toEvents(json.decodeFromString(lessonListSerializer, entry.rawJson))
        }.getOrDefault(emptyList())
        return events
            .filter { (it.end ?: it.start) >= now }
            .minByOrNull { it.start }
    }

    /**
     * The Mon–Sun week around [refDate] grouped from the cached semester, without
     * touching the network. Returns null when that semester isn't cached yet.
     * Used to move between weeks of an already-loaded semester instantly.
     */
    suspend fun cachedWeek(refDate: LocalDate): List<DaySchedule>? =
        loadCached(SemesterUtil.semkezFor(refDate), refDate)

    private suspend fun loadCached(semkez: String, date: LocalDate): List<DaySchedule>? =
        cache.load(semkez)?.let { entry ->
            runCatching {
                val lessons = json.decodeFromString(lessonListSerializer, entry.rawJson)
                groupWeek(ScheduleMapper.toEvents(lessons), date)
            }.getOrNull()
        }

    companion object {
        /** Filters [events] to the Mon–Sun week containing [refDate] and groups by day. */
        fun groupWeek(events: List<ScheduleEvent>, refDate: LocalDate): List<DaySchedule> {
            val start = SemesterUtil.weekStart(refDate)
            val end = SemesterUtil.weekEnd(refDate)
            return events
                .filter { event ->
                    val d = event.start.toLocalDate()
                    !d.isBefore(start) && !d.isAfter(end)
                }
                .groupBy { it.start.toLocalDate() }
                .toSortedMap()
                .map { (day, dayEvents) -> DaySchedule(day, dayEvents.sortedBy { it.start }) }
        }
    }
}
