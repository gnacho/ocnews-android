package app.ocnews.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// Nextcloud login flow v2: the app asks the server for a login URL, the user
// authorises it in the browser and the app polls until it receives an app
// password. This is how the official Nextcloud clients sign in, so the user
// never has to create and paste an app password by hand.
class NextcloudLoginFlow(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    data class Start(val pollToken: String, val pollEndpoint: String, val loginUrl: String)

    data class Credentials(val server: String, val loginName: String, val appPassword: String)

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun start(serverUrl: String): Start = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val base = serverUrl.trimEnd('/')
        val request = Request.Builder()
            .url("$base/index.php/login/v2")
            .post(FormBody.Builder().build())
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            val poll = body["poll"]?.jsonObject ?: error("no poll")
            Start(
                pollToken = poll["token"]?.jsonPrimitive?.content ?: error("no token"),
                pollEndpoint = poll["endpoint"]?.jsonPrimitive?.content ?: error("no endpoint"),
                loginUrl = body["login"]?.jsonPrimitive?.content ?: error("no login"),
            )
        }
    }

    // Null while the user has not finished in the browser yet.
    suspend fun poll(pollEndpoint: String, token: String): Credentials? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = Request.Builder()
                .url(pollEndpoint)
                .post(FormBody.Builder().add("token", token).build())
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) return@withContext null
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val body = json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                Credentials(
                    server = body["server"]?.jsonPrimitive?.content ?: "",
                    loginName = body["loginName"]?.jsonPrimitive?.content ?: "",
                    appPassword = body["appPassword"]?.jsonPrimitive?.content ?: "",
                )
            }
        }
}
