package app.ocnews.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// Checks that the URL really hosts a Nextcloud/OpenCloud instance before trying
// to sign in, so a wrong host gives a clear message instead of a 404 later.
fun interface ServerChecker {
    suspend fun check(serverUrl: String): ServerProbe.Result
}

class ServerProbe(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) : ServerChecker {
    sealed interface Result {
        data class Ok(val version: String) : Result
        data class Failed(val detail: String) : Result
    }

    override suspend fun check(serverUrl: String): Result = withContext(Dispatchers.IO) {
        val base = serverUrl.trimEnd('/')
        val request = Request.Builder().url("$base/status.php").get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.Failed("status.php: HTTP ${response.code}")
                }
                val body = Json { ignoreUnknownKeys = true }
                    .parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                val installed = body["installed"]?.jsonPrimitive?.booleanOrNull ?: false
                if (!installed) {
                    return@withContext Result.Failed("status.php: not installed")
                }
                val version = body["versionstring"]?.jsonPrimitive?.content
                    ?: body["version"]?.jsonPrimitive?.content.orEmpty()
                Result.Ok(version)
            }
        } catch (e: Exception) {
            Result.Failed("status.php: ${e.message ?: e::class.java.simpleName}")
        }
    }
}
