package app.orbz.android

import app.orbz.android.ui.burnsIn
import app.orbz.android.ui.compact
import app.orbz.android.ui.countdown
import app.orbz.android.ui.money
import app.orbz.android.ui.usd
import app.orbz.android.ui.usdMicro
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun micro() { assertEquals("$1.99", usdMicro("1990000")); assertEquals("$0.0015", usdMicro("1500")); assertEquals("$0.00", usdMicro("0")); assertEquals("<$0.0001", usdMicro("3")); assertEquals("$1,234.57", usdMicro("1234567890")) }
    @Test fun decimal() { assertEquals("$0.3890", usd("0.388984")); assertEquals("$12.50", usd("12.5")) }
    @Test fun moneyAndCounts() { assertEquals("$15.0K", money(15013.23)); assertEquals("<$0.01", money(0.0000062)); assertEquals("130.5K", compact(130505.5)); assertEquals("100K", compact(100000.0)); assertEquals("1.00B", compact(1e9)); assertEquals("0.00392", compact(0.00391902)) }
    @Test fun time() { assertEquals("2d 4h", burnsIn(52 * 3600L)); assertEquals("41m", burnsIn(41 * 60L)); assertEquals("<1m", burnsIn(30)); assertEquals("burned", burnsIn(0)); assertEquals("24:09", countdown(24 * 60 + 9)); assertEquals("00:00", countdown(-5)) }
}
