package app.orbz.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * The whole app against a console on the developer's machine: sign in with a test key, read the balance, chat,
 * check the token, read receipts, sign out. Needs `-Porbz.testPk=0x…` for a wallet that has credit there.
 */
@RunWith(AndroidJUnit4::class)
class FlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val pk = InstrumentationRegistry.getArguments().getString("pk") ?: ""
    private val key = InstrumentationRegistry.getArguments().getString("key") ?: ""

    /** On a timeout, the whole screen goes to logcat (tag ORBZ-TEST) so the failure explains itself. */
    private fun waitFor(timeoutMs: Long = 60_000, matcher: androidx.compose.ui.test.SemanticsMatcher) {
        try {
            rule.waitUntil(timeoutMs) { rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            rule.onRoot(useUnmergedTree = true).printToLog("ORBZ-TEST")
            throw e
        }
    }

    @Test
    fun signInChatCheckReceiptsSignOut() {
        check(pk.startsWith("0x")) { "pass -Porbz.testPk" }
        waitFor(matcher = hasTestTag("testKey"))
        rule.onNodeWithTag("testKey").performScrollTo().performTextInput(pk)
        rule.onNodeWithTag("testSignIn").performScrollTo().performClick()
        walk()
    }

    /** The same walk, in through an API key: the way 0.1.0 ships. */
    @Test
    fun apiKeyChatCheckReceiptsSignOut() {
        check(key.startsWith("sk-orbz-")) { "pass -Porbz.testKey" }
        waitFor(matcher = hasTestTag("apiKey"))
        rule.onNodeWithTag("apiKey").performScrollTo().performTextInput(key)
        rule.onNodeWithTag("keySignIn").performScrollTo().performClick()
        walk()
    }

    private fun walk() {

        waitFor(matcher = hasTestTag("available"))
        rule.onNodeWithTag("address").assertIsDisplayed()
        waitFor(matcher = hasText("NEXT GRANT"))

        rule.onNodeWithTag("tab-Chat").performClick()
        waitFor(matcher = hasTestTag("chatInput"))
        rule.onNodeWithTag("chatInput").performTextInput("Reply with the single word: pong")
        rule.onNodeWithTag("chatSend").performClick()
        waitFor(90_000, hasText("tokens · cost", substring = true))

        rule.onNodeWithTag("tab-Check").performClick()
        waitFor(matcher = hasTestTag("checkInput"))
        rule.onNodeWithTag("checkInput").performTextInput("0x6a043193D37872A958aAbbC0C7221541f5661BD0")
        rule.onNodeWithTag("checkGo").performClick()
        waitFor(matcher = hasText("LIQUIDITY"))
        waitFor(90_000, hasText("Not financial advice", substring = true))

        rule.onNodeWithTag("tab-Receipts").performClick()
        waitFor(matcher = hasText("EPOCH ", substring = true))

        rule.onNodeWithContentDescription("Settings").performClick()
        waitFor(matcher = hasTestTag("signOut"))
        rule.onNodeWithTag("signOut").performClick()
        waitFor(matcher = hasTestTag("testKey"))
    }
}
