package app.ocnews.ui.login

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Nextcloud authorisation inside the app. Some phones block launching another
// app (MIUI and friends), so this path does not depend on a browser; the top bar
// still offers opening the same URL in the system browser.
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextcloudLoginSheet(
    url: String,
    onClose: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val holder = remember { arrayOfNulls<WebView>(1) }
        DisposableEffect(url) {
            onDispose {
                holder[0]?.stopLoading()
                holder[0]?.destroy()
                holder[0] = null
            }
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Nextcloud") },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { onOpenBrowser(url) }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            holder[0] = this
                            loadUrl(url)
                        }
                    },
                )
            }
        }
    }
}
