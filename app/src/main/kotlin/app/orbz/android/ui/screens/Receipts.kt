package app.orbz.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.orbz.android.ui.ErrorLine
import app.orbz.android.ui.Ink
import app.orbz.android.ui.Label
import app.orbz.android.ui.Mono
import app.orbz.android.ui.ReceiptsVm
import app.orbz.android.ui.Sheet
import app.orbz.android.ui.usd
import app.orbz.android.ui.whenLocal

/** One receipt per epoch, newest first: what came in, what was granted, used, expired and burned. Public numbers. */
@Composable
fun ReceiptsScreen(vm: ReceiptsVm = viewModel()) {
    val rows by vm.rows.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Receipts", style = MaterialTheme.typography.titleLarge)
            Text("Every epoch, on the record: fees in, credit granted, credit used, credit expired, \$ORBZ burned.", style = MaterialTheme.typography.bodySmall)
            ErrorLine(error)
            if (rows == null && error == null) Text("Loading…", style = MaterialTheme.typography.bodySmall)
        }
        items(rows ?: emptyList(), key = { it.epoch }) { r ->
            Sheet {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EPOCH ${r.epoch}", style = Label)
                    Text(whenLocal(r.startsAt), style = Label.copy(color = Ink.dust))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Cell("Granted", usd(r.granted)); Cell("Used", usd(r.used)); Cell("Open", usd(r.open)); Cell("Wallets", r.wallets.toString())
                }
                if (r.feesIn != null || r.expired != "0") {
                    Spacer(Modifier.height(6.dp))
                    Text("fees in ${usd(r.feesIn)} · expired ${usd(r.expired)} · ${r.status}", style = Mono.copy(color = Ink.dust, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified))
                }
                r.burn?.let { b -> Text("burned ${b.orbzBurned ?: "?"} \$ORBZ for ${usd(b.usdgIn)}", style = Mono.copy(color = Ink.emberGlow)) }
            }
        }
    }
}

@Composable
private fun Cell(k: String, v: String) {
    Column { Text(k.uppercase(), style = Label.copy(color = Ink.dust)); Text(v, style = Mono.copy(color = Ink.marbleLight)) }
}
