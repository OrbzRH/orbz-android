package app.orbz.android

import app.orbz.android.auth.Siwe
import app.orbz.android.net.Challenge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SiweTest {
    private val c = Challenge("0123456789abcdef0123456789abcdef", "use.orbz.app", "https://use.orbz.app", 4663, "2026-10-04T10:00:00.000Z", "2026-10-04T10:10:00.000Z")

    /** The server's parser (lib/siwe.ts) demands exactly eleven lines in this order. */
    @Test
    fun matchesTheServersLayout() {
        val m = Siwe.build(c, "0x6FC81248b221FCaCf193eA76b70FeC682e987719")
        val l = m.split("\n")
        assertEquals(11, l.size)
        assertEquals("use.orbz.app wants you to sign in with your Ethereum account:", l[0])
        assertEquals("0x6FC81248b221FCaCf193eA76b70FeC682e987719", l[1])
        assertEquals("", l[2]); assertEquals(Siwe.LOGIN_STATEMENT, l[3]); assertEquals("", l[4])
        assertEquals("URI: https://use.orbz.app", l[5]); assertEquals("Version: 1", l[6]); assertEquals("Chain ID: 4663", l[7])
        assertEquals("Nonce: 0123456789abcdef0123456789abcdef", l[8])
        assertEquals("Issued At: 2026-10-04T10:00:00.000Z", l[9]); assertEquals("Expiration Time: 2026-10-04T10:10:00.000Z", l[10])
        assertTrue(!m.contains("\r"))
    }

    @Test
    fun hexIsUtf8Bytes() {
        assertEquals("0x4f72627a", Siwe.toHex("Orbz"))
    }
}
