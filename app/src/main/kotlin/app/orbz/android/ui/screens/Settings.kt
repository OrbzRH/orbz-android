package app.orbz.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.orbz.android.Config
import app.orbz.android.ui.AppVm
import app.orbz.android.ui.Fact
import app.orbz.android.ui.Ghost
import app.orbz.android.ui.Ink
import app.orbz.android.ui.Mono
import app.orbz.android.ui.SectionLabel
import app.orbz.android.ui.Sheet

@Composable
fun SettingsScreen(vm: AppVm, address: String) {
    val uri = LocalUriHandler.current
    val clip = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Sheet {
            SectionLabel("Signed in as")
            Text(address, style = Mono.copy(color = Ink.marbleLight))
            Text(if (vm.viaKey) "with an API key" else "with a wallet signature", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { clip.setText(AnnotatedString(address)) }) { Text("Copy address", color = Ink.vein) }
            Spacer(Modifier.height(4.dp))
            Ghost("Sign out", Modifier.fillMaxWidth(), tag = "signOut", onClick = { vm.signOut() })
            Text(if (vm.viaKey) "Signing out forgets the key on this phone. To kill the key everywhere, revoke it on the Keys page." else "Signing out ends this session and disconnects the wallet. Your credit stays with your wallet.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
        Sheet {
            SectionLabel("Safety")
            Text("This app signs in with one message signature and never asks your wallet for a transaction or a token approval. If a prompt ever asks for either, decline it: it is not from Orbz.", style = MaterialTheme.typography.bodyMedium)
        }
        Sheet {
            SectionLabel("About")
            Fact("Version", Config.VERSION)
            Fact("Chain", "${Config.CHAIN_NAME} (${Config.CHAIN_ID})")
            Fact("Console", Config.API_BASE.removePrefix("https://"))
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { uri.openUri(Config.SITE) }) { Text("orbz.app", color = Ink.vein) }
            TextButton(onClick = { uri.openUri("${Config.SITE}/docs") }) { Text("Docs", color = Ink.vein) }
            TextButton(onClick = { uri.openUri("https://github.com/OrbzRH") }) { Text("Source on GitHub", color = Ink.vein) }
            TextButton(onClick = { uri.openUri("https://x.com/OrbzRH") }) { Text("@OrbzRH", color = Ink.vein) }
        }
    }
}
