package app.ocnews.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import app.ocnews.util.isPrivateAddress
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

// Step by step connection report, run from the device, so the failure can be
// seen without a log viewer.
class ServerDiagnostics {
    suspend fun run(serverUrl: String, username: String, token: String): List<String> =
        withContext(Dispatchers.IO) {
            val lines = mutableListOf<String>()
            val url = try {
                URL(serverUrl.trimEnd('/'))
            } catch (e: Exception) {
                return@withContext listOf("URL inválida: ${e.message}")
            }
            val host = url.host
            val port = if (url.port > 0) url.port else 443
            lines += "servidor: $host:$port"

            // 1) DNS
            val started = System.currentTimeMillis()
            val addresses = runCatching { InetAddress.getAllByName(host) }
            val dnsMs = System.currentTimeMillis() - started
            lines += when {
                addresses.isSuccess -> {
                    val list = addresses.getOrThrow()
                    val detail = "1) DNS OK (${dnsMs}ms): " +
                        list.joinToString(", ") { it.hostAddress ?: "?" }
                    val privateOnly = list.all { isPrivateAddress(it) }
                    if (privateOnly) {
                        detail + "\n   AVISO: solo direcciones privadas. En tu red local el nombre apunta " +
                            "a un servidor interno; fuera de ella no funcionará."
                    } else {
                        detail
                    }
                }
                else -> "1) DNS FALLA (${dnsMs}ms): ${addresses.exceptionOrNull()?.javaClass?.simpleName} " +
                    "${addresses.exceptionOrNull()?.message}"
            }

            // 2) TCP
            val tcpStarted = System.currentTimeMillis()
            val tcp = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 8000)
                    socket.inetAddress.hostAddress ?: "?"
                }
            }
            val tcpMs = System.currentTimeMillis() - tcpStarted
            lines += if (tcp.isSuccess) {
                "2) TCP OK (${tcpMs}ms): ${tcp.getOrThrow()}"
            } else {
                "2) TCP FALLA (${tcpMs}ms): ${tcp.exceptionOrNull()?.javaClass?.simpleName} " +
                    "${tcp.exceptionOrNull()?.message}"
            }

            // 3) TLS
            val tlsStarted = System.currentTimeMillis()
            val tls = runCatching {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                (factory.createSocket() as SSLSocket).use { socket ->
                    socket.connect(InetSocketAddress(host, port), 8000)
                    socket.sslParameters = socket.sslParameters.apply { serverNames = listOf(javax.net.ssl.SNIHostName(host)) }
                    socket.startHandshake()
                    socket.session.protocol
                }
            }
            val tlsMs = System.currentTimeMillis() - tlsStarted
            lines += if (tls.isSuccess) {
                "3) TLS OK (${tlsMs}ms): ${tls.getOrThrow()}"
            } else {
                "3) TLS FALLA (${tlsMs}ms): ${tls.exceptionOrNull()?.javaClass?.simpleName} " +
                    "${tls.exceptionOrNull()?.message}"
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            // 4) status.php
            lines += httpStep(client, "4) status.php", "${serverUrl.trimEnd('/')}/status.php", null)

            // 5) version endpoint con las credenciales
            val basic = okhttp3.Credentials.basic(username, token)
            lines += httpStep(
                client,
                "5) api/version",
                "${serverUrl.trimEnd('/')}/index.php/apps/news/api/v1-3/version",
                basic,
            )

            lines
        }

    private fun httpStep(client: OkHttpClient, label: String, url: String, authorization: String?): String {
        val started = System.currentTimeMillis()
        return try {
            val builder = Request.Builder().url(url).get()
            if (authorization != null) builder.header("Authorization", authorization)
            client.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty().take(120).replace("\n", " ")
                "$label: HTTP ${response.code} (${System.currentTimeMillis() - started}ms) $body"
            }
        } catch (e: Exception) {
            "$label: FALLA (${System.currentTimeMillis() - started}ms) ${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
