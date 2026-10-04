package app.orbz.android

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Not a test of anything: walks the signed-in app and saves one PNG per screen to the app's external files
 * directory, for the README and the posts. Run by hand with `am instrument` so the files survive.
 */
@RunWith(AndroidJUnit4::class)
class ShotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val pk = InstrumentationRegistry.getArguments().getString("pk") ?: ""

    private fun waitFor(timeoutMs: Long = 60_000, matcher: androidx.compose.ui.test.SemanticsMatcher) =
        rule.waitUntil(timeoutMs) { rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.activity.runOnUiThread {
            val imm = rule.activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(rule.activity.window.decorView.windowToken, 0)
        }
        Thread.sleep(900)
        val bmp = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun walk() {
        waitFor(matcher = hasTestTag("testKey"))
        shot("connect")
        rule.onNodeWithTag("testKey").performScrollTo().performTextInput(pk)
        rule.onNodeWithTag("testSignIn").performScrollTo().performClick()
        waitFor(matcher = hasTestTag("available"))
        waitFor(matcher = hasText("NEXT GRANT"))
        shot("home")

        rule.onNodeWithTag("tab-Chat").performClick()
        waitFor(matcher = hasTestTag("chatInput"))
        rule.onNodeWithTag("chatInput").performTextInput("In two sentences, what is a time-weighted average balance?")
        rule.onNodeWithTag("chatSend").performClick()
        waitFor(90_000, hasText("tokens · cost", substring = true))
        shot("chat")

        rule.onNodeWithTag("tab-Check").performClick()
        waitFor(matcher = hasTestTag("checkInput"))
        rule.onNodeWithTag("checkInput").performTextInput("0x6a043193D37872A958aAbbC0C7221541f5661BD0")
        rule.onNodeWithTag("checkGo").performClick()
        waitFor(matcher = hasText("LIQUIDITY"))
        waitFor(90_000, hasText("Not financial advice", substring = true))
        shot("check")

        rule.onNodeWithTag("tab-Receipts").performClick()
        waitFor(matcher = hasText("EPOCH ", substring = true))
        shot("receipts")

        rule.onNodeWithContentDescription("Settings").performClick()
        waitFor(matcher = hasTestTag("signOut"))
        shot("settings")
        rule.onNodeWithTag("signOut").performClick()
        waitFor(matcher = hasTestTag("testKey"))
    }
}
