package app.orbz.android.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Numbers the way the console shows them. Money comes in as micro-USD strings; nothing is rounded twice.

private fun dollars(v: BigDecimal): String {
    if (v.signum() == 0) return "$0.00"
    if (v >= BigDecimal.ONE) return "$" + String.format(Locale.US, "%,.2f", v.setScale(2, RoundingMode.HALF_UP))
    if (v >= BigDecimal("0.0001")) return "$" + v.setScale(4, RoundingMode.HALF_UP).toPlainString()
    return "<$0.0001"
}

/** "1990000" (micro-USD) -> "$1.99". */
fun usdMicro(s: String?): String = runCatching { dollars(BigDecimal(s ?: "0").movePointLeft(6)) }.getOrDefault("$0.00")

/** "1.99" -> "$1.99". */
fun usd(s: String?): String = runCatching { dollars(BigDecimal(s ?: "0")) }.getOrDefault("$0.00")

/** "$15.0K", "$1.20M", "$12.51". */
fun money(v: Double?): String = when {
    v == null -> "unavailable"
    v > 0 && v < 0.01 -> "<$0.01"
    v >= 1e6 -> String.format(Locale.US, "$%.2fM", v / 1e6)
    v >= 1e3 -> String.format(Locale.US, "$%.1fK", v / 1e3)
    v >= 1 -> String.format(Locale.US, "$%.2f", v)
    else -> "$" + BigDecimal(v).round(java.math.MathContext(3)).toPlainString()
}

/** "130.5K", "1.00B", "0.00392". */
fun compact(v: Double?): String = when {
    v == null -> "unavailable"
    v > 0 && v < 1 -> BigDecimal(v).round(java.math.MathContext(3)).toPlainString()
    v >= 1e9 -> String.format(Locale.US, "%.2fB", v / 1e9)
    v >= 1e6 -> String.format(Locale.US, "%.2fM", v / 1e6)
    v >= 1e3 -> if (v % 1000.0 == 0.0) String.format(Locale.US, "%.0fK", v / 1e3) else String.format(Locale.US, "%.1fK", v / 1e3)
    else -> String.format(Locale.US, "%,.0f", v)
}

fun shortAddr(a: String) = if (a.length > 12) "${a.take(6)}…${a.takeLast(4)}" else a

/** "6h", "2d 4h", "41m", "<1m". */
fun burnsIn(seconds: Long): String {
    if (seconds <= 0) return "burned"
    if (seconds < 60) return "<1m"
    val m = seconds / 60
    if (m < 60) return "${m}m"
    val h = m / 60
    if (h < 24) return "${h}h"
    val d = h / 24
    val rh = h % 24
    return if (rh > 0) "${d}d ${rh}h" else "${d}d"
}

/** "24:09" until the next grant. */
fun countdown(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

private val dayFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.US).withZone(ZoneId.systemDefault())
fun whenLocal(iso: String?): String = runCatching { dayFmt.format(Instant.parse(iso)) }.getOrDefault("")
fun epochSeconds(iso: String?): Long = runCatching { Instant.parse(iso).epochSecond }.getOrDefault(0L)
