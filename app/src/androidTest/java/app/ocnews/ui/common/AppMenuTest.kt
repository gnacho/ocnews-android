package app.ocnews.ui.common

import androidx.activity.compose.BackHandler
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

// A menu must collapse with its animation on every dismissal path (tap outside,
// system back, tap on the card, tap on an item) and the screen behind must keep
// its state. Compose test injection targets the activity window, so the outside
// tap and the back press go through UiAutomator, which injects real system input.
@RunWith(AndroidJUnit4::class)
class AppMenuTest {

    @get:Rule
    val rule = createComposeRule()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun showMenu(
        onOutsideTap: () -> Unit = {},
        onItemTap: () -> Unit = {},
        onBack: () -> Unit = {},
    ) =
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
                        // Stands for the screen behind: back must not reach it.
                        BackHandler { onBack() }
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

    // The exit animation keeps the card on screen for a moment after dismissal.
    private fun assertCollapsesWithAnimation(expanded: androidx.compose.runtime.MutableState<Boolean>) {
        assertFalse("the menu state must be collapsed", expanded.value)
        rule.onNodeWithText("Menu item").assertIsDisplayed()
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        rule.onNodeWithText("Menu item").assertDoesNotExist()
    }

    @Test
    fun tappingOutsideCollapsesTheMenuWithoutActivatingTheContentBelow() {
        var outsideTaps = 0
        rule.mainClock.autoAdvance = false
        val expanded = showMenu(onOutsideTap = { outsideTaps++ })
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Menu item").assertIsDisplayed()

        device.click(device.displayWidth / 2, (device.displayHeight * 0.9f).toInt())
        rule.waitForIdle()

        assertEquals("the tap outside must not reach the content", 0, outsideTaps)
        assertCollapsesWithAnimation(expanded)
    }

    @Test
    fun pressingBackCollapsesTheMenuAndDoesNotReachTheContentBehind() {
        var outsideTaps = 0
        var backPresses = 0
        rule.mainClock.autoAdvance = false
        val expanded = showMenu(
            onOutsideTap = { outsideTaps++ },
            onBack = { backPresses++ },
        )
        rule.mainClock.advanceTimeByFrame()

        device.pressBack()
        rule.waitForIdle()

        assertEquals("back must not reach the screen behind", 0, backPresses)
        assertEquals(0, outsideTaps)
        assertCollapsesWithAnimation(expanded)
    }

    @Test
    fun tappingTheMenuCardCollapsesIt() {
        rule.mainClock.autoAdvance = false
        val expanded = showMenu()
        rule.mainClock.advanceTimeByFrame()

        // Tap the padding above the item, inside the card but not on a menu entry.
        val bounds = rule.onNodeWithText("Menu item").fetchSemanticsNode().boundsInRoot
        device.click((bounds.center.x).toInt(), (bounds.top - 12f).toInt())
        rule.waitForIdle()

        assertCollapsesWithAnimation(expanded)
    }

    @Test
    fun tappingAMenuItemRunsItAndCollapsesTheMenu() {
        var itemTaps = 0
        rule.mainClock.autoAdvance = false
        val expanded = showMenu(onItemTap = { itemTaps++ })
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Menu item").performClick()
        rule.waitForIdle()

        assertEquals(1, itemTaps)
        assertCollapsesWithAnimation(expanded)
    }
}
