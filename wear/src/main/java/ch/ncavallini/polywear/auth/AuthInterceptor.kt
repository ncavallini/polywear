package ch.ncavallini.polywear.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the stored credential to every outgoing eduapp request. A 401/403 is
 * left to bubble up (Retrofit -> HttpException) so the repository can map it to
 * a "re-authenticate on phone" state.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val credential = tokenStore.current()?.takeUnless { it.isExpired }
        val request = if (credential == null) {
            chain.request()
        } else {
            val builder = chain.request().newBuilder()
            when (credential.type) {
                Credential.Type.BEARER -> builder.header("Authorization", "Bearer ${credential.value}")
                Credential.Type.COOKIE -> builder.header("Cookie", credential.value)
            }
            builder.build()
        }
        return chain.proceed(request)
    }
}
