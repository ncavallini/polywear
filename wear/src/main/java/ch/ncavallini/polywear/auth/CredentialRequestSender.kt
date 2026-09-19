package ch.ncavallini.polywear.auth

import android.content.Context
import ch.ncavallini.polywear.WearContract
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Asks the phone companion to re-mint and re-send a fresh eduapp credential.
 * Sent when the watch's stored credential is rejected/expired so the phone (which
 * still holds the long-lived SSO session) can top the watch back up without a
 * manual re-login. The phone answers on [WearContract.CREDENTIAL_PATH].
 */
class CredentialRequestSender(context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    /** Returns the number of phone nodes the request reached. */
    suspend fun request(): Int {
        val nodes = runCatching { nodeClient.connectedNodes.await() }.getOrNull() ?: return 0
        var sent = 0
        for (node in nodes) {
            runCatching {
                messageClient
                    .sendMessage(node.id, WearContract.CREDENTIAL_REQUEST_PATH, ByteArray(0))
                    .await()
            }.onSuccess { sent++ }
        }
        return sent
    }
}
