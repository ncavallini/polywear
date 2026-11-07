package ch.ncavallini.polywear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import ch.ncavallini.polywear.PolyApp
import ch.ncavallini.polywear.MainActivity
import ch.ncavallini.polywear.data.model.ScheduleEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shows the next class (one that has not started yet) on the watch face. Reads only the
 * cached schedule — the schedule is refreshed by the app, not here. Tapping the
 * complication opens the app.
 *
 * Refreshes are scheduled by [ComplicationUpdateScheduler] (hourly at :40,
 * 06:40–20:40) rather than by the system's periodic update.
 *
 * Supports SHORT_TEXT and LONG_TEXT.
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

            else -> null
        }
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        ComplicationUpdateScheduler.onActivated(this, complicationInstanceId)
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        ComplicationUpdateScheduler.onDeactivated(this, complicationInstanceId)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        // Keeps the :40 refresh chain armed (also for instances added before it existed).
        ComplicationUpdateScheduler.onActivated(this, request.complicationInstanceId)
        val now = LocalDateTime.now(zone)
        val event = (application as PolyApp).container.scheduleRepository.nextEventFromCache(now)
        return build(request.complicationType, event)
    }

    private fun build(type: ComplicationType, event: ScheduleEvent?): ComplicationData? {
        val tap = tapAction()
        if (event == null) {
            return when (type) {
                ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                    text = text("—"), contentDescription = text("Open ETH Schedule"),
                ).setTapAction(tap).build()

                ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                    text = text("No upcoming classes"), contentDescription = text("Open ETH Schedule"),
                ).setTapAction(tap).build()

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
