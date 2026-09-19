package ch.ncavallini.polywear

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodically re-mints the eduapp cookie on the phone and pushes it to the
 * watch, so the watch's login is topped up before its ~24h expiry and the
 * "sign in on your phone" screen rarely (ideally never) shows.
 */
class CredentialRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val captured = CredentialRefresher(applicationContext).refresh()
            ?: return Result.success() // SSO session gone or reload timed out; retry next period.
        CredentialSender(applicationContext).send(captured)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "credential-refresh"

        /**
         * Enqueues the periodic refresh once (idempotent). Call after a successful
         * manual login/send, so we only start refreshing for a phone that is
         * actually signed in.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CredentialRefreshWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
