package ch.ncavallini.polywear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking

/**
 * Handles the watch's on-demand request for a fresh credential: re-mints the
 * eduapp cookie on the phone and pushes it back to the watch.
 *
 * Registered in the manifest for [WearContract.CREDENTIAL_REQUEST_PATH].
 */
class CredentialRequestListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearContract.CREDENTIAL_REQUEST_PATH) return

        // onMessageReceived runs on a background thread; block it while we refresh
        // so the service stays alive for the (async) WebView reload. The refresher
        // is internally bounded by a timeout.
        runBlocking {
            val captured = CredentialRefresher(applicationContext).refresh() ?: return@runBlocking
            CredentialSender(applicationContext).send(captured)
        }
    }
}
