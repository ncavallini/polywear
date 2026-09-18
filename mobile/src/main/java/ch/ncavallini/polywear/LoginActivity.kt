package ch.ncavallini.polywear

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import ch.ncavallini.polywear.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

/**
 * Phone-side login: runs the real eduapp web login in a WebView, then captures
 * the resulting credential and pushes it to the watch over the Data Layer.
 *
 * "Send to watch" is manual so you can trigger capture once you are fully logged
 * in and looking at the schedule. Phase 0 can later automate detection of the
 * post-login state and, if needed, restrict capture to the confirmed scheme.
 */
class LoginActivity : ComponentActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val capturer = CredentialCapturer()
    private val sender by lazy { CredentialSender(this) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                binding.status.text = getString(R.string.status_page, url ?: "")
            }
        }

        binding.sendButton.setOnClickListener { captureAndSend() }
        binding.webView.loadUrl(CredentialCapturer.LOGIN_URL)
    }

    private fun captureAndSend() {
        binding.status.text = getString(R.string.status_capturing)
        // eduapp auth is a session cookie (confirmed in Phase 0). Prefer the
        // cookie; fall back to a bearer token only if no cookie is present.
        val cookie = capturer.captureCookie()
        if (cookie != null) {
            deliver(cookie)
        } else {
            capturer.captureBearer(binding.webView) { bearer ->
                if (bearer == null) {
                    binding.status.text = getString(R.string.status_no_credential)
                } else {
                    deliver(bearer)
                }
            }
        }
    }

    private fun deliver(captured: CredentialCapturer.Captured) {
        lifecycleScope.launch {
            val delivered = runCatching { sender.send(captured) }.getOrDefault(0)
            binding.status.text = getString(R.string.status_sent, captured.type, delivered)
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
