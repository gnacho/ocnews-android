package app.ocnews.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Verifies that a real system tap outside the menu only closes it: it must not
// reach the content underneath. Compose test injection targets the activity
// window, so the outside tap goes through UiAutomator instead, which injects
// input at the system level and honours window stacking.
@RunWith(AndroidJUnit4::class)
class AppMenuTest {

    @get:Rule
    val rule = createComposeRule()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun showMenu(onOutsideTap: () -> Unit = {}, onItemTap: () -> Unit = {}) =
        mutableStateOf(true).also { expanded ->
            rule.setContent {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .align(Alignment.BottomCenter)
                                .testTag("underneath")
                                .clickable { onOutsideTap() },
                        )
                        AppMenu(
                            expanded = expanded.value,
                            onDismissRequest = { expanded.value = false },
                            topOffset = 0.dp,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Menu item") },
                                onClick = {
                                    expanded.value = false
                                    onItemTap()
                                },
                            )
                        }
                    }
                }
            }
        }

    @Test
    fun tappingOutsideClosesTheMenuWithoutActivatingTheContentBelow() {
        var outsideTaps = 0
        val expanded = showMenu(onOutsideTap = { outsideTaps++ })

        rule.onNodeWithText("Menu item").assertIsDisplayed()

        // Real tap in the middle of the content that sits under the menu.
        device.click(device.displayWidth / 2, (device.displayHeight * 0.9f).toInt())
        rule.waitForIdle()

        assertEquals("the tap outside must not reach the content", 0, outsideTaps)
        assertFalse("the menu must be collapsed", expanded.value)
        rule.onNodeWithText("Menu item").assertDoesNotExist()
    }

    @Test
    fun tappingAMenuItemRunsItAndClosesTheMenu() {
        var itemTaps = 0
        val expanded = showMenu(onItemTap = { itemTaps++ })

        rule.onNodeWithText("Menu item").performClick()
        rule.waitForIdle()

        assertEquals(1, itemTaps)
        assertFalse(expanded.value)
        rule.onNodeWithText("Menu item").assertDoesNotExist()
    }
}
