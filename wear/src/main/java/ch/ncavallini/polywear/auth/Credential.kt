package ch.ncavallini.polywear.auth

/** A credential captured on the phone and used by the watch for API calls. */
data class Credential(
    val type: Type,
    val value: String,
    val expiresAtEpochSeconds: Long?,
) {
    enum class Type { BEARER, COOKIE }

    val isExpired: Boolean
        get() = expiresAtEpochSeconds?.let { it * 1000L <= System.currentTimeMillis() } ?: false
}
