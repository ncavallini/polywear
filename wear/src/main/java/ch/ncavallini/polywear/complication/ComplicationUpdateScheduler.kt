package ch.ncavallini.polywear.complication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Drives the next-class complication refreshes instead of the system's periodic
 * `UPDATE_PERIOD_SECONDS` wake-up (which is disabled in the manifest).
 *
 * ETH classes follow a fixed hourly grid, so one refresh per hour is enough to
 * keep "next class" current. Refreshes run at :40 from [FIRST_HOUR]:40 to
 * [LAST_HOUR]:40 (Zurich time); outside that window nothing wakes up until the
 * next morning.
 *
 * The chain only runs while at least one instance of the complication is on a
 * watch face: instance ids are tracked in [onActivated]/[onDeactivated], and
 * the alarm is not re-armed once the last one is removed.
 */
object ComplicationUpdateScheduler {

    const val FIRST_HOUR = 6
    const val LAST_HOUR = 20
    const val MINUTE = 40

    private val zone: ZoneId = ZoneId.of("Europe/Zurich")

    private const val PREFS = "complication_scheduler"
    private const val KEY_ACTIVE_IDS = "active_ids"
    private const val REQUEST_CODE = 4040

    /** The next refresh strictly after [now]: the next :40 within FIRST_HOUR..LAST_HOUR. */
    fun nextUpdateAfter(now: ZonedDateTime): ZonedDateTime {
        val local = now.withZoneSameInstant(zone)
        var candidate = local.truncatedTo(ChronoUnit.HOURS).withMinute(MINUTE)
        if (!candidate.isAfter(local)) candidate = candidate.plusHours(1)
        return when {
            candidate.hour < FIRST_HOUR ->
                candidate.withHour(FIRST_HOUR).withMinute(MINUTE)
            candidate.hour > LAST_HOUR ->
                candidate.toLocalDate().plusDays(1)
                    .atTime(FIRST_HOUR, MINUTE).atZone(zone)
            else -> candidate
        }
    }

    /**
     * Records [complicationInstanceId] as in use and arms the alarm. Also called on
     * every data request, so instances added before this scheduler existed (which
     * never see an activation callback) are picked up too.
     */
    fun onActivated(context: Context, complicationInstanceId: Int) {
        val ids = activeIds(context) + complicationInstanceId.toString()
        prefs(context).edit().putStringSet(KEY_ACTIVE_IDS, ids).apply()
        schedule(context)
    }

    fun onDeactivated(context: Context, complicationInstanceId: Int) {
        val ids = activeIds(context) - complicationInstanceId.toString()
        prefs(context).edit().putStringSet(KEY_ACTIVE_IDS, ids).apply()
        if (ids.isEmpty()) cancel(context)
    }

    /** Arms (or re-arms) the single pending refresh alarm, if a complication is in use. */
    fun schedule(context: Context) {
        if (activeIds(context).isEmpty()) {
            cancel(context)
            return
        }
        val triggerAt = nextUpdateAfter(ZonedDateTime.now(zone)).toInstant().toEpochMilli()
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // Inexact (no SCHEDULE_EXACT_ALARM needed) but allowed to fire in Doze;
        // the system may shift it by a few minutes, which is fine for this use.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    fun requestUpdate(context: Context) {
        ComplicationDataSourceUpdateRequester.create(
            context = context,
            complicationDataSourceComponent = ComponentName(context, NextClassComplicationService::class.java),
        ).requestUpdateAll()
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ComplicationUpdateReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun activeIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_ACTIVE_IDS, emptySet()).orEmpty().toSet()
}

/** Fired by the :40 alarm: refresh the complication and arm the next alarm. */
class ComplicationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ComplicationUpdateScheduler.requestUpdate(context)
        ComplicationUpdateScheduler.schedule(context)
    }
}

/** Alarms are lost on reboot and computed in wall-clock time: re-arm on those events. */
class ComplicationRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> ComplicationUpdateScheduler.schedule(context)
        }
    }
}
