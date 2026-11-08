package app.ocnews.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.ocnews.data.repo.NewsRepository
import app.ocnews.data.repo.NewsRepository.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val token: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
)

class LoginViewModel(private val repository: NewsRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onServerUrlChange(value: String) = _state.update { it.copy(serverUrl = value) }
    fun onUsernameChange(value: String) = _state.update { it.copy(username = value) }
    fun onTokenChange(value: String) = _state.update { it.copy(token = value) }

    fun login() {
        val s = _state.value
        if (s.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repository.login(s.serverUrl.trim(), s.username.trim(), s.token.trim())) {
                LoginResult.Success -> _state.update { it.copy(loading = false, loggedIn = true) }
                is LoginResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    companion object {
        fun factory(repository: NewsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LoginViewModel(repository) as T
            }
    }
}
