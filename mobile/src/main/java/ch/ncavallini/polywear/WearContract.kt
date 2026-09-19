package ch.ncavallini.polywear

/**
 * Constants shared with the :wear module for the Data Layer credential hand-off.
 * Duplicated verbatim in :wear (no shared module) — keep in sync.
 */
object WearContract {
    const val CREDENTIAL_PATH = "/polywear/credential"

    /** Watch asks the phone for a fresh credential (empty payload); phone replies on CREDENTIAL_PATH. */
    const val CREDENTIAL_REQUEST_PATH = "/polywear/credential-request"

    const val KEY_TYPE = "type"
    const val KEY_VALUE = "value"
    const val KEY_EXPIRES_AT = "expiresAtEpochSeconds"

    const val TYPE_BEARER = "BEARER"
    const val TYPE_COOKIE = "COOKIE"
}
