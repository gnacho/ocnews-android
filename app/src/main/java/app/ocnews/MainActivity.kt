package app.ocnews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ocnews.ui.login.LoginScreen
import app.ocnews.ui.login.LoginViewModel

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.factory((application as OcnewsApplication).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by loginViewModel.state.collectAsStateWithLifecycle()
            LoginScreen(
                state = state,
                onServerUrlChange = loginViewModel::onServerUrlChange,
                onUsernameChange = loginViewModel::onUsernameChange,
                onTokenChange = loginViewModel::onTokenChange,
                onLogin = loginViewModel::login,
            )
        }
    }
}
