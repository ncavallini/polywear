package ch.ncavallini.polywear

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Headlessly reloads eduapp in a WebView so the phone's still-valid ETH SSO
 * session silently re-mints a fresh eduapp session cookie, then captures it.
 *
 * This is what lets the watch recover from its ~24h cookie expiry without you
 * logging in again: the phone keeps the long-lived SSO session, while the watch
 * only ever held the short-lived eduapp cookie. Reading [CredentialCapturer]
 * alone would just return the same stale cookie — we must reload the page so the
 * SSO redirect runs and a new cookie is issued.
 */
class CredentialRefresher(context: Context) {

    private val appContext = context.applicationContext
    private val capturer = CredentialCapturer()

    /**
     * Returns a freshly re-minted credential, or null if the SSO session is also
     * gone (a real re-login on the phone is needed) or the reload timed out.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun refresh(): CredentialCapturer.Captured? = withTimeoutOrNull(TIMEOUT_MS) {
        val result = CompletableDeferred<CredentialCapturer.Captured?>()
        val webView = withContext(Dispatchers.Main) {
            CookieManager.getInstance().setAcceptCookie(true)
            WebView(appContext).apply {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Only trust the cookie once we're back on the eduapp origin,
                        // i.e. any SSO redirect has completed and the new session
                        // cookie has been issued.
                        if (url != null && url.startsWith(CredentialCapturer.EDUAPP_ORIGIN)) {
                            val captured = capturer.captureCookie()
                            if (captured != null && !result.isCompleted) {
                                result.complete(captured)
                            }
                        }
                    }
                }
                loadUrl(CredentialCapturer.LOGIN_URL)
            }
        }
        try {
            result.await()
        } finally {
            // Destroy on the main thread even while the enclosing timeout is cancelling.
            withContext(NonCancellable + Dispatchers.Main) { webView.destroy() }
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
