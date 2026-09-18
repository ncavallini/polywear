package ch.ncavallini.polywear

/**
 * Constants shared between the watch app and the phone companion for the
 * Wearable Data Layer credential hand-off.
 *
 * NOTE: this is duplicated verbatim in the :mobile module (there is no shared
 * module). Keep the two copies in sync.
 */
object WearContract {
    /** MessageClient path the phone uses to push the captured credential. */
    const val CREDENTIAL_PATH = "/polywear/credential"

    /**
     * Payload is UTF-8 JSON: {"type":"BEARER"|"COOKIE","value":"...","expiresAtEpochSeconds":<long?>}
     */
    const val KEY_TYPE = "type"
    const val KEY_VALUE = "value"
    const val KEY_EXPIRES_AT = "expiresAtEpochSeconds"

    const val TYPE_BEARER = "BEARER"
    const val TYPE_COOKIE = "COOKIE"
}
