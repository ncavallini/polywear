package ch.ncavallini.polywear.auth

import ch.ncavallini.polywear.PolyApp
import ch.ncavallini.polywear.WearContract
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Receives the credential pushed by the phone companion over the Data Layer and
 * stores it. Registered in the manifest for [WearContract.CREDENTIAL_PATH].
 */
class CredentialListenerService : WearableListenerService() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearContract.CREDENTIAL_PATH) return

        val payload = event.data.toString(Charsets.UTF_8)
        val credential = runCatching { parse(payload) }.getOrNull() ?: return

        val tokenStore = (applicationContext as PolyApp).container.tokenStore
        tokenStore.save(credential)
    }

    private fun parse(payload: String): Credential? {
        val obj = json.parseToJsonElement(payload).jsonObject
        val typeName = obj[WearContract.KEY_TYPE]?.jsonPrimitive?.content ?: return null
        val value = obj[WearContract.KEY_VALUE]?.jsonPrimitive?.content ?: return null
        val expires = obj[WearContract.KEY_EXPIRES_AT]?.jsonPrimitive?.longOrNull
        val type = when (typeName) {
            WearContract.TYPE_BEARER -> Credential.Type.BEARER
            WearContract.TYPE_COOKIE -> Credential.Type.COOKIE
            else -> return null
        }
        return Credential(type, value, expires)
    }
}
