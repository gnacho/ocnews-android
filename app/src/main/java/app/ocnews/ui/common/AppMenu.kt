package app.ocnews.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.delay

private const val MENU_ANIM_MS = 120

// Dropdown menu in a full-screen modal window: a tap outside the card only closes
// the menu and can never reach the content underneath, and the card collapses with
// a fade and scale animation.
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    topOffset: Dp = 0.dp,
    endPadding: Dp = 8.dp,
    maxWidth: Dp = 320.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Keep the window mounted until the exit animation finishes.
    var mounted by remember { mutableStateOf(expanded) }
    LaunchedEffect(expanded) {
        if (expanded) {
            mounted = true
        } else {
            delay(MENU_ANIM_MS.toLong())
            mounted = false
        }
    }
    if (!mounted) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            // No dim behind the menu: it is a dropdown, not a dialog.
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        }
        Box(Modifier.fillMaxSize()) {
            // Scrim: consumes every tap that is not on the menu card.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures { onDismissRequest() }
                    },
            )
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(MENU_ANIM_MS)) +
                    scaleIn(tween(MENU_ANIM_MS), initialScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f)),
                exit = fadeOut(tween(MENU_ANIM_MS)) +
                    scaleOut(tween(MENU_ANIM_MS), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = topOffset, end = endPadding),
            ) {
                Surface(
                    modifier = modifier.widthIn(max = maxWidth),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                ) {
                    Column(Modifier.padding(vertical = 8.dp), content = content)
                }
            }
        }
    }
}
