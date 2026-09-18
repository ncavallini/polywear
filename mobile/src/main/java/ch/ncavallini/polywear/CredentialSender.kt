package ch.ncavallini.polywear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Sends a captured credential to every connected watch node over the Data Layer. */
class CredentialSender(context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    /** Returns the number of watch nodes the credential was delivered to. */
    suspend fun send(captured: CredentialCapturer.Captured): Int {
        val payload = buildJsonObject {
            put(WearContract.KEY_TYPE, captured.type)
            put(WearContract.KEY_VALUE, captured.value)
            captured.expiresAtEpochSeconds?.let { put(WearContract.KEY_EXPIRES_AT, it) }
        }.let { Json.encodeToString(it) }.toByteArray(Charsets.UTF_8)

        val nodes = nodeClient.connectedNodes.await()
        var delivered = 0
        for (node in nodes) {
            runCatching {
                messageClient.sendMessage(node.id, WearContract.CREDENTIAL_PATH, payload).await()
            }.onSuccess { delivered++ }
        }
        return delivered
    }
}
