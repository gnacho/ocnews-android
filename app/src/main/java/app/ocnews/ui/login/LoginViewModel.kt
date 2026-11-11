package app.ocnews.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.data.remote.NextcloudLoginFlow
import app.ocnews.data.remote.ServerChecker
import app.ocnews.data.remote.ServerDiagnostics
import app.ocnews.data.remote.ServerProbe
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.repo.NewsRepository.LoginResult
import app.ocnews.R
import app.ocnews.util.normalizeServerUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val token: String = "",
    val loading: Boolean = false,
    val errorRes: Int? = null,
    val errorArgs: List<Any> = emptyList(),
    val loggedIn: Boolean = false,
    // Set while the Nextcloud browser login is in progress.
    val browserLoginUrl: String? = null,
    val browserLoginWaiting: Boolean = false,
    // Technical detail of the last failure, shown under the message.
    val detail: String? = null,
    // Connection report produced by the diagnostics button.
    val diagnostic: List<String>? = null,
    val diagnosticRunning: Boolean = false,
)

class LoginViewModel(
    private val repository: NewsRepository,
    private val loginFlow: NextcloudLoginFlow = NextcloudLoginFlow(),
    private val serverProbe: ServerChecker = ServerProbe(),
    private val diagnostics: ServerDiagnostics = ServerDiagnostics(),
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onServerUrlChange(value: String) = _state.update { it.copy(serverUrl = value) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value) }
    fun onTokenChange(value: String) = _state.update { it.copy(token = value) }

    fun login() {
        val s = _state.value
        if (s.loading) return
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, errorRes = null, errorArgs = emptyList(), detail = null)
            }
            val server = normalizeServerUrl(s.serverUrl)
            when (val probe = serverProbe.check(server)) {
                is ServerProbe.Result.Failed -> {
                    _state.update {
                        it.copy(
                            loading = false,
                            errorRes = R.string.err_server_not_found,
                            detail = probe.detail,
                        )
                    }
                    return@launch
                }
                is ServerProbe.Result.Ok -> Unit
            }
            performLogin(server, s.username.trim(), s.token.trim())
        }
    }

    // Nextcloud login flow v2: authorise in the browser and come back logged in.
    fun startBrowserLogin() {
        val s = _state.value
        if (s.loading || s.browserLoginWaiting) return
        val server = normalizeServerUrl(s.serverUrl)
        if (server.isEmpty()) {
            _state.update { it.copy(errorRes = R.string.err_login_failed) }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    browserLoginWaiting = true,
                    errorRes = null,
                    errorArgs = emptyList(),
                    detail = null,
                )
            }
            when (val probe = serverProbe.check(server)) {
                is ServerProbe.Result.Failed -> {
                    _state.update {
                        it.copy(
                            browserLoginWaiting = false,
                            errorRes = R.string.err_server_not_found,
                            detail = probe.detail,
                        )
                    }
                    return@launch
                }
                is ServerProbe.Result.Ok -> Unit
            }
            val started = runCatching { loginFlow.start(server) }
            val start = started.getOrNull()
            if (start == null) {
                val reason = started.exceptionOrNull()?.message ?: "sin respuesta"
                println("OCNEWS login v2 start failed: $reason")
                _state.update {
                    it.copy(
                        browserLoginWaiting = false,
                        errorRes = R.string.err_login_failed,
                        detail = "login/v2: $reason",
                    )
                }
                return@launch
            }
            println("OCNEWS login v2 url: ${start.loginUrl}")
            _state.update { it.copy(browserLoginUrl = start.loginUrl, detail = null) }
            repeat(POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                val polled = runCatching { loginFlow.poll(start.pollEndpoint, start.pollToken) }
                val credentials = polled.getOrNull()
                polled.exceptionOrNull()?.let { println("OCNEWS login v2 poll failed: ${it.message}") }
                if (credentials != null && credentials.loginName.isNotBlank()) {
                    _state.update {
                        it.copy(browserLoginUrl = null, browserLoginWaiting = false, loading = true)
                    }
                    performLogin(server, credentials.loginName, credentials.appPassword)
                    return@launch
                }
            }
            _state.update {
                it.copy(
                    browserLoginUrl = null,
                    browserLoginWaiting = false,
                    errorRes = R.string.err_login_failed,
                )
            }
        }
    }

    // Account imported from the Nextcloud app through Single Sign-On: it gives
    // the server URL, the user id and an app password, so the normal sign in works.
    fun loginWithSso(url: String, userId: String, appPassword: String) {
        if (_state.value.loading) return
        println("OCNEWS sso account: url=$url user=$userId token=${appPassword.length} chars")
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    errorRes = null,
                    errorArgs = emptyList(),
                    detail = "sso: $url · $userId",
                    serverUrl = url,
                    username = userId,
                )
            }
            performLogin(normalizeServerUrl(url), userId, appPassword)
        }
    }

    // Runs the connection report from the device, for cases where the sign in
    // gives no clue.
    fun runDiagnostics() {
        val s = _state.value
        if (s.diagnosticRunning) return
        viewModelScope.launch {
            _state.update { it.copy(diagnosticRunning = true, diagnostic = null) }
            val report = runCatching {
                diagnostics.run(normalizeServerUrl(s.serverUrl), s.username.trim(), s.token.trim())
            }.getOrElse { listOf("diagnóstico falló: ${it.message}") }
            _state.update { it.copy(diagnosticRunning = false, diagnostic = report) }
        }
    }

    fun cancelBrowserLogin() {
        _state.update { it.copy(browserLoginUrl = null, browserLoginWaiting = false) }
    }

    private suspend fun performLogin(server: String, username: String, token: String) {
        // Never leave the screen stuck on "signing in": a slow or black-holed
        // request must end in a message.
        val result = withTimeoutOrNull(LOGIN_TIMEOUT_MS) {
            repository.login(server, username, token)
        }
        if (result == null) {
            // Distinguish "the server cannot be reached from here" (a network or
            // firewall block) from "the API answers but not this request".
            val probe = runCatching { serverProbe.check(server) }.getOrNull()
            val where = when (probe) {
                is ServerProbe.Result.Failed -> "status.php: ${probe.detail}"
                is ServerProbe.Result.Ok -> "status.php OK (${probe.version}), API sin respuesta"
                null -> "status.php: sin respuesta"
            }
            _state.update {
                it.copy(
                    loading = false,
                    errorRes = R.string.err_login_timeout,
                    errorArgs = emptyList(),
                    detail = "${LOGIN_TIMEOUT_MS / 1000}s · $where",
                )
            }
            return
        }
        when (result) {
            LoginResult.Success -> _state.update { it.copy(loading = false, loggedIn = true) }
            is LoginResult.Error -> {
                println("OCNEWS login failed: res=${result.messageRes} args=${result.args}")
                _state.update {
                    it.copy(
                        loading = false,
                        errorRes = result.messageRes,
                        errorArgs = result.args,
                        detail = buildString {
                            append("HTTP ")
                            append(result.args.firstOrNull() ?: "-")
                            append(" · ")
                            append(server)
                            append(" · ")
                            append(username)
                        },
                    )
                }
            }
        }
    }

    companion object {
        private const val LOGIN_TIMEOUT_MS = 20_000L
        private const val POLL_INTERVAL_MS = 2_000L
        private const val POLL_ATTEMPTS = 60

        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LoginViewModel(repository) as T
            }
    }
}
