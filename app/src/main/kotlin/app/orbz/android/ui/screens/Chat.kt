package app.orbz.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.orbz.android.ui.ChatVm
import app.orbz.android.ui.ErrorLine
import app.orbz.android.ui.Field
import app.orbz.android.ui.Ink
import app.orbz.android.ui.Primary
import app.orbz.android.ui.ReceiptLine
import app.orbz.android.ui.Rich
import app.orbz.android.ui.Turn
import app.orbz.android.ui.usd

/** Orbz Opus, streamed, with the cost under every answer. Same meter and daily limit as the console's chat. */
@Composable
fun ChatScreen(vm: ChatVm = viewModel()) {
    val turns by vm.turns.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val balance by vm.balance.collectAsState()
    var draft by rememberSaveable { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(turns.size, turns.lastOrNull()?.text?.length) { if (turns.isNotEmpty()) list.animateScrollToItem(turns.size - 1) }

    Column(Modifier.fillMaxSize().imePadding()) {
        if (turns.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Ask Orbz Opus anything.", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text("Writing, code, analysis, any language. Paid from your credit at list price, with the cost under every reply.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(turns, key = { it.id }) { t -> Bubble(t) }
            }
        }
        Column(Modifier.fillMaxWidth().background(Ink.bg).padding(12.dp)) {
            ErrorLine(error)
            Row(verticalAlignment = Alignment.Bottom) {
                Field(draft, { draft = it }, "Ask Orbz Opus…", Modifier.weight(1f), single = false, tag = "chatInput")
                Spacer(Modifier.width(8.dp))
                Primary("Send", enabled = !busy && draft.isNotBlank(), tag = "chatSend", onClick = { vm.send(draft); draft = "" })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (balance != null) "balance ${usd(balance)}" else "List price, no markup.", style = MaterialTheme.typography.bodySmall)
                if (turns.isNotEmpty()) TextButton(onClick = { vm.clear() }) { Text("Clear", color = Ink.vein) }
            }
        }
    }
}

@Composable
private fun Bubble(t: Turn) {
    val user = t.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        Box(
            Modifier.fillMaxWidth(0.92f)
                .background(if (user) Ink.bg2 else Ink.night, RoundedCornerShape(12.dp))
                .border(1.dp, if (user) Ink.brass.copy(alpha = 0.35f) else Ink.line, RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            if (user) Text(t.text, style = MaterialTheme.typography.bodyMedium.copy(color = Ink.marbleLight))
            else if (t.text.isEmpty() && t.streaming) Text("…", color = Ink.dust)
            else Rich(t.text)
        }
        if (!user && t.cost != null) ReceiptLine("${t.tokens ?: "?"} tokens · cost ${usd(t.cost)}")
    }
}
