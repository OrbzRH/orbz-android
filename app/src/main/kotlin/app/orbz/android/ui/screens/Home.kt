package app.orbz.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orbz.android.Config
import app.orbz.android.ui.AppVm
import app.orbz.android.ui.Display
import app.orbz.android.ui.ErrorLine
import app.orbz.android.ui.Fact
import app.orbz.android.ui.Ghost
import app.orbz.android.ui.Ink
import app.orbz.android.ui.Label
import app.orbz.android.ui.SectionLabel
import app.orbz.android.ui.Sheet
import app.orbz.android.ui.burnsIn
import app.orbz.android.ui.compact
import app.orbz.android.ui.countdown
import app.orbz.android.ui.epochSeconds
import app.orbz.android.ui.usdMicro
import kotlinx.coroutines.delay

/** The console's front page: the balance, the fuse, the epoch clock, the month, the holding. */
@Composable
fun HomeScreen(vm: AppVm) {
    val s by vm.summary.collectAsState()
    val error by vm.error.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() / 1000 } }
    LaunchedEffect(Unit) { vm.refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val sum = s
        if (sum == null) { Text(if (error == null) "Loading…" else "", style = MaterialTheme.typography.bodySmall); ErrorLine(error); return@Column }

        Sheet {
            Text("AVAILABLE CREDIT", style = Label)
            Spacer(Modifier.height(6.dp))
            Text(usdMicro(sum.balance.available), style = Display.copy(fontSize = 40.sp, lineHeight = 44.sp), modifier = Modifier.semantics { testTag = "available" })
            Spacer(Modifier.height(6.dp))
            Text("open ${usdMicro(sum.balance.open)} · reserved ${usdMicro(sum.balance.reserved)} · ${sum.tierName}", style = MaterialTheme.typography.bodySmall)
            val next = sum.buckets.firstOrNull()
            if (next != null) {
                Spacer(Modifier.height(10.dp))
                val left = epochSeconds(next.expiresAt) - now
                Text("${usdMicro(next.remaining)} burns in ${burnsIn(left)}", color = if (left < 86_400) Ink.emberGlow else Ink.vein, style = MaterialTheme.typography.bodyMedium)
                Text("Oldest credit first. Whatever is left when a fuse runs out buys \$ORBZ and burns it.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Sheet {
            SectionLabel("Epoch")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("#${sum.epoch.n}", style = Display)
                Column(horizontalAlignment = Alignment.End) {
                    Text("NEXT GRANT", style = Label.copy(color = Ink.dust))
                    Text(countdown(sum.epoch.nextAt - now), style = Display.copy(color = Ink.ember))
                }
            }
            Text("Every ${Config.EPOCH_SECONDS / 60} minutes, the fees of the epoch become credit for holders of ${compact(Config.FLOOR_TOKENS.toDouble())} \$ORBZ or more.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
        }

        sum.token?.let { t ->
            val whole = t.whole?.toDoubleOrNull()
            val eligible = whole != null && whole >= Config.FLOOR_TOKENS
            Sheet {
                SectionLabel("\$ORBZ in this wallet")
                Fact("Balance", if (whole == null) "unavailable" else compact(whole), ok = eligible)
                Fact("Credit", if (whole == null) "unavailable" else if (eligible) "gets credit every epoch" else "below ${compact(Config.FLOOR_TOKENS.toDouble())}, no credit yet", warn = whole != null && !eligible)
                sum.eth?.toBigDecimalOrNull()?.let { wei -> Fact("ETH", wei.movePointLeft(18).setScale(5, java.math.RoundingMode.DOWN).toPlainString()) }
            }
        }

        sum.month?.let { m ->
            Sheet {
                SectionLabel(m.label)
                Fact("Used", usdMicro(m.used))
                Fact("Requests", m.requests.toString())
                Fact("Expired", usdMicro(m.expired))
            }
        }

        ErrorLine(error)
        Ghost("Refresh", onClick = { vm.refresh() })
        Spacer(Modifier.height(8.dp))
    }
}
