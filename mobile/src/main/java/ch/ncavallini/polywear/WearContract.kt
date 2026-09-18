package ch.ncavallini.polywear

/**
 * Constants shared with the :wear module for the Data Layer credential hand-off.
 * Duplicated verbatim in :wear (no shared module) — keep in sync.
 */
object WearContract {
    const val CREDENTIAL_PATH = "/polywear/credential"

    const val KEY_TYPE = "type"
    const val KEY_VALUE = "value"
    const val KEY_EXPIRES_AT = "expiresAtEpochSeconds"

    const val TYPE_BEARER = "BEARER"
    const val TYPE_COOKIE = "COOKIE"
}
