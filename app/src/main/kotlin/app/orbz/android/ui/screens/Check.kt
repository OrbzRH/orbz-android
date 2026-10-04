package app.orbz.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.orbz.android.Config
import app.orbz.android.ui.CheckVm
import app.orbz.android.ui.ErrorLine
import app.orbz.android.ui.Fact
import app.orbz.android.ui.Field
import app.orbz.android.ui.Ghost
import app.orbz.android.ui.Ink
import app.orbz.android.ui.Label
import app.orbz.android.ui.Primary
import app.orbz.android.ui.ReceiptLine
import app.orbz.android.ui.Rich
import app.orbz.android.ui.SectionLabel
import app.orbz.android.ui.Sheet
import app.orbz.android.ui.compact
import app.orbz.android.ui.money
import app.orbz.android.ui.shortAddr
import app.orbz.android.ui.usd
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

// Reading the facts the server sends. A missing or null fact shows as "unavailable", never as a guess.
private fun JsonObject.at(path: String): JsonPrimitive? {
    var cur: Any? = this
    for (k in path.split(".")) cur = (cur as? JsonObject)?.get(k) ?: return null
    return cur as? JsonPrimitive
}
private fun JsonObject.obj(path: String): JsonObject? { var cur: Any? = this; for (k in path.split(".")) cur = (cur as? JsonObject)?.get(k) ?: return null; return cur as? JsonObject }
private fun JsonObject.str(p: String) = at(p)?.takeIf { it !is JsonNull }?.contentOrNull
private fun JsonObject.num(p: String) = at(p)?.takeIf { it !is JsonNull }?.doubleOrNull
private fun JsonObject.bool(p: String) = at(p)?.takeIf { it !is JsonNull }?.booleanOrNull
private const val NA = "unavailable"

/** Token check and Wallet check share one box: a token gets the token sheet, anything else the wallet sheet. */
@Composable
fun CheckScreen(vm: CheckVm = viewModel()) {
    val kind by vm.kind.collectAsState()
    val facts by vm.facts.collectAsState()
    val loading by vm.loading.collectAsState()
    val read by vm.read.collectAsState()
    val reading by vm.reading.collectAsState()
    val cost by vm.cost.collectAsState()
    val error by vm.error.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf("") }
    val uri = LocalUriHandler.current

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ask about any token or wallet", style = MaterialTheme.typography.titleLarge)
        Text("Paste a Robinhood Chain address. The facts are free; the read is about \$0.001 of your credit.", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(input, { input = it }, "0x…", Modifier.weight(1f), mono = true, tag = "checkInput")
            Spacer(Modifier.width(8.dp))
            Primary(if (loading) "…" else "Check", enabled = !loading && !reading && input.isNotBlank(), tag = "checkGo", onClick = { vm.check(input) })
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Ink.ember, trackColor = Ink.line)
        ErrorLine(error)

        val f = facts
        if (f != null) {
            Sheet {
                if (kind == "token") TokenSheet(f) else WalletSheet(f)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { uri.openUri("${Config.EXPLORER}/address/${f.str("address")}") }) { Text("Open in explorer", color = Ink.vein) }
            }
            Sheet {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel("Orbz Opus read")
                    if (cost != null) Text("cost ${usd(cost)}", style = Label.copy(color = Ink.dust))
                }
                if (read.isEmpty() && reading) Text("Reading…", color = Ink.dust) else Rich(read, Modifier.padding(top = 4.dp))
                if (reading && read.isNotEmpty()) Text("▍", color = Ink.ember)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Field(question, { question = it }, "Ask a follow-up about this address", Modifier.weight(1f), tag = "followUp")
                    Spacer(Modifier.width(8.dp))
                    Ghost("Ask", enabled = !reading && question.isNotBlank(), tag = "followUpGo", onClick = { vm.ask(question); question = "" })
                }
                if (kind == "token") ReceiptLine("Facts only, never a buy or sell call.")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TokenSheet(f: JsonObject) {
    val m = f.obj("market.top")
    Text(f.str("token.name") ?: NA, style = MaterialTheme.typography.titleLarge)
    Text("\$${f.str("token.symbol") ?: "?"} · ${shortAddr(f.str("address") ?: "")}", style = Label.copy(color = Ink.vein))
    Spacer(Modifier.height(10.dp))
    SectionLabel("Market")
    val liq = m?.num("liquidityUsd")
    val age = m?.num("pairAgeHours")
    Fact("Price", m?.num("priceUsd")?.let { "$" + java.math.BigDecimal(it).round(java.math.MathContext(4)).toPlainString() } ?: NA)
    Fact("Liquidity", money(liq), warn = liq != null && liq < 10_000)
    Fact("Volume 24h", money(m?.num("volume24h")))
    val buys = m?.at("buys24h")?.intOrNull; val sells = m?.at("sells24h")?.intOrNull
    Fact("Trades 24h", if (buys != null && sells != null) "$buys buys · $sells sells" else NA)
    Fact("Pool", if (m != null) "${m.str("dex") ?: ""} · ${age?.toInt() ?: "?"}h old" else if (f.at("market.pairs")?.intOrNull == 0) "no pool found" else NA, warn = age != null && age < 72)
    Spacer(Modifier.height(10.dp))
    SectionLabel("Supply and control")
    Fact("Supply", compact(f.num("token.totalSupply")))
    Fact("Burned", f.num("supply.burnedPct")?.let { "$it%" } ?: NA)
    val owner = f.str("control.owner")
    val renounced = f.bool("control.ownerRenounced") == true
    Fact("Owner", if (owner != null) (if (renounced) "renounced" else shortAddr(owner)) else if (f.bool("control.ownerFunction") == false) "no owner function" else NA, warn = owner != null && !renounced)
    val up = f.bool("control.upgradeable"); val proxy = f.bool("control.proxyLike") == true
    Fact("Upgradeable", if (up == true) "yes" else if (proxy) "likely a proxy" else if (up == false) "no" else NA, warn = up == true || proxy)
    val act = f.obj("activity")
    if (act != null) {
        Spacer(Modifier.height(10.dp))
        SectionLabel("Recent transfers")
        Fact("Transfers", "${act.at("transfers")?.intOrNull ?: 0} · ${act.at("wallets")?.intOrNull ?: 0} wallets")
        (act["topReceivers"] as? JsonArray)?.take(3)?.forEach { r ->
            val o = r as? JsonObject ?: return@forEach
            val contract = o.bool("isContract") == true
            val pct = o.num("pctSupply")
            Fact(if (contract) "contract" else "wallet", "${shortAddr(o.str("address") ?: "")} · ${pct ?: "?"}%", warn = !contract && (pct ?: 0.0) >= 2)
        }
    }
}

@Composable
private fun WalletSheet(w: JsonObject) {
    val kind = when (w.str("kind")) { "smart-wallet" -> "Smart wallet (EIP-7702)"; "contract" -> "Contract"; else -> "Wallet" }
    Text(kind, style = MaterialTheme.typography.titleLarge)
    Text(shortAddr(w.str("address") ?: ""), style = Label.copy(color = Ink.vein))
    Spacer(Modifier.height(10.dp))
    SectionLabel("Overview")
    Fact("Value", money(w.num("totals.valueUsd")))
    val eth = w.num("eth.amount")
    Fact("ETH", if (eth == null) NA else compact(eth) + (w.num("eth.valueUsd")?.let { " · ${money(it)}" } ?: ""))
    Fact("Transactions sent", w.at("txSent")?.intOrNull?.toString() ?: NA)
    val age = w.at("ageDays")?.intOrNull
    Fact("First seen", if (age == null) NA else if (age == 0) "today" else if (age == 1) "1 day old" else "$age days old", warn = age != null && age < 7)
    w.obj("fundedBy")?.let { fb -> Fact("Funded by", "${shortAddr(fb.str("address") ?: "")} · ${compact(fb.num("amount"))} ${fb.str("asset") ?: ""}") }
    w.obj("orbz")?.let { o ->
        val ok = o.bool("eligible") == true
        Fact("\$ORBZ", "${compact(o.num("amount"))} · ${if (ok) "gets credit" else "below 100K"}", ok = ok, warn = !ok)
    }
    Spacer(Modifier.height(10.dp))
    SectionLabel("Holdings (${w.at("totals.tokens")?.intOrNull ?: 0})")
    val rows = (w["holdings"] as? JsonArray)?.take(8) ?: emptyList()
    if (rows.isEmpty()) Text("No tokens.", style = MaterialTheme.typography.bodySmall)
    rows.forEach { r ->
        val h = r as? JsonObject ?: return@forEach
        val market = h.str("market")
        Fact("${h.str("symbol") ?: shortAddr(h.str("address") ?: "")} · ${compact(h.num("amount"))}", when (market) { "priced" -> money(h.num("valueUsd")); "thin" -> "thin market"; else -> "no market" }, warn = market != "priced")
    }
    val odd = (w.at("totals.noMarket")?.intOrNull ?: 0) + (w.at("totals.thin")?.intOrNull ?: 0)
    if (odd > 0) Text("$odd with no or a thin market. Tokens like these are often airdropped to wallets: do not approve or swap them.", color = Ink.emberGlow, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
}
