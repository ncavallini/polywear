package ch.ncavallini.polywear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import ch.ncavallini.polywear.PolyApp
import ch.ncavallini.polywear.MainActivity
import ch.ncavallini.polywear.data.model.ScheduleEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Shows the next (or currently ongoing) class on the watch face. Reads only the
 * cached schedule — the schedule is refreshed by the app, not here. Tapping the
 * complication opens the app.
 *
 * Supports SHORT_TEXT, LONG_TEXT and RANGED_VALUE (countdown).
 */
class NextClassComplicationService : SuspendingComplicationDataSourceService() {

    private val zone = ZoneId.of("Europe/Zurich")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val room = "HG E 5"
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = text(room),
                contentDescription = text("Next class"),
            ).setTitle(text("Big Data")).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = text("09:15 · $room"),
                contentDescription = text("Next class"),
            ).setTitle(text("Big Data")).build()

            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                value = 135f, min = 0f, max = 180f,
                contentDescription = text("Next class"),
            ).setText(text(room)).setTitle(text("Big Data")).build()

            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val now = LocalDateTime.now(zone)
        val event = (application as PolyApp).container.scheduleRepository.nextEventFromCache(now)
        return build(request.complicationType, event, now)
    }

    private fun build(type: ComplicationType, event: ScheduleEvent?, now: LocalDateTime): ComplicationData? {
        val tap = tapAction()
        if (event == null) {
            return when (type) {
                ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                    text = text("—"), contentDescription = text("Open ETH Schedule"),
                ).setTapAction(tap).build()

                ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                    text = text("No upcoming classes"), contentDescription = text("Open ETH Schedule"),
                ).setTapAction(tap).build()

                ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(
                    value = 0f, min = 0f, max = 1f, contentDescription = text("Open ETH Schedule"),
                ).setText(text("—")).setTapAction(tap).build()

                else -> null
            }
        }

        val time = event.start.format(timeFormat)
        // Primary text is the room; fall back to the start time if no room is listed.
        val place = event.location ?: time
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = text(place),
                contentDescription = text("Next: ${event.title} at $time in ${event.location ?: "?"}"),
            ).setTitle(text(event.title)).setTapAction(tap).build()

            ComplicationType.LONG_TEXT -> {
                val detail = listOfNotNull(time, event.location).joinToString(" · ")
                LongTextComplicationData.Builder(
                    text = text(detail),
                    contentDescription = text("Next: ${event.title} at $time"),
                ).setTitle(text(event.title)).setTapAction(tap).build()
            }

            ComplicationType.RANGED_VALUE -> {
                val end = event.end ?: event.start.plusHours(1)
                val ongoing = !now.isBefore(event.start) && now.isBefore(end)
                // Arc = how soon (or how far into an ongoing class); text = the class time.
                val (value, max) = if (ongoing) {
                    val total = ChronoUnit.MINUTES.between(event.start, end).coerceAtLeast(1)
                    ChronoUnit.MINUTES.between(event.start, now).toFloat() to total.toFloat()
                } else {
                    val window = 180L
                    val mins = ChronoUnit.MINUTES.between(now, event.start).coerceIn(0, window)
                    (window - mins).toFloat() to window.toFloat()
                }
                RangedValueComplicationData.Builder(
                    value = value, min = 0f, max = max,
                    contentDescription = text("Next: ${event.title} at $time in ${event.location ?: "?"}"),
                ).setText(text(place)).setTitle(text(event.title)).setTapAction(tap).build()
            }

            else -> null
        }
    }

    private fun text(value: String): ComplicationText =
        PlainComplicationText.Builder(value).build()

    private fun tapAction(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
