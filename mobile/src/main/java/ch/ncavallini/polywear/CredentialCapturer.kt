package ch.ncavallini.polywear

import android.webkit.CookieManager
import android.webkit.WebView

/**
 * Extracts a usable credential from the logged-in eduapp WebView.
 *
 * Because the exact auth scheme is not confirmed yet (Phase 0), this tries both
 * strategies and the caller prefers a Bearer token if present, otherwise the
 * session cookie. Once Phase 0 pins down the real mechanism, keep only the
 * relevant path and fix the token key / cookie name.
 */
class CredentialCapturer {

    data class Captured(
        val type: String,
        val value: String,
        val expiresAtEpochSeconds: Long?,
    )

    /** Reads the session cookie(s) for the eduapp origin, if any. */
    fun captureCookie(): Captured? {
        val cookie = CookieManager.getInstance().getCookie(EDUAPP_ORIGIN)
        return cookie?.takeIf { it.isNotBlank() }
            ?.let { Captured(WearContract.TYPE_COOKIE, it, expiresAtEpochSeconds = null) }
    }

    /**
     * Attempts to read a bearer token from local/session storage. The candidate
     * key list is a guess — adjust after inspecting eduapp in Phase 0.
     */
    fun captureBearer(webView: WebView, onResult: (Captured?) -> Unit) {
        val js = """
            (function() {
              var keys = ['access_token','token','id_token','oauth_token','authToken','jwt'];
              for (var i = 0; i < keys.length; i++) {
                var v = window.localStorage.getItem(keys[i]) || window.sessionStorage.getItem(keys[i]);
                if (v) { return v; }
              }
              return '';
            })();
        """.trimIndent()

        webView.evaluateJavascript(js) { raw ->
            val token = raw
                ?.removeSurrounding("\"")
                ?.takeIf { it.isNotBlank() && it != "null" }
            onResult(token?.let { Captured(WearContract.TYPE_BEARER, it, expiresAtEpochSeconds = null) })
        }
    }

    companion object {
        const val EDUAPP_ORIGIN = "https://eduapp.ethz.ch"
        const val LOGIN_URL = "https://eduapp.ethz.ch/"
    }
}
