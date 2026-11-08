package app.ocnews.data.remote

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.Response

// Adds Basic auth (user:app-token) to requests against the configured host.
class AuthInterceptor(
    private val credentialsProvider: () -> Credentials?,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val creds = credentialsProvider() ?: return chain.proceed(chain.request())
        val token = "${creds.username}:${creds.token}"
            .toByteArray(Charsets.UTF_8)
            .let { Base64.encodeToString(it, Base64.NO_WRAP) }
        val request = chain.request().newBuilder()
            .header("Authorization", "Basic $token")
            .build()
        return chain.proceed(request)
    }
}

data class Credentials(
    val username: String,
    val token: String,
)
