package ch.ncavallini.polywear.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encrypted, at-rest storage for the credential received from the phone.
 * Exposes the current value as a [StateFlow] so the UI reacts when the phone
 * delivers a credential while the watch screen is open.
 */
class TokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _credential = MutableStateFlow(read())
    val credential: StateFlow<Credential?> = _credential.asStateFlow()

    fun current(): Credential? = _credential.value

    fun save(credential: Credential) {
        prefs.edit()
            .putString(KEY_TYPE, credential.type.name)
            .putString(KEY_VALUE, credential.value)
            .apply {
                if (credential.expiresAtEpochSeconds != null) {
                    putLong(KEY_EXPIRES, credential.expiresAtEpochSeconds)
                } else {
                    remove(KEY_EXPIRES)
                }
            }
            .apply()
        _credential.value = credential
    }

    fun clear() {
        prefs.edit().clear().apply()
        _credential.value = null
    }

    private fun read(): Credential? {
        val typeName = prefs.getString(KEY_TYPE, null) ?: return null
        val value = prefs.getString(KEY_VALUE, null) ?: return null
        val type = runCatching { Credential.Type.valueOf(typeName) }.getOrNull() ?: return null
        val expires = if (prefs.contains(KEY_EXPIRES)) prefs.getLong(KEY_EXPIRES, 0L) else null
        return Credential(type, value, expires)
    }

    private companion object {
        const val PREFS_NAME = "eduapp_secure_prefs"
        const val KEY_TYPE = "cred_type"
        const val KEY_VALUE = "cred_value"
        const val KEY_EXPIRES = "cred_expires"
    }
}
