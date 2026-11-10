package app.ocnews.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.data.remote.NextcloudLoginFlow
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.repo.NewsRepository.LoginResult
import app.ocnews.R
import app.ocnews.util.normalizeServerUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
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
)

class LoginViewModel(
    private val repository: NewsRepository,
    private val loginFlow: NextcloudLoginFlow = NextcloudLoginFlow(),
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
            _state.update { it.copy(loading = true, errorRes = null, errorArgs = emptyList()) }
            performLogin(normalizeServerUrl(s.serverUrl), s.username.trim(), s.token.trim())
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
                it.copy(browserLoginWaiting = true, errorRes = null, errorArgs = emptyList())
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

    fun cancelBrowserLogin() {
        _state.update { it.copy(browserLoginUrl = null, browserLoginWaiting = false) }
    }

    private suspend fun performLogin(server: String, username: String, token: String) {
        when (val result = repository.login(server, username, token)) {
            LoginResult.Success -> _state.update { it.copy(loading = false, loggedIn = true) }
            is LoginResult.Error -> _state.update {
                it.copy(loading = false, errorRes = result.messageRes, errorArgs = result.args)
            }
        }
    }

    companion object {
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
